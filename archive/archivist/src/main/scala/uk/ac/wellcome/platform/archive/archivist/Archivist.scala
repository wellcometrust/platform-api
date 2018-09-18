package uk.ac.wellcome.platform.archive.archivist

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.Flow
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import com.amazonaws.services.sns.AmazonSNSAsync
import com.google.inject.Injector
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.archivist.models.BagUploaderConfig
import uk.ac.wellcome.platform.archive.archivist.flow.{ArchiveCompleteFlow, ArchiveZipFileFlow, ZipFileDownloadFlow}
import uk.ac.wellcome.platform.archive.common.messaging.MessageStream
import uk.ac.wellcome.platform.archive.common.models.{IngestBagRequest, IngestBagRequestNotification, IngestRequestContext, NotificationMessage}

import scala.concurrent.ExecutionContext
import scala.util.Success

trait Archivist extends Logging {
  val injector: Injector

  def run() = {
    implicit val s3Client: S3Client = injector.getInstance(classOf[S3Client])
    implicit val snsClient: AmazonSNSAsync =
      injector.getInstance(classOf[AmazonSNSAsync])
    implicit val actorSystem: ActorSystem =
      injector.getInstance(classOf[ActorSystem])
    implicit val executionContext: ExecutionContext = actorSystem.dispatcher
    implicit val adapter: LoggingAdapter =
      Logging(actorSystem.eventStream, "customLogger")

    val decider: Supervision.Decider = {
      case e => {
        error("Stream failure", e)
        Supervision.Resume
      }
    }

    implicit val materializer = ActorMaterializer(
      ActorMaterializerSettings(actorSystem).withSupervisionStrategy(decider)
    )

    val messageStream =
      injector.getInstance(classOf[MessageStream[NotificationMessage, Object]])
    val bagUploaderConfig = injector.getInstance(classOf[BagUploaderConfig])
    val snsConfig = injector.getInstance(classOf[SNSConfig])

    val workFlow =
      Flow[NotificationMessage]
        .log("notification message")
        .via(NotificationMessageFlow())
        .log("download location")
        .via(ZipFileDownloadFlow())
        .log("download zip")
        .via(ArchiveZipFileFlow(bagUploaderConfig))
        .log("archive verified")
        .via(ArchiveCompleteFlow(snsConfig.topicArn))

    messageStream.run("archivist", workFlow)
  }
}

object NotificationMessageFlow {
  def apply() = {
    Flow[NotificationMessage]
      .map(message => fromJson[IngestBagRequest](message.Message))
      // TODO: Log error here
      .collect {
      case Success(bagRequest) => bagRequest
    }
  }
}