package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.File
import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.ActorAttributes
import akka.stream.scaladsl.{Flow, Source, StreamConverters}
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sns.AmazonSNS
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.archivist.models.errors.ZipFileDownloadingError
import uk.ac.wellcome.platform.archive.common.messaging.SnsPublishFlow
import uk.ac.wellcome.platform.archive.common.models.IngestBagRequest
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError
import uk.ac.wellcome.platform.archive.common.progress.models._

import scala.util.{Failure, Success, Try}

/** This flow takes an ingest request, and downloads the entire ZIP file
  * associated with the request to a local (temporary) path.
  *
  * It returns an Either with errors on the Left, or the zipfile and the
  * original request on the Right.
  *
  */
object ZipFileDownloadFlow extends Logging {

  def apply(parallelism: Int, snsConfig: SNSConfig)(implicit s3Client: AmazonS3,
                                                    snsClient: AmazonSNS)
    : Flow[IngestBagRequest,
           Either[ArchiveError[IngestBagRequest], ZipFileDownloadComplete],
           NotUsed] = {

    Flow[IngestBagRequest]
      .log("download location")
      .flatMapMerge(
        parallelism, {
          case request @ IngestBagRequest(_, location, _, _) =>
            val triedInputStream =
              Try(s3Client.getObject(location.namespace, location.key)).map(
                response => response.getObjectContent)

            triedInputStream match {
              case Failure(ex) =>
                warn(s"Failed downloading zipFile from $location")
                Source.single(Left(ZipFileDownloadingError(request, ex)))
              case Success(inputStream) =>
                val tmpFile = File.createTempFile("archivist", ".tmp")

                StreamConverters
                  .fromInputStream(() => inputStream)
                  .via(FileStoreFlow(tmpFile, parallelism))
                  .map { result =>
                    result.status match {
                      case Success(_) =>
                        Right(
                          ZipFileDownloadComplete(
                            zipFile = new ZipFile(tmpFile),
                            ingestBagRequest = request
                          ))
                      case Failure(ex) =>
                        warn(s"Failed downloading zipFile from $location")
                        Left(ZipFileDownloadingError(request, ex))
                    }
                  }
            }

        }
      )
      .withAttributes(ActorAttributes.dispatcher(
        "akka.stream.materializer.blocking-io-dispatcher"))
      .flatMapMerge(
        parallelism,
        (result: Either[ArchiveError[IngestBagRequest],
                        ZipFileDownloadComplete]) =>
          Source
            .single(toProgressUpdate(result))
            .log("sending to progress monitor")
            .via(
              SnsPublishFlow[ProgressUpdate](
                snsClient,
                snsConfig,
                Some("archivist_progress")))
            .map(_ => result)
      )
      .log("downloaded zipfile")
  }

  private def toProgressUpdate(
    result: Either[ArchiveError[IngestBagRequest], ZipFileDownloadComplete])
    : ProgressUpdate =
    result match {
      case Right(ZipFileDownloadComplete(_, ingestBagRequest)) =>
        ProgressEventUpdate(
          ingestBagRequest.archiveRequestId,
          List(ProgressEvent("zipFile downloaded successfully")))
      case Left(archiveError) =>
        ProgressStatusUpdate(
          archiveError.t.archiveRequestId,
          Progress.Failed,
          List(ProgressEvent(archiveError.toString))
        )
    }
}

case class ZipFileDownloadComplete(zipFile: ZipFile,
                                   ingestBagRequest: IngestBagRequest)
