package uk.ac.wellcome.platform.archive.archivist

import java.net.URI

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.archive.archivist.fixtures.{Archivist => ArchivistFixture}
import uk.ac.wellcome.platform.archive.common.models.{ArchiveComplete, BagLocation}
import uk.ac.wellcome.platform.archive.common.progress.fixtures.ArchiveProgressMonitorFixture
import uk.ac.wellcome.platform.archive.common.progress.models.ArchiveProgress
import uk.ac.wellcome.test.utils.ExtendedPatience

// TODO: Test file boundaries
// TODO: Test shutdown mid-stream does not succeed

class ArchivistFeatureTest
  extends FunSpec
    with Matchers
    with ScalaFutures
    with MetricsSenderFixture
    with ProgressMonitorFixture
    with ArchivistFixture
    with ExtendedPatience {

  val callbackUrl = new URI("http://localhost/archive/complete")

  it("downloads, uploads and verifies a BagIt bag") {
    withArchivist {
      case (
        ingestBucket,
        storageBucket,
        queuePair,
        topic,
        progressTable,
        archivist) =>
        sendFakeBag(ingestBucket, Some(callbackUrl), queuePair) {
          case (requestId, uploadLocation, validBag) =>
            archivist.run()
            eventually {
              listKeysInBucket(storageBucket) should have size 27

              assertQueuePairSizes(queuePair, 0, 0)

              assertSnsReceivesOnly(
                ArchiveComplete(
                  requestId,
                  BagLocation(storageBucket.name, "archive", validBag),
                  Some(callbackUrl)
                ),
                topic
              )
            }
        }
    }
  }

  it("fails when ingesting an invalid bag") {
    withArchivist {
      case (
        ingestBucket,
        storageBucket,
        queuePair,
        topic,
        progressTable,
        archivist) =>
        sendFakeBag(ingestBucket, Some(callbackUrl), queuePair, false) { _ =>
          archivist.run()
          eventually {
            assertQueuePairSizes(queuePair, 0, 1)
            assertSnsReceivesNothing(topic)
          }
        }
    }
  }

  it("continues after failure") {
    withArchivist {
      case (
        ingestBucket,
        storageBucket,
        queuePair,
        topic,
        progressTable,
        archivist) => {

        archivist.run()

        sendFakeBag(ingestBucket, Some(callbackUrl), queuePair) {
          case (requestId1, _, validBag1) =>

            sendFakeBag(ingestBucket, Some(callbackUrl), queuePair, valid = false) { _ =>

              sendFakeBag(ingestBucket, Some(callbackUrl), queuePair) {
                case (requestId2, _, validBag2) =>

                  sendFakeBag(ingestBucket, Some(callbackUrl), queuePair, valid = false) { _ =>

                    eventually {

                      //assertQueuePairSizes(queuePair, 0, 2)

                      assertSnsReceives(
                        Set(
                          ArchiveComplete(
                            requestId1,
                            BagLocation(
                              storageBucket.name,
                              "archive",
                              validBag1),
                            Some(callbackUrl)
                          ),
                          ArchiveComplete(
                            requestId2,
                            BagLocation(
                              storageBucket.name,
                              "archive",
                              validBag2),
                            Some(callbackUrl)
                          )
                        ),
                        topic
                      )
                    }
                  }
              }
            }
        }
      }
    }
  }
}
