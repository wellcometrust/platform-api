package uk.ac.wellcome.transformer.source

case class SierraBibData(
  id: String,
  title: Option[String],
  lettering: Option[String] = None,
  deleted: Boolean = false,
  suppressed: Boolean = false,
  country: Option[Country] = None,
  fixedFields: Map[String, FixedField] = Map(),
  varFields: List[VarField] = List()
)
