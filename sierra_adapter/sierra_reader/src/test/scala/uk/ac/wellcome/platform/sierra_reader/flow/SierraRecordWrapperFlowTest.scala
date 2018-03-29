package uk.ac.wellcome.platform.sierra_reader.flow

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Flow, Sink, Source}
import io.circe.Json
import io.circe.parser._
import org.scalatest.compatible.Assertion
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.SierraRecord
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.concurrent.ExecutionContextExecutor

class SierraRecordWrapperFlowTest
    extends FunSpec
    with Akka
    with ScalaFutures
    with ExtendedPatience
    with Matchers {

  private def withRecordWrapperFlow(actorSystem: ActorSystem)(
    testWith: TestWith[Flow[Json, SierraRecord, NotUsed], Assertion]) = {
    implicit val executionContext: ExecutionContextExecutor =
      actorSystem.dispatcher

    val wrapperFlow = SierraRecordWrapperFlow()

    testWith(wrapperFlow)
  }

  it("creates a SierraRecord from a bib") {
    withActorSystem { actorSystem =>
      withRecordWrapperFlow(actorSystem) { wrapperFlow =>
        withMaterializer(actorSystem) { materializer =>
          val id = "100001"
          val updatedDate = "2013-12-13T12:43:16Z"
          val json = parse(s"""
          |{
          | "id": "$id",
          | "updatedDate": "$updatedDate"
          |}
        """.stripMargin).right.get

          val expectedRecord = SierraRecord(
            id = id,
            data = json.noSpaces,
            modifiedDate = updatedDate
          )

          val futureRecord = Source
            .single(json)
            .via(wrapperFlow)
            .runWith(Sink.head)

          whenReady(futureRecord) { sierraRecord =>
            sierraRecord shouldBe expectedRecord
          }
        }
      }
    }
  }

  it("creates a SierraRecord from an item") {
    withActorSystem { actorSystem =>
      withRecordWrapperFlow(actorSystem) { wrapperFlow =>
        withMaterializer(actorSystem) { materializer =>
          val id = "400004"
          val updatedDate = "2014-04-14T14:14:14Z"
          val json = parse(s"""
          |{
          | "id": "$id",
          | "updatedDate": "$updatedDate",
          | "bibIds": ["4", "44", "444", "4444"]
          |}
        """.stripMargin).right.get

          val expectedRecord = SierraRecord(
            id = id,
            data = json.noSpaces,
            modifiedDate = updatedDate
          )

          val futureRecord = Source
            .single(json)
            .via(wrapperFlow)
            .runWith(Sink.head)

          whenReady(futureRecord) { sierraRecord =>
            sierraRecord shouldBe expectedRecord
          }
        }
      }
    }
  }

  it("is able to handle deleted bibs") {
    withActorSystem { actorSystem =>
      withRecordWrapperFlow(actorSystem) { wrapperFlow =>
        withMaterializer(actorSystem) { materializer =>
          val id = "1357947"
          val deletedDate = "2014-01-31"
          val json = parse(s"""{
                            |  "id" : "$id",
                            |  "deletedDate" : "$deletedDate",
                            |  "deleted" : true
                            |}""".stripMargin).right.get

          val expectedRecord = SierraRecord(
            id = id,
            data = json.noSpaces,
            modifiedDate = s"${deletedDate}T00:00:00Z"
          )

          val futureRecord = Source
            .single(json)
            .via(wrapperFlow)
            .runWith(Sink.head)

          whenReady(futureRecord) { sierraRecord =>
            sierraRecord shouldBe expectedRecord
          }
        }
      }
    }
  }

  it("fails the stream if the record contains invalid JSON") {
    withActorSystem { actorSystem =>
      withRecordWrapperFlow(actorSystem) { wrapperFlow =>
        withMaterializer(actorSystem) { materializer =>
          val invalidSierraJson = parse(s"""{
          | "missing": ["id", "updatedDate"],
          | "reason": "This JSON will not pass!",
          |  "comment": "XML is coming!"
          |}""".stripMargin).right.get

          val futureUnit = Source
            .single(invalidSierraJson)
            .via(wrapperFlow)
            .runWith(Sink.head)

          whenReady(futureUnit.failed) { _ =>
            true shouldBe true
          }
        }
      }
    }
  }
}
