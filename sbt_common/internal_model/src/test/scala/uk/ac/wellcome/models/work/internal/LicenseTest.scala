package uk.ac.wellcome.models.work.internal

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil

class LicenseTest extends FunSpec with Matchers {

  it("should serialise a License as JSON") {
    val result = JsonUtil.toJson[License](License_CCBY)
    result.isSuccess shouldBe true
    result.get shouldBe """{"id":"cc-by","label":"Attribution 4.0 International (CC BY 4.0)","url":"http://creativecommons.org/licenses/by/4.0/","ontologyType":"License"}"""
  }

  it("should deserialise a JSON string as a License") {
    val id = License_CC0.id
    val label = License_CC0.label
    val url = License_CC0.url

    val jsonString = s"""
      {
        "id": "$id",
        "label": "$label",
        "url": "$url",
        "ontologyType": "License"
      }"""
    val result = JsonUtil.fromJson[License](jsonString)
    result.isSuccess shouldBe true

    val license = result.get
    license.id shouldBe id
    license.label shouldBe label
    license.url shouldBe url
  }
}
