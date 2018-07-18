package uk.ac.wellcome.sierra_adapter.models

import java.time.Instant

import io.circe.optics.JsonPath.root
import io.circe.parser._
import uk.ac.wellcome.models.transformable.sierra.{
  SierraBibRecord,
  SierraItemRecord,
  SierraRecordNumber
}

import scala.util.Try

case class SierraRecord(id: SierraRecordNumber, data: String, modifiedDate: Instant) {
  def toBibRecord: SierraBibRecord =
    SierraBibRecord(
      id = this.id.s,
      data = this.data,
      modifiedDate = this.modifiedDate)

  def toItemRecord: Try[SierraItemRecord] =
    for {
      json <- parse(this.data).toTry
      bibIdsJsonSeq = root.bibIds.arr
        .getOption(json)
        .getOrElse(throw new IllegalArgumentException(
          "Json data did not contain bibIds"))
      bibIds = bibIdsJsonSeq.map { json =>
        json.asString.getOrElse(
          throw new IllegalArgumentException("Found non string in bibIds"))
      }.toList
    } yield {
      SierraItemRecord(
        id = this.id.s,
        data = this.data,
        modifiedDate = this.modifiedDate,
        bibIds = bibIds)
    }
}

case object SierraRecord {
  def apply(id: String, data: String, modifiedDate: String): SierraRecord =
    SierraRecord(
      id = id,
      data = data,
      modifiedDate = Instant.parse(modifiedDate)
    )

  def apply(id: String, data: String, modifiedDate: Instant): SierraRecord =
    SierraRecord(
      id = SierraRecordNumber(id),
      data = data,
      modifiedDate = modifiedDate
    )
}
