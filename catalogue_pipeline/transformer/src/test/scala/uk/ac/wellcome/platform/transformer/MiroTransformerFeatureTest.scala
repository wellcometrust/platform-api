package uk.ac.wellcome.platform.transformer

import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.models.work.internal.UnidentifiedWork
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.platform.transformer.transformers.MiroTransformableWrapper
import uk.ac.wellcome.platform.transformer.utils.TransformableMessageUtils
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._

class MiroTransformerFeatureTest
  extends FunSpec
    with Matchers
    with SQS
    with SNS
    with S3
    with Messaging
    with fixtures.Server
    with Eventually
    with ExtendedPatience
    with MiroTransformableWrapper
    with TransformableMessageUtils {

  it("transforms miro records and publishes the result to the given topic") {
    val miroID = "M0000001"
    val title = "A guide for a giraffe"

    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          withLocalS3Bucket { messageBucket =>
            val miroHybridRecordMessage =
              hybridRecordNotificationMessage(
                message = createValidMiroTransformableJson(
                  MiroID = miroID,
                  MiroCollection = "foo",
                  data = buildJSONForWork(s""""image_title": "$title"""")
                ),
                sourceName = "miro",
                s3Client = s3Client,
                bucket = storageBucket
              )

            sqsClient.sendMessage(
              queue.url,
              JsonUtil.toJson(miroHybridRecordMessage).get
            )

            val flags: Map[String, String] = Map(
              "aws.metrics.namespace" -> "sierra-transformer"
            ) ++ s3LocalFlags(storageBucket) ++
              sqsLocalFlags(queue) ++ messageWriterLocalFlags(
              messageBucket,
              topic)

            withServer(flags) { _ =>
              eventually {
                val works = getMessages[UnidentifiedWork](topic)
                works.length shouldBe >=(1)

                works.map { actualWork =>
                  actualWork.identifiers.head.value shouldBe miroID
                  actualWork.title shouldBe title
                }
              }
            }
          }
        }
      }
    }
  }

  // This is based on a specific bug that we found where different records
  // were written to the same s3 key because of the hashing algorithm clashing
  it("sends different messages for different miro records") {
    withLocalSnsTopic { topic =>
      withLocalSqsQueue { queue =>
        withLocalS3Bucket { storageBucket =>
          withLocalS3Bucket { messageBucket =>
            val flags: Map[String, String] = Map(
              "aws.metrics.namespace" -> "sierra-transformer"
            ) ++ s3LocalFlags(storageBucket) ++
              sqsLocalFlags(queue) ++ messageWriterLocalFlags(
              messageBucket,
              topic)

            withServer(flags) { _ =>

              val miroHybridRecordMessage1 =
                hybridRecordNotificationMessage(
                  message = createValidMiroTransformableJson(
                    MiroID = "L0011975",
                    MiroCollection = "images-L",
                    data =
                      """
                      {
                          "image_cleared": "Y",
                          "image_copyright_cleared": "Y",
                          "image_credit_line": "Wellcome Library, London",
                          "image_image_desc": "Antonio Dionisi",
                          "image_innopac_id": "12917175",
                          "image_library_dept": "General Collections",
                          "image_no_calc": "L0011975",
                          "image_phys_format": "Book",
                          "image_tech_file_size": [
                              "5247788"
                          ],
                          "image_title": "Antonio Dionisi",
                          "image_use_restrictions": "CC-BY"
                      }
                    """
                  ),
                  sourceName = "miro",
                  s3Client = s3Client,
                  bucket = storageBucket
                )
              val miroHybridRecordMessage2 =
                hybridRecordNotificationMessage(
                  message = createValidMiroTransformableJson(
                    MiroID = "L0023034",
                    MiroCollection = "images-L",
                    data =
                      """
                      {
                          "image_cleared": "Y",
                          "image_copyright_cleared": "Y",
                          "image_image_desc": "Use of the guillotine",
                          "image_innopac_id": "12074536",
                          "image_keywords": [
                              "Surgery"
                          ],
                          "image_library_dept": "General Collections",
                          "image_no_calc": "L0023034",
                          "image_tech_file_size": [
                              "5710662"
                          ],
                          "image_title": "Greenfield Sluder, Tonsillectomy..., use of guillotine",
                          "image_use_restrictions": "CC-BY"
                      }
                    """
                  ),
                  sourceName = "miro",
                  s3Client = s3Client,
                  bucket = storageBucket
                )
              sqsClient.sendMessage(
                queue.url,
                JsonUtil.toJson(miroHybridRecordMessage1).get
              )

              sqsClient.sendMessage(
                queue.url,
                JsonUtil.toJson(miroHybridRecordMessage2).get
              )
              eventually {
                val works = getMessages[UnidentifiedWork](topic)
                works.distinct.length shouldBe >=(2)

              }
            }
          }
        }
      }
    }
  }
}
