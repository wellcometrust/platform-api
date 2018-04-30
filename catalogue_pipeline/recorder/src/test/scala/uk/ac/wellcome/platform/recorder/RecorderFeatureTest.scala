package uk.ac.wellcome.platform.recorder

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SQS}
import uk.ac.wellcome.models.work.internal.{IdentifierSchemes, SourceIdentifier, UnidentifiedWork}
import uk.ac.wellcome.s3.S3ObjectLocation
import uk.ac.wellcome.test.fixtures.LocalVersionedHybridStore

class RecorderFeatureTest
    extends FunSpec
      with Matchers
      with fixtures.Server
      with LocalVersionedHybridStore
      with Messaging
      with SQS {

  it("receives a transformed Work, and saves it to the VHS") {
    val title = "Not from Guildford after all"

    val work = UnidentifiedWork(
      title = Some(title),
      sourceIdentifier = SourceIdentifier(
        identifierScheme = IdentifierSchemes.miroImageNumber,
        value = "V0237865",
        ontologyType = "Work"
      ),
      version = 1
    )

    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withLocalDynamoDbTable { table =>
          withVersionedHybridStore(bucket = bucket, table = table) { _ =>

            val flags = sqsLocalFlags(queue) ++ s3LocalFlags(bucket) ++ dynamoDbLocalEndpointFlags(table)
            withServer(flags) { _ =>

              val messageBody = put[UnidentifiedWork](
                obj = work,
                location = S3ObjectLocation(
                  bucket = bucket.name, key = "work_message.json"
                )
              )

              sqsClient.sendMessage(queue.url, messageBody)

              eventually {
                assertStored[UnidentifiedWork](bucket, table, work)
              }
            }
          }
        }
      }
    }
  }

}
