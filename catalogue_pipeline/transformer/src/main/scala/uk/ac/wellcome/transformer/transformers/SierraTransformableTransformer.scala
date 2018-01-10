package uk.ac.wellcome.transformer.transformers

import com.twitter.inject.Logging
import uk.ac.wellcome.models._
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.transformer.source.{SierraBibData, SierraItemData}
import uk.ac.wellcome.utils.JsonUtil

import scala.util.{Failure, Success, Try}

class SierraTransformableTransformer
    extends TransformableTransformer[SierraTransformable]
    with Logging {

  private def extractItemData(itemRecord: SierraItemRecord) = {
    info(s"Attempting to transform ${itemRecord}")

    JsonUtil
      .fromJson[SierraItemData](itemRecord.data) match {
      case Success(sierraItemData) =>
        Some(
          Item(
            sourceIdentifier = SourceIdentifier(
              IdentifierSchemes.sierraSystemNumber,
              sierraItemData.id
            ),
            identifiers = List(
              SourceIdentifier(
                identifierScheme = IdentifierSchemes.sierraSystemNumber,
                sierraItemData.id
              )
            ),
            visible = !sierraItemData.deleted
          ))
      case Failure(e) => {
        error(s"Failed to parse item!", e)

        None
      }
    }
  }

  override def transformForType(
    sierraTransformable: SierraTransformable): Try[Option[Work]] = {
    sierraTransformable.maybeBibData
      .map { bibData =>
        info(s"Attempting to transform $bibData")

        JsonUtil.fromJson[SierraBibData](bibData.data).map { sierraBibData =>
          Some(Work(
            title = sierraBibData.title,
            sourceIdentifier = SourceIdentifier(
              identifierScheme = IdentifierSchemes.sierraSystemNumber,
              sierraBibData.id
            ),
            identifiers = List(
              SourceIdentifier(
                identifierScheme = IdentifierSchemes.sierraSystemNumber,
                sierraBibData.id
              )
            ),
            items = Option(sierraTransformable.itemData)
              .getOrElse(Map.empty)
              .values
              .flatMap(extractItemData)
              .toList,
            visible = !(sierraBibData.deleted || sierraBibData.suppressed)
          ))
        }

      }
      .getOrElse(Success(None))
  }
}
