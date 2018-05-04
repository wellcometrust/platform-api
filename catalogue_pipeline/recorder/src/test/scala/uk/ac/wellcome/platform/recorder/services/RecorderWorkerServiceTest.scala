package uk.ac.wellcome.platform.recorder.services

import akka.actor.ActorSystem
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.gu.scanamo.{DynamoFormat, Scanamo}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.Assertion
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.messaging.message.MessageReader
import uk.ac.wellcome.messaging.metrics.MetricsSender
import uk.ac.wellcome.messaging.sns.{SNSConfig, SNSWriter}
import uk.ac.wellcome.messaging.sqs.{SQSConfig, SQSMessage, SQSReader}
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SNS, SQS}
import uk.ac.wellcome.platform.recorder.models.RecorderWorkEntry
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.models.work.internal.{IdentifierSchemes, SourceIdentifier, UnidentifiedWork}
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.test.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}
import uk.ac.wellcome.messaging.test.fixtures
import uk.ac.wellcome.storage.vhs.HybridRecord
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class RecorderWorkerServiceTest
    extends FunSpec
    with Matchers
    with MockitoSugar
    with Akka
    with LocalVersionedHybridStore
    with SQS
    with ScalaFutures
    with Messaging
    with fixtures.MetricsSender
    with ExtendedPatience {

  val title = "Whose umbrella did I find?"

  val sourceIdentifier = SourceIdentifier(
    identifierScheme = IdentifierSchemes.miroImageNumber,
    value = "U8634924",
    ontologyType = "Work"
  )

  val work = UnidentifiedWork(
    title = Some(title),
    sourceIdentifier = sourceIdentifier,
    identifiers = List(sourceIdentifier),
    version = 2
  )

  it("returns a successful Future if the Work is recorded successfully") {
    withLocalDynamoDbTable { table =>
      withLocalS3Bucket { bucket =>
        withLocalSnsTopic { topic =>
          withRecorderWorkerService(table, bucket, topic) { service =>
            whenReady(service.processMessage(work = work)) { _ =>
              assertStoredSingleWork(bucket, table, work)
            }
          }
        }
      }
    }
  }

  it("doesn't overwrite a newer work with an older work") {
    val olderWork = work
    val newerWork = work.copy(version = 10, title = Some("A nice new thing"))

    withLocalDynamoDbTable { table =>
      withLocalS3Bucket { bucket =>
        withLocalSnsTopic { topic =>
          withRecorderWorkerService(table, bucket, topic) { service =>
            whenReady(service.processMessage(work = newerWork)) { _ =>
              whenReady(service.processMessage(work = olderWork)) { _ =>
                assertStoredSingleWork(bucket, table, newerWork)
              }
            }
          }
        }
      }
    }
  }

  it("overwrites an older work with an newer work") {
    val olderWork = work
    val newerWork = work.copy(version = 10, title = Some("A nice new thing"))

    withLocalDynamoDbTable { table =>
      withLocalS3Bucket { bucket =>
        withLocalSnsTopic { topic =>
          withRecorderWorkerService(table, bucket, topic) { service =>
            whenReady(service.processMessage(work = olderWork)) { _ =>
              whenReady(service.processMessage(work = newerWork)) { _ =>
                assertStoredSingleWork(bucket, table, newerWork, expectedVhsVersion = 2)
              }
            }
          }
        }
      }
    }
  }

  it("returns a failed Future if saving to S3 fails") {
    withLocalDynamoDbTable { table =>
      val badBucket = Bucket(name = "bad-bukkit")
      withLocalSnsTopic { topic =>
        withRecorderWorkerService(table, badBucket, topic) { service =>
          whenReady(service.processMessage(work = work).failed) { err =>
            err shouldBe a[AmazonServiceException]
          }
        }
      }
    }
  }

  it("returns a failed Future if saving to DynamoDB fails") {
    val badTable = Table(name = "bad-table", index = "bad-index")
    withLocalS3Bucket { bucket =>
      withLocalSnsTopic { topic =>
        withRecorderWorkerService(badTable, bucket, topic) { service =>
          whenReady(service.processMessage(work = work).failed) { err =>
            err shouldBe a[AmazonServiceException]
          }
        }
      }
    }
  }

  private def assertStoredSingleWork(bucket: Bucket, table: Table, expectedWork: UnidentifiedWork, expectedVhsVersion: Int = 1) = {
    val actualRecords: List[HybridRecord] =
      Scanamo
        .scan[HybridRecord](dynamoDbClient)(table.name)
        .map(_.right.get)

    actualRecords.size shouldBe 1

    val hybridRecord: HybridRecord = actualRecords.head
    hybridRecord.id shouldBe s"${expectedWork.sourceIdentifier.identifierScheme.toString}/${expectedWork.sourceIdentifier.value}"
    hybridRecord.version shouldBe expectedVhsVersion

    val content = getContentFromS3(
      bucket = bucket,
      key = hybridRecord.s3key
    )
    fromJson[RecorderWorkEntry](content).get shouldBe RecorderWorkEntry(expectedWork)
  }

  private def withRecorderWorkerService(table: Table, bucket: Bucket, topic: Topic)(
    testWith: TestWith[RecorderWorkerService, Assertion]) = {
    withMessageReader[UnidentifiedWork, Unit](bucket, topic) { messageReader =>
      withActorSystem { actorSystem =>
        withMetricsSender(actorSystem) { metricsSender =>
          withLocalSqsQueue { queue =>
            withVersionedHybridStore[RecorderWorkEntry, Unit](bucket = bucket, table = table) { versionedHybridStore =>
              val workerService = new RecorderWorkerService(
                versionedHybridStore = versionedHybridStore,
                sqsReader = new SQSReader(
                  sqsClient = sqsClient,
                  sqsConfig = SQSConfig(
                    queueUrl = queue.url,
                    waitTime = 1 second,
                    maxMessages = 1
                  )
                ),
                messageReader = messageReader,
                system = actorSystem,
                metrics = metricsSender
              )

              try {
                testWith(workerService)
              } finally {
                workerService.stop()
              }
            }
          }
        }
      }
    }
  }
}
