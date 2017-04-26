package uk.ac.wellcome.models

import com.sksamuel.elastic4s._
import uk.ac.wellcome.utils.JsonUtil

/** Represents a set of identifiers as stored in DynamoDB */
case class Identifier(CanonicalID: String, MiroID: String)

/** An identifier received from one of the original sources */
case class SourceIdentifier(source: String, sourceId: String, value: String)

case class IdentifiedUnifiedItem(canonicalId: String, unifiedItem: UnifiedItem)

/** A representation of an item in our ontology, without a canonical identifier */
case class UnifiedItem(identifiers: List[SourceIdentifier],
                       label: String,
                       accessStatus: Option[String] = None)

object IdentifiedUnifiedItem extends Indexable[IdentifiedUnifiedItem] {
  override def json(t: IdentifiedUnifiedItem): String =
    JsonUtil.toJson(t).get
}
