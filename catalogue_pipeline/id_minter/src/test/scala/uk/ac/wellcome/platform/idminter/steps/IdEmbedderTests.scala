package uk.ac.wellcome.platform.idminter.steps

import io.circe.parser._
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.models.work.test.util.WorksUtil
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}
import uk.ac.wellcome.test.utils.{ExtendedPatience, JsonTestUtil}
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.Try

class IdEmbedderTests
    extends FunSpec
    with ScalaFutures
    with Matchers
    with MockitoSugar
    with Akka
    with JsonTestUtil
    with ExtendedPatience
    with WorksUtil {

  private def withIdEmbedder(
    testWith: TestWith[(IdentifierGenerator, IdEmbedder), Assertion]) = {
    withActorSystem { actorSystem =>
      val identifierGenerator: IdentifierGenerator =
        mock[IdentifierGenerator]

      val idEmbedder = new IdEmbedder(
        identifierGenerator = identifierGenerator
      )

      testWith((identifierGenerator, idEmbedder))
    }
  }

  it("sets the canonicalId given by the IdentifierGenerator on the work") {
    val identifier = SourceIdentifier(
      identifierType = IdentifierType("miro-image-number"),
      ontologyType = "Work",
      value = "1234"
    )

    val originalWork = unidentifiedWorkWith(
      title = "crap",
      sourceIdentifier = identifier)

    val newCanonicalId = "5467"

    withIdEmbedder {
      case (identifierGenerator, idEmbedder) =>
        setUpIdentifierGeneratorMock(
          mockIdentifierGenerator = identifierGenerator,
          sourceIdentifier = identifier,
          ontologyType = originalWork.ontologyType,
          newCanonicalId = newCanonicalId
        )

        val newWorkFuture = idEmbedder.embedId(
          json = parse(
            toJson(originalWork).get
          ).right.get
        )

        val expectedWork = identifiedWorkWith(
          canonicalId = newCanonicalId,
          title = originalWork.title,
          sourceIdentifier = originalWork.sourceIdentifier,
          version = originalWork.version
        )

        whenReady(newWorkFuture) { newWorkJson =>
          assertJsonStringsAreEqual(
            newWorkJson.toString(),
            toJson(expectedWork).get
          )
        }
    }
  }

  it("mints identifiers for creators in work") {
    val workIdentifier = SourceIdentifier(
      identifierType = IdentifierType("miro-image-number"),
      ontologyType = "Work",
      value = "1234"
    )

    val creatorIdentifier = SourceIdentifier(
      identifierType = IdentifierType("lc-names"),
      ontologyType = "Person",
      value = "1234"
    )

    val person = Person(label = "The Librarian")
    val originalWork = unidentifiedWorkWith(
      title = "crap",
      sourceIdentifier = workIdentifier,
      contributors = List(
        Contributor(
          agent = Identifiable(person, sourceIdentifier = creatorIdentifier))
      )
    )

    val newWorkCanonicalId = "5467"
    val newCreatorCanonicalId = "8901"

    withIdEmbedder {
      case (identifierGenerator, idEmbedder) =>
        setUpIdentifierGeneratorMock(
          mockIdentifierGenerator = identifierGenerator,
          sourceIdentifier = workIdentifier,
          ontologyType = originalWork.ontologyType,
          newCanonicalId = newWorkCanonicalId
        )

        setUpIdentifierGeneratorMock(
          mockIdentifierGenerator = identifierGenerator,
          sourceIdentifier = creatorIdentifier,
          ontologyType = "Person",
          newCanonicalId = newCreatorCanonicalId
        )

        val newWorkFuture = idEmbedder.embedId(
          json = parse(
            toJson(originalWork).get
          ).right.get
        )

        val expectedWork = identifiedWorkWith(
          canonicalId = newWorkCanonicalId,
          title = originalWork.title,
          sourceIdentifier = originalWork.sourceIdentifier,
          contributors = List(
            Contributor(
              agent = Identified(
                agent = person,
                canonicalId = newCreatorCanonicalId,
                sourceIdentifier = creatorIdentifier))
          ),
          version = originalWork.version
        )

        whenReady(newWorkFuture) { newWorkJson =>
          assertJsonStringsAreEqual(
            newWorkJson.toString(),
            toJson(expectedWork).get
          )
        }
    }
  }

  it("returns a failed future if the call to IdentifierGenerator fails") {
    val identifier = SourceIdentifier(
      identifierType = IdentifierType("miro-image-number"),
      ontologyType = "Work",
      value = "1234"
    )

    val originalWork = unidentifiedWorkWith(
      title = "crap",
      sourceIdentifier = identifier)

    val expectedException = new Exception("Aaaaah something happened!")

    withIdEmbedder {
      case (identifierGenerator, idEmbedder) =>
        when(
          identifierGenerator
            .retrieveOrGenerateCanonicalId(
              identifier
            )
        ).thenReturn(Try(throw expectedException))

        val newWorkFuture =
          idEmbedder.embedId(json = parse(toJson(originalWork).get).right.get)

        whenReady(newWorkFuture.failed) { exception =>
          exception shouldBe expectedException
        }
    }
  }

  it("adds canonicalIds to all items") {
    val identifier = SourceIdentifier(
      identifierType = IdentifierType("miro-image-number"),
      ontologyType = "Item",
      value = "1234"
    )

    val originalItem1 = Identifiable(
      sourceIdentifier = identifier,
      agent = Item(locations = List())
    )

    val originalItem2 = Identifiable(
      sourceIdentifier = SourceIdentifier(
        identifierType = IdentifierType("miro-image-number"),
        ontologyType = "Item",
        value = "1235"
      ),
      agent = Item(locations = List())
    )

    val originalWork = unidentifiedWorkWith(
      title = "crap",
      sourceIdentifier = identifier,
      items = List(originalItem1, originalItem2))

    val newItemCanonicalId1 = "item1-canonical-id"
    val newItemCanonicalId2 = "item2-canonical-id"

    withIdEmbedder {
      case (identifierGenerator, idEmbedder) =>
        setUpIdentifierGeneratorMock(
          mockIdentifierGenerator = identifierGenerator,
          sourceIdentifier = identifier,
          ontologyType = originalWork.ontologyType,
          newCanonicalId = "work-canonical-id"
        )

        setUpIdentifierGeneratorMock(
          mockIdentifierGenerator = identifierGenerator,
          sourceIdentifier = originalItem1.sourceIdentifier,
          ontologyType = originalItem1.agent.ontologyType,
          newCanonicalId = newItemCanonicalId1
        )

        setUpIdentifierGeneratorMock(
          mockIdentifierGenerator = identifierGenerator,
          sourceIdentifier = originalItem2.sourceIdentifier,
          ontologyType = originalItem2.agent.ontologyType,
          newCanonicalId = newItemCanonicalId2
        )

        val eventualWork = idEmbedder.embedId(
          parse(
            toJson(originalWork).get
          ).right.get
        )

        val expectedItem1 = createItem(
          sourceIdentifier = originalItem1.sourceIdentifier,
          canonicalId = newItemCanonicalId1,
          locations = originalItem1.agent.locations
        )

        val expectedItem2 = createItem(
          sourceIdentifier = originalItem2.sourceIdentifier,
          canonicalId = newItemCanonicalId2,
          locations = originalItem2.agent.locations
        )

        whenReady(eventualWork) { json =>
          val work = fromJson[IdentifiedWork](json.toString()).get

          val actualItem1 = work.items.head
          val actualItem2 = work.items.tail.head

          assertJsonStringsAreEqual(
            toJson(actualItem1).get,
            toJson(expectedItem1).get
          )

          assertJsonStringsAreEqual(
            toJson(actualItem2).get,
            toJson(expectedItem2).get
          )
        }
    }
  }

  describe("unidentifiable objects should pass through unchanged") {
    it("an empty map") {
      assertIdEmbedderDoesNothing("""{}""")
    }

    it("a map with some string keys") {
      assertIdEmbedderDoesNothing("""{
        "so": "sofia",
        "sk": "skopje"
      }""")
    }

    it("a map with some list objects") {
      assertIdEmbedderDoesNothing("""{
        "te": "tehran",
        "ta": [
          "tallinn",
          "tashkent"
        ]
      }""")
    }

    it("a complex nested structure") {
      assertIdEmbedderDoesNothing("""{
        "u": "ulan bator",
        "v": [
          "vatican city",
          {
            "vic": "victoria",
            "vie": "vienna",
            "vil": "vilnius"
          }
        ],
        "w": {
          "wa": [
            "warsaw",
            "washington dc"
          ],
          "we": "wellington",
          "wi": {
            "win": "windhoek"
          }
        }
      }""")
    }
  }

  describe("identifiable objects should be updated correctly") {

    it("identify a document that is Identifiable") {

      val ontologyType = "false capitals"
      val sourceIdentifier = SourceIdentifier(
        identifierType = IdentifierType("miro-image-number"),
        ontologyType = ontologyType,
        "sydney"
      )

      val newCanonicalId =
        generateMockCanonicalId(sourceIdentifier, ontologyType)

      withIdEmbedder {
        case (identifierGenerator, idEmbedder) =>
          setUpIdentifierGeneratorMock(
            mockIdentifierGenerator = identifierGenerator,
            sourceIdentifier = sourceIdentifier,
            ontologyType = ontologyType,
            newCanonicalId = newCanonicalId
          )

          val inputJson = s"""
        {
          "sourceIdentifier": {
            "identifierType": {
              "id": "${sourceIdentifier.identifierType.id}",
              "label": "${sourceIdentifier.identifierType.label}",
              "ontologyType": "${sourceIdentifier.identifierType.ontologyType}"
            },
            "ontologyType": "$ontologyType",
            "value": "${sourceIdentifier.value}"
          },
          "ontologyType": "$ontologyType"
        }
        """

          val outputJson = s"""
        {
          "canonicalId": "$newCanonicalId",
          "sourceIdentifier": {
            "identifierType": {
              "id": "${sourceIdentifier.identifierType.id}",
              "label": "${sourceIdentifier.identifierType.label}",
              "ontologyType": "${sourceIdentifier.identifierType.ontologyType}"
            },
            "ontologyType": "$ontologyType",
            "value": "${sourceIdentifier.value}"
          },
          "ontologyType": "$ontologyType"
        }
        """

          val eventualJson = idEmbedder.embedId(parse(inputJson).right.get)

          whenReady(eventualJson) { json =>
            assertJsonStringsAreEqual(json.toString, outputJson)
          }
      }
    }

    it("identify a document with a key that is identifiable") {
      val ontologyType = "fictional cities"

      val sourceIdentifier = SourceIdentifier(
        identifierType = IdentifierType("miro-image-number"),
        ontologyType = ontologyType,
        "king's landing"
      )

      val newCanonicalId = generateMockCanonicalId(
        sourceIdentifier,
        ontologyType
      )

      withIdEmbedder {
        case (identifierGenerator, idEmbedder) =>
          setUpIdentifierGeneratorMock(
            mockIdentifierGenerator = identifierGenerator,
            sourceIdentifier = sourceIdentifier,
            ontologyType = ontologyType,
            newCanonicalId = newCanonicalId
          )

          val inputJson = s"""
        {
          "ke": null,
          "ki": "kiev",
          "item": {
            "sourceIdentifier": {
              "identifierType": {
                "id": "${sourceIdentifier.identifierType.id}",
                "label": "${sourceIdentifier.identifierType.label}",
                "ontologyType": "${sourceIdentifier.identifierType.ontologyType}"
              },
              "ontologyType": "$ontologyType",
              "value": "${sourceIdentifier.value}"
            },
            "ontologyType": "$ontologyType"
          }
        }
        """

          val outputJson = s"""
        {
          "ke": null,
          "ki": "kiev",
          "item": {
            "canonicalId": "$newCanonicalId",
            "sourceIdentifier": {
              "identifierType": {
                "id": "${sourceIdentifier.identifierType.id}",
                "label": "${sourceIdentifier.identifierType.label}",
                "ontologyType": "${sourceIdentifier.identifierType.ontologyType}"
              },
              "ontologyType": "$ontologyType",
              "value": "${sourceIdentifier.value}"
            },
            "ontologyType": "$ontologyType"
          }
        }
        """

          val eventualJson = idEmbedder.embedId(parse(inputJson).right.get)

          whenReady(eventualJson) { json =>
            assertJsonStringsAreEqual(json.toString, outputJson)
          }
      }
    }
  }

  def generateMockCanonicalId(
    sourceIdentifier: SourceIdentifier,
    ontologyType: String
  ): String =
    s"${sourceIdentifier.identifierType.id}==${sourceIdentifier.value}"

  private def setUpIdentifierGeneratorMock(
    mockIdentifierGenerator: IdentifierGenerator,
    sourceIdentifier: SourceIdentifier,
    ontologyType: String,
    newCanonicalId: String) = {
    when(
      mockIdentifierGenerator
        .retrieveOrGenerateCanonicalId(
          sourceIdentifier
        )
    ).thenReturn(Try(newCanonicalId))
  }

  private def assertIdEmbedderDoesNothing(jsonString: String) = {
    withIdEmbedder {
      case (_, idEmbedder) =>
        val eventualJson = idEmbedder.embedId(parse(jsonString).right.get)
        whenReady(eventualJson) { json =>
          assertJsonStringsAreEqual(json.toString(), jsonString)
        }
    }
  }

}
