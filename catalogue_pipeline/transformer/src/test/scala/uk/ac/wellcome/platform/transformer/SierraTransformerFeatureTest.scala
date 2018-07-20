package uk.ac.wellcome.platform.transformer

import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.models.transformable.sierra.{SierraRecordNumber, SierraRecordTypes}
import uk.ac.wellcome.models.work.internal.{IdentifierType, SourceIdentifier, UnidentifiedWork}
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.platform.transformer.utils.TransformableMessageUtils
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._

class SierraTransformerFeatureTest
    extends FunSpec
    with Matchers
    with SQS
    with SNS
    with S3
    with Messaging
    with fixtures.Server
    with Eventually
    with ExtendedPatience
    with TransformableMessageUtils {

  it("transforms sierra records and publishes the result to the given topic") {
    val id = createSierraRecordNumberString
    val title = "A pot of possums"

    val sierraId = SierraRecordNumber(id)

    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          withLocalS3Bucket { messagingBucket =>
            val sierraHybridRecordMessage =
              hybridRecordNotificationMessage(
                message = createValidSierraTransformableJson(
                  id = id,
                  title = title
                ),
                sourceName = "sierra",
                s3Client = s3Client,
                bucket = storageBucket
              )

            sqsClient.sendMessage(
              queue.url,
              JsonUtil.toJson(sierraHybridRecordMessage).get
            )

            val flags: Map[String, String] = Map(
              "aws.metrics.namespace" -> "sierra-transformer"
            ) ++ s3LocalFlags(storageBucket) ++
              sqsLocalFlags(queue) ++ messageWriterLocalFlags(
              messagingBucket,
              topic)

            withServer(flags) { _ =>
              eventually {
                val snsMessages = listMessagesReceivedFromSNS(topic)
                snsMessages.size should be >= 1

                val sourceIdentifier = SourceIdentifier(
                  identifierType = IdentifierType("sierra-system-number"),
                  ontologyType = "Work",
                  value = sierraId.withCheckDigit(SierraRecordTypes.bibs)
                )

                val sierraIdentifier = SourceIdentifier(
                  identifierType = IdentifierType("sierra-identifier"),
                  ontologyType = "Work",
                  value = sierraId.withoutCheckDigit
                )

                val works = getMessages[UnidentifiedWork](topic)
                works.length shouldBe >=(1)

                works.map { actualWork =>
                  actualWork.sourceIdentifier shouldBe sourceIdentifier
                  actualWork.title shouldBe title
                  actualWork.identifiers shouldBe List(
                    sourceIdentifier,
                    sierraIdentifier)
                }
              }
            }
          }
        }
      }
    }
  }
}
