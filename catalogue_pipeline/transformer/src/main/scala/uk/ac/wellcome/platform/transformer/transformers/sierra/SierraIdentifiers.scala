package uk.ac.wellcome.platform.transformer.transformers.sierra

import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.work.internal.{IdentifierType, SourceIdentifier}

trait SierraIdentifiers {

  // Populate wwork:identifiers.
  //
  //    We populate with two identifier schemes:
  //
  //    -   "sierra-system-number" for the full record type (incl. prefix and
  //        check digit)
  //    -   "sierra-identifier" for the 7-digit internal ID
  //
  //    Adding other identifiers is out-of-scope for now.
  //
  def getOtherIdentifiers(sierraId: String): List[SourceIdentifier] =
    List(
      SourceIdentifier(
        identifierType = IdentifierType("sierra-identifier"),
        ontologyType = "Work",
        value = sierraId
      )
    )
}
