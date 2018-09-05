package uk.ac.wellcome.platform.archive.common.progress.monitor

import java.time.Instant
import java.time.format.DateTimeFormatter

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import com.gu.scanamo._
import com.gu.scanamo.error.ConditionNotMet
import com.gu.scanamo.syntax._
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.progress.models.{
  ArchiveProgress,
  ProgressEvent
}
import uk.ac.wellcome.storage.dynamo.DynamoConfig

import scala.concurrent.{ExecutionContext, Future}

class ArchiveProgressMonitor(
  dynamoDbClient: AmazonDynamoDB,
  dynamoConfig: DynamoConfig)(implicit ec: ExecutionContext)
    extends Logging {

  implicit val instantLongFormat: AnyRef with DynamoFormat[Instant] =
    DynamoFormat.coercedXmap[Instant, String, IllegalArgumentException](str =>
      Instant.from(DateTimeFormatter.ISO_INSTANT.parse(str)))(
      DateTimeFormatter.ISO_INSTANT.format(_)
    )

  def initialize(progress: ArchiveProgress) = Future {
    val progressTable = Table[ArchiveProgress](dynamoConfig.table)
    debug(s"initializing archiveProgressMonitor with $progress")

    val ops = progressTable
      .given(not(attributeExists('id)))
      .put(progress)

    Scanamo.exec(dynamoDbClient)(ops) match {
      case Left(e: ConditionalCheckFailedException) =>
        throw IdConstraintError(
          s"There is already a monitor with id:${progress.id}",
          e)
      case Left(scanamoError) =>
        val exception = new RuntimeException(
          s"Failed to create progress ${scanamoError.toString}")
        warn(s"Failed to update Dynamo record: ${progress.id}", exception)
        throw exception
      case Right(a) =>
        debug(s"Successfully updated Dynamo record: ${progress.id} $a")
    }
    progress
  }

  def addEvent(id: String,
               description: String,
               status: Option[ArchiveProgress.Status] = None) = Future {
    val event = ProgressEvent(description, Instant.now())

    val update = status match {
      case None    => append('events -> event)
      case Some(s) => append('events -> event) and set('result -> s)
    }

    val progressTable = Table[ArchiveProgress](dynamoConfig.table)
    val ops = progressTable
      .given(attributeExists('id))
      .update('id -> id, update)

    Scanamo.exec(dynamoDbClient)(ops) match {
      case Left(ConditionNotMet(e: ConditionalCheckFailedException)) =>
        throw IdConstraintError(s"Progress does not exist for id:$id", e)
      case Left(scanamoError) =>
        val exception = new RuntimeException(scanamoError.toString)
        warn(s"Failed to update Dynamo record: $id", exception)
        throw exception
      case Right(_) =>
        debug(s"Successfully updated Dynamo record: $id")
    }
    event
  }
}

final case class IdConstraintError(private val message: String,
                                   private val cause: Throwable)
    extends Exception(message, cause)
