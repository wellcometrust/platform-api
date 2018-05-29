package uk.ac.wellcome.platform.matcher.storage

import com.gu.scanamo.Scanamo
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.matcher.WorkNode
import uk.ac.wellcome.platform.matcher.fixtures.MatcherFixtures
import uk.ac.wellcome.platform.matcher.models.{WorkGraph, WorkUpdate}

import scala.concurrent.Future

class WorkGraphStoreTest
    extends FunSpec
    with Matchers
    with MockitoSugar
    with ScalaFutures
    with MatcherFixtures {

  describe("Get graph of linked works") {
    it("returns nothing if there are no matching graphs") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
          whenReady(
            workGraphStore.findAffectedWorks(
<<<<<<< HEAD
              WorkUpdate("Not-there", Set.empty))) { workGraph =>
            workGraph shouldBe WorkGraph(Set.empty)
=======
              LinkedWorkUpdate("Not-there", 0, Set.empty))) { linkedWorkGraph =>
            linkedWorkGraph shouldBe LinkedWorksGraph(Set.empty)
>>>>>>> works are versioned
          }
        }
      }
    }

    it(
      "returns a WorkNode if it has no links and it's the only node in the setId") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
<<<<<<< HEAD
          val workNode = WorkNode("A", Nil, "A")
          Scanamo.put(dynamoDbClient)(table.name)(workNode)

          whenReady(
            workGraphStore.findAffectedWorks(WorkUpdate("A", Set.empty))) {
            workGraph =>
              workGraph shouldBe WorkGraph(Set(workNode))
=======
          val work = LinkedWork(workId = "A", version = 0, linkedIds = Nil, setId = "A")
          Scanamo.put(dynamoDbClient)(table.name)(work)

          whenReady(workGraphStore.findAffectedWorks(
            LinkedWorkUpdate("A", 0, Set.empty))) { linkedWorkGraph =>
            linkedWorkGraph shouldBe LinkedWorksGraph(Set(work))
>>>>>>> works are versioned
          }
        }
      }
    }

    it("returns a WorkNode and the links in the workUpdate") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
<<<<<<< HEAD
          val workNodeA = WorkNode("A", Nil, "A")
          val workNodeB = WorkNode("B", Nil, "B")
          Scanamo.put(dynamoDbClient)(table.name)(workNodeA)
          Scanamo.put(dynamoDbClient)(table.name)(workNodeB)

          whenReady(
            workGraphStore.findAffectedWorks(WorkUpdate("A", Set("B")))) {
            workGraph =>
              workGraph.nodes shouldBe Set(workNodeA, workNodeB)
=======
          val workA = LinkedWork(workId = "A", version = 0, linkedIds = Nil, setId = "A")
          val workB = LinkedWork(workId = "B", version = 0, linkedIds = Nil, setId = "B")
          Scanamo.put(dynamoDbClient)(table.name)(workA)
          Scanamo.put(dynamoDbClient)(table.name)(workB)

          whenReady(workGraphStore.findAffectedWorks(
            LinkedWorkUpdate("A", 0, Set("B")))) { linkedWorkGraph =>
            linkedWorkGraph.linkedWorksSet shouldBe Set(workA, workB)
>>>>>>> works are versioned
          }
        }
      }
    }

    it("returns a WorkNode and the links in the database") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
<<<<<<< HEAD
          val workNodeA = WorkNode("A", List("B"), "AB")
          val workNodeB = WorkNode("B", Nil, "AB")
          Scanamo.put(dynamoDbClient)(table.name)(workNodeA)
          Scanamo.put(dynamoDbClient)(table.name)(workNodeB)

          whenReady(
            workGraphStore.findAffectedWorks(WorkUpdate("A", Set.empty))) {
            workGraph =>
              workGraph.nodes shouldBe Set(workNodeA, workNodeB)
=======
          val workA =
            LinkedWork(workId = "A", version = 0, linkedIds = List("B"), setId = "AB")
          val workB = LinkedWork(workId = "B", version = 0, linkedIds = Nil, setId = "AB")
          Scanamo.put(dynamoDbClient)(table.name)(workA)
          Scanamo.put(dynamoDbClient)(table.name)(workB)

          whenReady(workGraphStore.findAffectedWorks(
            LinkedWorkUpdate("A", 0, Set.empty))) { linkedWorkGraph =>
            linkedWorkGraph.linkedWorksSet shouldBe Set(workA, workB)
>>>>>>> works are versioned
          }
        }
      }
    }

    it(
      "returns a WorkNode and the links in the database more than one level down") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
<<<<<<< HEAD
          val workNodeA = WorkNode("A", List("B"), "ABC")
          val workNodeB = WorkNode("B", List("C"), "ABC")
          val workNodeC = WorkNode("C", Nil, "ABC")
          Scanamo.put(dynamoDbClient)(table.name)(workNodeA)
          Scanamo.put(dynamoDbClient)(table.name)(workNodeB)
          Scanamo.put(dynamoDbClient)(table.name)(workNodeC)

          whenReady(
            workGraphStore.findAffectedWorks(WorkUpdate("A", Set.empty))) {
            workGraph =>
              workGraph.nodes shouldBe Set(workNodeA, workNodeB, workNodeC)
=======
          val workA =
            LinkedWork(workId = "A", version = 0, linkedIds = List("B"), setId = "ABC")
          val workB =
            LinkedWork(workId = "B", version = 0, linkedIds = List("C"), setId = "ABC")
          val workC = LinkedWork(workId = "C", version = 0, linkedIds = Nil, setId = "ABC")
          Scanamo.put(dynamoDbClient)(table.name)(workA)
          Scanamo.put(dynamoDbClient)(table.name)(workB)
          Scanamo.put(dynamoDbClient)(table.name)(workC)

          whenReady(workGraphStore.findAffectedWorks(
            LinkedWorkUpdate("A", 0, Set.empty))) { linkedWorkGraph =>
            linkedWorkGraph.linkedWorksSet shouldBe Set(workA, workB, workC)
>>>>>>> works are versioned
          }
        }
      }
    }

    it(
      "returns a WorkNode and the links in the database where an update joins two sets of works") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
<<<<<<< HEAD
          val workNodeA = WorkNode("A", List("B"), "AB")
          val workNodeB = WorkNode("B", Nil, "AB")
          val workNodeC = WorkNode("C", Nil, "C")
          Scanamo.put(dynamoDbClient)(table.name)(workNodeA)
          Scanamo.put(dynamoDbClient)(table.name)(workNodeB)
          Scanamo.put(dynamoDbClient)(table.name)(workNodeC)

          whenReady(
            workGraphStore.findAffectedWorks(WorkUpdate("B", Set("C")))) {
            workGraph =>
              workGraph.nodes shouldBe Set(workNodeA, workNodeB, workNodeC)
=======
          val workA =
            LinkedWork(workId = "A", version = 0, linkedIds = List("B"), setId = "AB")
          val workB = LinkedWork(workId = "B", version = 0, linkedIds = Nil, setId = "AB")
          val workC = LinkedWork(workId = "C", version = 0, linkedIds = Nil, setId = "C")
          Scanamo.put(dynamoDbClient)(table.name)(workA)
          Scanamo.put(dynamoDbClient)(table.name)(workB)
          Scanamo.put(dynamoDbClient)(table.name)(workC)

          whenReady(workGraphStore.findAffectedWorks(
            LinkedWorkUpdate("B", 0, Set("C")))) { linkedWorkGraph =>
            linkedWorkGraph.linkedWorksSet shouldBe Set(workA, workB, workC)
>>>>>>> works are versioned
          }
        }
      }
    }
  }

  describe("Put graph of linked works") {
    it("puts a simple graph") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
<<<<<<< HEAD
          val workNodeA = WorkNode("A", List("B"), "A+B")
          val workNodeB = WorkNode("B", Nil, "A+B")
=======
          val workA = LinkedWork("A", version = 0, List("B"), "A+B")
          val workB = LinkedWork("B", version = 0, Nil, "A+B")
>>>>>>> works are versioned

          whenReady(workGraphStore.put(WorkGraph(Set(workNodeA, workNodeB)))) {
            _ =>
              val savedLinkedWorks = Scanamo
                .scan[WorkNode](dynamoDbClient)(table.name)
                .map(_.right.get)
              savedLinkedWorks should contain theSameElementsAs List(
                workNodeA,
                workNodeB)
          }
        }
      }
    }

    it("throws if linkedDao fails to put") {
      withLocalDynamoDbTable { table =>
        val mockWorkNodeDao = mock[WorkNodeDao]
        val expectedException = new RuntimeException("FAILED")
        when(mockWorkNodeDao.put(any[WorkNode]))
          .thenReturn(Future.failed(expectedException))
        val workGraphStore = new WorkGraphStore(mockWorkNodeDao)

        whenReady(
          workGraphStore
<<<<<<< HEAD
            .put(WorkGraph(Set(WorkNode("A", Nil, "A+B"))))
=======
            .put(LinkedWorksGraph(Set(LinkedWork("A", version = 0, Nil, "A+B"))))
>>>>>>> works are versioned
            .failed) { failedException =>
          failedException shouldBe expectedException
        }
      }
    }
  }
}
