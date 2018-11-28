package uk.ac.wellcome.platform.archive.registrar.http.models
import java.net.URL

import io.circe.generic.extras.JsonKey
import uk.ac.wellcome.platform.archive.display.{
  DisplayLocation,
  DisplaySpace
}
import uk.ac.wellcome.platform.archive.registrar.common.models._

case class DisplayBag(
  @JsonKey("@context")
  context: String,
  id: String,
  space: DisplaySpace,
  info: DisplayBagInfo,
  manifest: DisplayBagManifest,
  tagManifest: DisplayBagManifest,
  accessLocation: DisplayLocation,
  createdDate: String,
  @JsonKey("type")
  ontologyType: String = "Bag"
)

object DisplayBag {
  def apply(storageManifest: StorageManifest, contextUrl: URL): DisplayBag =
    DisplayBag(
      contextUrl.toString,
      storageManifest.id.toString,
      DisplaySpace(storageManifest.space.underlying),
      DisplayBagInfo(storageManifest.info),
      DisplayBagManifest(storageManifest.manifest),
      DisplayBagManifest(storageManifest.tagManifest),
      DisplayLocation(storageManifest.accessLocation),
      storageManifest.createdDate.toString
    )
}
