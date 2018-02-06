package uk.ac.wellcome.platform.reindexer.services

import javax.inject.Inject

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.ops.ScanamoOps
import com.gu.scanamo.query.{UniqueKey, _}
import com.gu.scanamo.request.{ScanamoQueryOptions, ScanamoQueryRequest}
import com.gu.scanamo.syntax._
import com.gu.scanamo.update.UpdateExpression
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.transformable.Reindexable
import uk.ac.wellcome.platform.reindexer.models.{ReindexAttempt, ReindexStatus}
import uk.ac.wellcome.reindexer.models.ScanamoQueryStream
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class ReindexTargetService[T <: Reindexable[String]] @Inject()(
  dynamoDBClient: AmazonDynamoDB,
  metricsSender: MetricsSender,
  @Flag("reindex.sourceData.tableName") targetTableName: String)
    extends Logging {

  type ScanamoQueryResult = Either[DynamoReadError, T]

  type ScanamoQueryResultFunction =
    (List[ScanamoQueryResult]) => Boolean

  private val gsiName = "ReindexTracker"

  private def scanamoUpdate(k: UniqueKey[_],
                            updateExpression: UpdateExpression)(
    implicit evidence: DynamoFormat[T]): Either[DynamoReadError, T] =
    Scanamo.update[T](dynamoDBClient)(targetTableName)(k, updateExpression)

  private def scanamoQueryStreamFunction(
    queryRequest: ScanamoQueryRequest,
    resultFunction: ScanamoQueryResultFunction
  )(implicit evidence: DynamoFormat[T]): ScanamoOps[List[Boolean]] =
    ScanamoQueryStream.run[T, Boolean](queryRequest, resultFunction)

  private def updateVersion(requestedVersion: Int)(
    resultGroup: List[ScanamoQueryResult])(
    implicit evidence: DynamoFormat[T]): Boolean = {
    val updatedResults = resultGroup.map {
      case Left(e) => Left(e)
      case Right(miroTransformable) => {
        val reindexItem = Reindexable.getReindexItem(miroTransformable)

        scanamoUpdate(reindexItem.hashKey and reindexItem.rangeKey,
                      set('ReindexVersion -> requestedVersion))
      }
    }

    val performedUpdates = updatedResults.nonEmpty

    if (performedUpdates) {
      info(s"ReindexTargetService completed batch of ${updatedResults.length}")
      metricsSender.incrementCount("reindex-updated-items",
                                   updatedResults.length)
      ReindexStatus.progress(updatedResults.length, 1)
    }

    performedUpdates
  }

  private def createScanamoQueryRequest(reindexJob: ReindexJob): ScanamoQueryRequest =
    ScanamoQueryRequest(
      targetTableName,
      Some(gsiName),
      Query(
        AndQueryCondition(
          KeyEquals('reindexShard, reindexJob.shardId),
          KeyIs('reindexVersion, LT, reindexJob.desiredVersion)
        )),
      ScanamoQueryOptions.default
    )

  def runReindex(reindexJob: ReindexJob)(
      implicit evidence: DynamoFormat[T]): Future[Unit] = {

    info(s"ReindexTargetService running $reindexJob")

    val scanamoQueryRequest = createScanamoQueryRequest(reindexJob)

    val ops = scanamoQueryStreamFunction(
      queryRequest = scanamoQueryRequest,
      resultFunction = updateVersion(requestedVersion)
    )

    Scanamo.exec(dynamoDBClient)(ops) match {
      case Right(_) =>
        info(s"Successfully processed reindex job $reindexJob")
      case Left(err) =>
        warn(s"Failed to process reindex job $reindexJob", err)
        throw err
    }
  }
}
