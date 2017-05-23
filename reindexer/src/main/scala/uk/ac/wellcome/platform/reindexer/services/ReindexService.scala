package uk.ac.wellcome.platform.reindexer.services

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.google.inject.Inject
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.query.{KeyEquals, _}
import com.gu.scanamo.syntax._
import com.gu.scanamo.{Scanamo, Table}
import com.twitter.inject.Logging
import uk.ac.wellcome.models._
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class ReindexService @Inject()(dynamoDBClient: AmazonDynamoDB,
                               dynamoConfig: DynamoConfig) extends Logging {

  val tableName = dynamoConfig.table
  val gsiName = "ReindexTracker"

  lazy val miroTable = Table[MiroTransformable]("MiroData")
  lazy val calmTable = Table[CalmTransformable]("CalmData")

  case class ReindexAttempt(reindex: Reindex, successful: List[Reindexable[String]], attempt: Int)

  def run: Future[List[ReindexAttempt]] = for {
      indices <- getIndicesForReindex
      attempts = indices.map(ReindexAttempt(_, Nil, 0))
      completions <- Future.sequence(attempts.map(processReindexAttempt))
    } yield completions

  private def processReindexAttempt(reindexAttempt: ReindexAttempt): Future[ReindexAttempt] = (reindexAttempt match {
    case ReindexAttempt(_, _, 0) => runReindex(reindexAttempt) // First attempt.
      // Loops forever \/ Here!
    case ReindexAttempt(_, Nil, attempt) => Future.successful(reindexAttempt) // Stop: done!
    case ReindexAttempt(_, _, attempt) if attempt > 2 => Future.failed(new RuntimeException(s"Giving up on $reindexAttempt, tried too many times.")) // Stop: give up!
    case _ => runReindex(reindexAttempt) // Carry on.
  }).flatMap(processReindexAttempt)

  def runReindex(reindexAttempt: ReindexAttempt) =
    for {
      rows <- getRowsWithOldReindexVersion(reindexAttempt.reindex)
      filteredRows <- logAndFilterLeft(rows)
      updateOps <- updateRows(reindexAttempt.reindex, filteredRows.map(_.getReindexItem))
      updatedRows <- logAndFilterLeft(updateOps)
    } yield reindexAttempt.copy(successful = updatedRows, attempt = reindexAttempt.attempt + 1)

  def logAndFilterLeft(
    rows: List[Either[DynamoReadError, Reindexable[String]]]) = Future {
    rows
      .flatMap(_ match {
        case Left(e: DynamoReadError) => error(e.toString); None
        case a => Some(a)
      })
      .map(_.right.get)
  }

  def updateRows(reindex: Reindex, rows: List[ReindexItem[String]]) = {
    val updateTable = reindex match {
      case Reindex("MiroData", _, _) => miroTable
      case Reindex("CalmData", _, _) => calmTable
      case _ =>
        throw new RuntimeException(
          s"Attempting to update unidentified table ${reindex.TableName}")
    }

    val ops = rows.map(reindexItem => {
      val uniqueKey = reindexItem.hashKey and reindexItem.rangeKey
      updateTable.update(uniqueKey,
                         set('ReindexVersion -> reindex.requestedVersion))
    })

    Future(ops.map(Scanamo.exec(dynamoDBClient)(_)))
  }

  def getRowsWithOldReindexVersion(reindex: Reindex) = Future {
    val query = reindex match {
      case Reindex("MiroData", _, _) =>
        Scanamo.queryIndex[MiroTransformable](dynamoDBClient) _
      case Reindex("CalmData", _, _) =>
        Scanamo.queryIndex[CalmTransformable](dynamoDBClient) _
      case _ => throw new RuntimeException("nope")
    }

    query(reindex.TableName, gsiName)(
      Query(
        AndQueryCondition(
          KeyEquals('ReindexShard, "default"),
          KeyIs('ReindexVersion, LT, reindex.requestedVersion)
        )
      ))
  }

  def getIndicesForReindex: Future[List[Reindex]] = getIndices.map(_.filter {
    case Reindex(_, requested, current) if requested > current => true
    case _ => false
  })

  def getIndices: Future[List[Reindex]] = Future {
    Scanamo.scan[Reindex](dynamoDBClient)(dynamoConfig.table).map {
      case Right(reindexes) => reindexes
      case _ => throw new RuntimeException("nope")
    }
  }
}
