package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.Flow
import uk.ac.wellcome.platform.archive.archivist.models.ArchiveJob

object ArchiveBagFlow {
  def apply(delimiter: String)(implicit s3Client: S3Client): Flow[ArchiveJob, ArchiveJob, NotUsed] =
    Flow[ArchiveJob]
      .via(ArchiveJobFlow(delimiter))
      .groupBy(Int.MaxValue, {
        case Right(archiveItemJob) => archiveItemJob.bagName
        case Left(archiveItemJob) => archiveItemJob.bagName
      })
      .reduce((first, second) => if (first.isLeft) first else second)
      .mergeSubstreams
      // TODO: Log error here
      .collect {
        case Right(archiveItemJob) => archiveItemJob.archiveJob
      }
}
