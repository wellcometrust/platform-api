package uk.ac.wellcome.platform.matcher.locking
import java.time.Instant

import com.gu.scanamo.Scanamo
import com.gu.scanamo.error.DynamoReadError
import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.platform.matcher.fixtures.MatcherFixtures
import uk.ac.wellcome.platform.matcher.lockable.{FailedLockException, RowLock}
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb

import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DynamoLockingServiceTest
    extends FunSpec
    with MatcherFixtures
    with ScalaFutures {

  it("locks around a callback") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      withDynamoRowLockDao(lockTable) { dynamoRowLockDao =>
        withLockingService(dynamoRowLockDao) { lockingService =>
          val id = "id"
          val lockedDuringCallback =
            lockingService.withLocks(Set(id))(f = Future {
              assertOnlyHaveRowLockRecordIds(Set(id), lockTable)
            })

          whenReady(lockedDuringCallback) { _ =>
            assertNoRowLocks(lockTable)
          }
        }
      }
    }
  }

  it(
    "throws a FailedLockException and releases locks when a row lock fails") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      withDynamoRowLockDao(lockTable) { dynamoRowLockDao =>
        withLockingService(dynamoRowLockDao) { lockingService =>
          val idA = "id"
          val lockedId = "lockedId"
          givenLocks(Set(lockedId), "existingContext", lockTable)

          val eventuallyLockFails =
            lockingService.withLocks(Set(idA, lockedId))(f = Future {
              fail("Lock did not fail")
            })

          whenReady(eventuallyLockFails.failed) { failure =>
            failure shouldBe a[FailedLockException]
            // still expect original locks to exist
            assertOnlyHaveRowLockRecordIds(Set(lockedId), lockTable)
          }
        }
      }
    }
  }

  it("throws a FailedLockException and releases locks when a nested row lock fails") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      withDynamoRowLockDao(lockTable) { dynamoRowLockDao =>
        withLockingService(dynamoRowLockDao) { lockingService =>
          val idA = "idA"
          val idB = "idB"
          givenLocks(Set(idB), "existingContext", lockTable)

          val eventuallyLockFails =
            lockingService.withLocks(Set(idA))({
              lockingService.withLocks(Set(idB))(Future {
                fail("Lock did not fail")
              })
            })

          whenReady(eventuallyLockFails.failed) { failure =>
            failure shouldBe a[FailedLockException]
            // still expect original locks to exist
            assertOnlyHaveRowLockRecordIds(Set(idB), lockTable)
          }
        }
      }
    }
  }

  it("releases locks when the callback fails") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      withDynamoRowLockDao(lockTable) { dynamoRowLockDao =>
        withLockingService(dynamoRowLockDao) { lockingService =>
          case class ExpectedException() extends Exception()

          val id = "id"
          val eventuallyLockFails =
            lockingService.withLocks(Set(id))(f = Future {
              assertOnlyHaveRowLockRecordIds(Set(id), lockTable)
              throw new ExpectedException
            })

          whenReady(eventuallyLockFails.failed) { failure =>
            failure shouldBe a[ExpectedException]
            assertNoRowLocks(lockTable)
          }
        }
      }
    }
  }

  private def givenLocks(ids: Set[String],
                         contextId: String,
                         lockTable: LocalDynamoDb.Table) = {
    ids.foreach(
      id =>
        Scanamo.put[RowLock](dynamoDbClient)(lockTable.name)(
          aRowLock(id, contextId)))
  }

  private def aRowLock(id: String, contextId: String) = {
    RowLock(id, contextId, Instant.now, Instant.now.plusSeconds(100))
  }

  private def assertOnlyHaveRowLockRecordIds(
    expectedIds: Set[String],
    lockTable: LocalDynamoDb.Table): Any = {
    val locks: immutable.Seq[Either[DynamoReadError, RowLock]] =
      Scanamo.scan[RowLock](dynamoDbClient)(lockTable.name)
    val actualIds = locks.map(lock => lock.right.get.id).toSet
    actualIds shouldBe expectedIds
  }
}
