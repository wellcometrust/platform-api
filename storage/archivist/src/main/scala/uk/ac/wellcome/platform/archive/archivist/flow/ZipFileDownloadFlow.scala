package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.sns.AmazonSNS
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.archivist.models.TypeAliases._
import uk.ac.wellcome.platform.archive.common.messaging.SnsPublishFlow
import uk.ac.wellcome.platform.archive.common.models.{
  IngestBagRequest,
  Parallelism
}
import uk.ac.wellcome.platform.archive.common.progress.models._

import scala.concurrent.ExecutionContext

/** This flow takes an ingest request, and downloads the entire ZIP file
  * associated with the request to a local (temporary) path.
  *
  * It returns an Either with errors on the Left, or the zipfile and the
  * original request on the Right.
  *
  */
object ZipFileDownloadFlow {

  def apply(snsConfig: SNSConfig)(
    implicit transferManager: TransferManager,
    ec: ExecutionContext,
    snsClient: AmazonSNS,
    parallelism: Parallelism
  ): Flow[IngestBagRequest, BagDownload, NotUsed] = {

    val downloadSuccessMessage = "Ingest bag file downloaded successfully"

    val snsPublishFlow = SnsPublishFlow[ProgressUpdate](
      snsClient,
      snsConfig,
      subject = "archivist_progress"
    )

    Flow[IngestBagRequest].flatMapMerge(
      parallelism.value,
      request => {
        val bagDownload = request.toIngestBagJob.bagDownload
        Source
          .fromFuture(bagDownload)
          .map { either =>
            {
              either.fold(
                error => ProgressUpdate.failed(request.id, error),
                _ => ProgressUpdate.event(
                  id = request.id,
                  description = downloadSuccessMessage
                )
              )
            }
          }
          .via(snsPublishFlow)
          .mapAsync(parallelism.value)(_ => bagDownload)
      }
    )

  }

}
