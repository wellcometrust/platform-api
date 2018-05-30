package uk.ac.wellcome.platform.matcher.storage

import com.gu.scanamo.Scanamo
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.matcher.fixtures.MatcherFixtures
import uk.ac.wellcome.platform.matcher.models.{
  LinkedWorkUpdate,
  LinkedWorksGraph,
  WorkNode
}

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
              LinkedWorkUpdate("Not-there", Set.empty))) { linkedWorkGraph =>
            linkedWorkGraph shouldBe LinkedWorksGraph(Set.empty)
          }
        }
      }
    }

    it(
      "returns a LinkedWork if it has no links and it's the only node in the setId") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
          val work =
            WorkNode(id = "A", referencedWorkIds = Nil, componentId = "A")
          Scanamo.put(dynamoDbClient)(table.name)(work)

          whenReady(workGraphStore.findAffectedWorks(
            LinkedWorkUpdate("A", Set.empty))) { linkedWorkGraph =>
            linkedWorkGraph shouldBe LinkedWorksGraph(Set(work))
          }
        }
      }
    }

    it("returns a LinkedWork and the links in the workUpdate") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
          val workA =
            WorkNode(id = "A", referencedWorkIds = Nil, componentId = "A")
          val workB =
            WorkNode(id = "B", referencedWorkIds = Nil, componentId = "B")
          Scanamo.put(dynamoDbClient)(table.name)(workA)
          Scanamo.put(dynamoDbClient)(table.name)(workB)

          whenReady(workGraphStore.findAffectedWorks(
            LinkedWorkUpdate("A", Set("B")))) { linkedWorkGraph =>
            linkedWorkGraph.linkedWorksSet shouldBe Set(workA, workB)
          }
        }
      }
    }

    it("returns a LinkedWork and the links in the database") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
          val workA =
            WorkNode(id = "A", referencedWorkIds = List("B"), componentId = "AB")
          val workB =
            WorkNode(id = "B", referencedWorkIds = Nil, componentId = "AB")
          Scanamo.put(dynamoDbClient)(table.name)(workA)
          Scanamo.put(dynamoDbClient)(table.name)(workB)

          whenReady(workGraphStore.findAffectedWorks(
            LinkedWorkUpdate("A", Set.empty))) { linkedWorkGraph =>
            linkedWorkGraph.linkedWorksSet shouldBe Set(workA, workB)
          }
        }
      }
    }

    it(
      "returns a LinkedWork and the links in the database more than one level down") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
          val workA =
            WorkNode(id = "A", referencedWorkIds = List("B"), componentId = "ABC")
          val workB =
            WorkNode(id = "B", referencedWorkIds = List("C"), componentId = "AB")
          val workC =
            WorkNode(id = "C", referencedWorkIds = Nil, componentId = "AB")
          Scanamo.put(dynamoDbClient)(table.name)(workA)
          Scanamo.put(dynamoDbClient)(table.name)(workB)
          Scanamo.put(dynamoDbClient)(table.name)(workC)

          whenReady(workGraphStore.findAffectedWorks(
            LinkedWorkUpdate("A", Set.empty))) { linkedWorkGraph =>
            linkedWorkGraph.linkedWorksSet shouldBe Set(workA, workB, workC)
          }
        }
      }
    }

    it(
      "returns a LinkedWork and the links in the database where an update joins two sets of works") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
          val workA =
            WorkNode(id = "A", referencedWorkIds = List("B"), componentId = "ABC")
          val workB =
            WorkNode(id = "B", referencedWorkIds = Nil, componentId = "AB")
          val workC =
            WorkNode(id = "C", referencedWorkIds = Nil, componentId = "C")
          Scanamo.put(dynamoDbClient)(table.name)(workA)
          Scanamo.put(dynamoDbClient)(table.name)(workB)
          Scanamo.put(dynamoDbClient)(table.name)(workC)

          whenReady(workGraphStore.findAffectedWorks(
            LinkedWorkUpdate("B", Set("C")))) { linkedWorkGraph =>
            linkedWorkGraph.linkedWorksSet shouldBe Set(workA, workB, workC)
          }
        }
      }
    }
  }

  describe("Put graph of linked works") {
    it("puts a simple graph") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
          val workA = WorkNode("A", List("B"), "A+B")
          val workB = WorkNode("B", Nil, "A+B")

          whenReady(workGraphStore.put(LinkedWorksGraph(Set(workA, workB)))) {
            _ =>
              val savedLinkedWorks = Scanamo
                .scan[WorkNode](dynamoDbClient)(table.name)
                .map(_.right.get)
              savedLinkedWorks should contain theSameElementsAs List(
                workA,
                workB)
          }
        }
      }
    }

    it("throws if linkedDao fails to put") {
      withLocalDynamoDbTable { table =>
        val mockLinkedWorkDao = mock[LinkedWorkDao]
        val expectedException = new RuntimeException("FAILED")
        when(mockLinkedWorkDao.put(any[WorkNode]))
          .thenReturn(Future.failed(expectedException))
        val workGraphStore = new WorkGraphStore(mockLinkedWorkDao)

        whenReady(
          workGraphStore
            .put(LinkedWorksGraph(Set(WorkNode("A", Nil, "A+B"))))
            .failed) { failedException =>
          failedException shouldBe expectedException
        }
      }
    }
  }

}
