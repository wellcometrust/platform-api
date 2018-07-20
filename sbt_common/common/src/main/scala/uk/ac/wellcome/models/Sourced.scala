package uk.ac.wellcome.models

trait Sourced {
  def sourceId: String
  val sourceName: String
  val id: String = Sourced.id(sourceName, sourceId)
}

object Sourced {
  def id(sourceName: String, sourceId: String) = s"$sourceName/$sourceId"
}
