package uk.ac.wellcome.platform.archive.archivist

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.scaladsl.Flow
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sns.AmazonSNS
import com.google.inject.name.Names
import com.google.inject.{Injector, Key}
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.archivist.flow._
import uk.ac.wellcome.platform.archive.archivist.models.BagUploaderConfig
import uk.ac.wellcome.platform.archive.archivist.models.errors.ArchiveError
import uk.ac.wellcome.platform.archive.common.messaging.MessageStream
import uk.ac.wellcome.platform.archive.common.models.{ArchiveComplete, IngestBagRequest, NotificationMessage}

import scala.util.Success

trait Archivist extends Logging {
  val injector: Injector

  def run() = {
    implicit val amazonS3: AmazonS3 = injector.getInstance(classOf[AmazonS3])

    implicit val snsClient: AmazonSNS =
      injector.getInstance(classOf[AmazonSNS])
    implicit val actorSystem: ActorSystem =
      injector.getInstance(classOf[ActorSystem])
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
      injector.getInstance(classOf[MessageStream[NotificationMessage, Unit]])
    val bagUploaderConfig = injector.getInstance(classOf[BagUploaderConfig])
    val snsRegistrarConfig = injector.getInstance(Key.get(classOf[SNSConfig], Names.named("registrarSnsConfig")))
    val snsProgressConfig = injector.getInstance(Key.get(classOf[SNSConfig], Names.named("progressSnsConfig")))

    info(s"registrar topic: $snsRegistrarConfig")
    info(s"progress topic: $snsProgressConfig")

    val workFlow =
      Flow[NotificationMessage]
        .log("notification message")
        .via(NotificationMessageFlow())
        .log("download zip")
        .via(ZipFileDownloadFlow(bagUploaderConfig.parallelism, snsProgressConfig))
        .log("archiving zip")
        .via(FoldEitherFlow[
          ArchiveError[IngestBagRequest],
          ZipFileDownloadComplete,
          Unit
          ](ifLeft = _ => ())(ifRight = buhflow(bagUploaderConfig, snsProgressConfig, snsRegistrarConfig)))

    messageStream.run("archivist", workFlow)
  }

  private def buhflow(bagUploaderConfig: BagUploaderConfig, snsProgressConfig: SNSConfig, snsRegistrarConfig: SNSConfig)(implicit s3: AmazonS3, snsClient: AmazonSNS): Flow[ZipFileDownloadComplete, Unit, NotUsed] = {
    ArchiveZipFileFlow(bagUploaderConfig, snsProgressConfig)
      .log("archive verified")
      .via(FoldEitherFlow[
        ArchiveError[_],
        ArchiveComplete,
        Unit
        ](ifLeft = _ => ())(ifRight = ArchiveCompleteFlow(snsRegistrarConfig).map(_=>()) ))
  }
}

object NotificationMessageFlow {

  import IngestBagRequest._

  def apply() = {
    Flow[NotificationMessage]
      .map(message => fromJson[IngestBagRequest](message.Message))
      // TODO: Log error here
      .collect {
        case Success(bagRequest) => bagRequest
      }
  }
}
