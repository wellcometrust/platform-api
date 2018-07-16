package uk.ac.wellcome.models.transformable.sierra

import org.scalatest.{FunSpec, Matchers}
import org.scalatest.prop.TableDrivenPropertyChecks._

class SierraRecordNumbersTest extends FunSpec with Matchers {

  describe("addCheckDigit") {
    val testCases = Table(
      // Example from the Sierra docs
      ("1024364", "bibs", "b10243641"),
      // Examples taken from catalogue records
      ("1828888", "bibs", "b18288881"),
      ("1828888", "items", "i18288881"),
      // Example from a catalogue record with an "x" check digit
      ("1840974", "items", "i1840974x")
    )

    it("correctly handles all of our test cases") {
      forAll(testCases) {
        (sierraId: String, recordType: String, expectedId: String) =>
          val sierraRecordType = recordType match {
            case "bibs" => SierraRecordTypes.bibs
            case "items" => SierraRecordTypes.items
          }

          SierraRecordNumbers.addCheckDigit(sierraId, recordType = sierraRecordType) shouldBe expectedId
      }
    }

    it("throws an error if passed a Sierra ID which is non-numeric") {
      val caught = intercept[RuntimeException] {
        SierraRecordNumbers.addCheckDigit("abcdefg", recordType = SierraRecordTypes.bibs)
      }

      caught.getMessage shouldEqual "Expected 7-digit numeric ID, got abcdefg"
    }

    it("throws an error if passed a Sierra ID which is too short") {
      val caught = intercept[RuntimeException] {
        SierraRecordNumbers.addCheckDigit("123", recordType = SierraRecordTypes.bibs)
      }

      caught.getMessage shouldEqual "Expected 7-digit numeric ID, got 123"
    }

    it("throws an error if passed a Sierra ID which is too long") {
      val caught = intercept[RuntimeException] {
        SierraRecordNumbers.addCheckDigit("12345678", recordType = SierraRecordTypes.bibs)
      }

      caught.getMessage shouldEqual "Expected 7-digit numeric ID, got 12345678"
    }
  }

  describe("assertValidRecordNumber") {
    it("accepts a valid 7-digit record number") {
      SierraRecordNumbers.assertValidRecordNumber("1234567")
    }

    it("accepts a valid 8-digit record number") {
      SierraRecordNumbers.assertValidRecordNumber("12345678")
    }

    it("rejects an invalid string") {
      val caught = intercept[AssertionError] {
        SierraRecordNumbers.assertValidRecordNumber("NaN")
      }
      caught.getMessage shouldBe "assertion failed: Not a valid Sierra record number: NaN"
    }
  }

  describe("increment") {
    it("rejects 8-digit record numbers") {
      val caught = intercept[AssertionError] {
        SierraRecordNumbers.increment("12345678")
      }
      caught.getMessage shouldBe "assertion failed: increment() can only be used with 7-digit record numbers; not 12345678"
    }

    it("rejects bad inputs") {
      val caught = intercept[AssertionError] {
        SierraRecordNumbers.increment("NaN")
      }
      caught.getMessage shouldBe "assertion failed: increment() can only be used with 7-digit record numbers; not NaN"
    }

    it("increments a 7-digit record number") {
      SierraRecordNumbers.increment("1000001") shouldBe "1000002"
    }

    it("increments a 7-digit record number that wraps around") {
      SierraRecordNumbers.increment("1000009") shouldBe "1000010"
    }
  }
}
