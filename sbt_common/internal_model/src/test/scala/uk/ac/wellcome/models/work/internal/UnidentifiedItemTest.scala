package uk.ac.wellcome.models.work.internal

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.test.utils.JsonTestUtil
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._

class UnidentifiedItemTest extends FunSpec with Matchers with JsonTestUtil {

  val unidentifiedItemJson: String =
    s"""
      |{
      |  "sourceIdentifier": {
      |      "identifierType": {
      |        "id": "${IdentifierType("MiroImageNumber").id}",
      |        "label": "${IdentifierType("MiroImageNumber").label}",
      |        "ontologyType": "IdentifierType"
      |      },
      |      "ontologyType": "Item",
      |      "value": "value"
      |  },
      |  "identifiers": [
      |    {
      |      "identifierType": {
      |        "id": "${IdentifierType("MiroImageNumber").id}",
      |        "label": "${IdentifierType("MiroImageNumber").label}",
      |        "ontologyType": "IdentifierType"
      |      },
      |      "ontologyType": "Item",
      |      "value": "value"
      |    }
      |  ],
      |  "locations": [
      |    {
      |      "locationType": "location",
      |      "url" : "",
      |      "credit" : null,
      |      "license": {
      |        "licenseType": "${License_CCBY.licenseType}",
      |        "label": "${License_CCBY.label}",
      |        "url": "${License_CCBY.url}",
      |        "ontologyType": "License"
      |      },
      |      "type": "DigitalLocation",
      |      "ontologyType": "DigitalLocation"
      |    }
      |  ],
      |  "ontologyType": "Item"
      |}
    """.stripMargin

  val location = DigitalLocation(
    locationType = "location",
    url = "",
    license = License_CCBY
  )

  val identifier = SourceIdentifier(
    identifierType = IdentifierType("MiroImageNumber"),
    ontologyType = "Item",
    value = "value"
  )

  val unidentifiedItem = UnidentifiedItem(
    sourceIdentifier = identifier,
    identifiers = List(identifier),
    locations = List(location)
  )

  it("should serialise an unidentified Item as JSON") {
    val result = JsonUtil.toJson(unidentifiedItem)

    result.isSuccess shouldBe true
    assertJsonStringsAreEqual(result.get, unidentifiedItemJson)
  }

  it("should deserialize a JSON string as a unidentified Item") {
    val result = JsonUtil.fromJson[UnidentifiedItem](unidentifiedItemJson)

    result.isSuccess shouldBe true
    result.get shouldBe unidentifiedItem
  }
}
