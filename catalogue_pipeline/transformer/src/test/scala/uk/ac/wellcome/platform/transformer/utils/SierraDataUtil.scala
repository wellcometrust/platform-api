package uk.ac.wellcome.platform.transformer.utils

import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.models.work.test.util.IdentifiersUtil
import uk.ac.wellcome.platform.transformer.source._
import uk.ac.wellcome.platform.transformer.source.sierra.{Language => SierraLanguage}
import uk.ac.wellcome.utils.JsonUtil._

trait SierraDataUtil extends IdentifiersUtil with SierraUtil {
  def createSierraBibDataWith(
    title: Option[String] = Some(randomAlphanumeric(25)),
    lang: Option[SierraLanguage] = None,
    materialType: Option[SierraMaterialType] = None,
    varFields: List[VarField] = List()
  ): SierraBibData =
    SierraBibData(
      title = title,
      lang = lang,
      materialType = materialType,
      varFields = varFields
    )

  def createSierraBibData: SierraBibData = createSierraBibDataWith()

  def createSierraItemDataWith(
    deleted: Boolean = false,
    location: Option[SierraItemLocation] = None
  ): SierraItemData =
    SierraItemData(
      deleted = deleted,
      location = location
    )

  def createSierraItemData: SierraItemData = createSierraItemDataWith()

  def createSierraItemRecordWith(data: SierraItemData): SierraItemRecord =
    createSierraItemRecordWith(
      data = toJson(data).get
    )

  def createSierraMaterialTypeWith(code: String = randomAlphanumeric(1),
                                   value: String = randomAlphanumeric(5)) = {
    SierraMaterialType(code, value)
  }

  def createSierraMaterialType: SierraMaterialType =
    createSierraMaterialTypeWith()

  def createSierraEbookMaterialType: SierraMaterialType =
    createSierraMaterialTypeWith(code = "v", value = "E-books")
}
