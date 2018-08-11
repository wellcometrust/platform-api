package uk.ac.wellcome.platform.archiver.flow

import akka.stream.alpakka.sns.scaladsl.SnsPublisher
import akka.stream.scaladsl.Flow
import com.amazonaws.services.sns.AmazonSNSAsync
import uk.ac.wellcome.json.JsonUtil._

import scala.util.{Failure, Success}

object BagArchiveCompleteFlow {
  def apply(topicArn: String)(implicit snsClient: AmazonSNSAsync) = Flow[BagLocation]
    .map(createNotification)
    .log("created notification")
    .map(toJson(_))
    .map {
      case Success(json) => json
      case Failure(e) => throw e
    }
    .log("notification serialised")
    .via(SnsPublisher.flow(topicArn))
    .log("published notification")

  def createNotification(bagLocation: BagLocation) =
    BagArchiveCompleteNotification(bagLocation)

}

case class BagArchiveCompleteNotification(
                                           bagLocation: BagLocation
                                         )
