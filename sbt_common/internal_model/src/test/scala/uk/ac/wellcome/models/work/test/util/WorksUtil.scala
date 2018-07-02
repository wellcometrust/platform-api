package uk.ac.wellcome.models.work.test.util

import uk.ac.wellcome.models.work.internal._

import scala.util.Random

trait WorksUtil extends ItemsUtil {
  val canonicalId = "1234"
  val title = "this is the first image title"
  val description = "this is a description"
  val lettering = "some lettering"
  val period = Period("the past")
  val agent = Agent("a person")
  val workType = WorkType(
    id = "1dz4yn34va",
    label = "An aggregation of angry archipelago aged ankylosaurs."
  )
  val subject = Subject[Unidentifiable[AbstractConcept]](
    label = "a subject created by WorksUtil",
    concepts = List(
      Unidentifiable(Concept("a subject concept")),
      Unidentifiable(Place("a subject place")),
      Unidentifiable(Period("a subject period")))
  )

  val genre = Genre[Unidentifiable[AbstractConcept]](
    label = "an unidentified genre created by WorksUtil",
    concepts = List(
      Unidentifiable(Concept("a genre concept")),
      Unidentifiable(Place("a genre place")),
      Unidentifiable(Period("a genre period")))
  )

  val sourceIdentifier = SourceIdentifier(
    identifierType = IdentifierType("miro-image-number"),
    "Work",
    "sourceIdentifierFromWorksUtil"
  )

  def createWork: IdentifiedWork =
    createWorks(count = 1).head

  def createWorks(count: Int, start: Int = 1): Seq[IdentifiedWork] =
    (start to count).map(
      (idx: Int) =>
        identifiedWorkWith(
          canonicalId = s"${idx}-${canonicalId}",
          title = s"${idx}-${title}",
<<<<<<< HEAD
          description = s"${idx}-${description}",
          lettering = s"${idx}-${lettering}",
          createdDate = Period(s"${idx}-${period.label}"),
          creator = Agent(s"${idx}-${agent.label}"),
          items = createItems(count = 2)
=======
          description = Some(s"${idx}-${description}"),
          lettering = Some(s"${idx}-${lettering}"),
          createdDate = Some(Period(s"${idx}-${period.label}")),
          contributors = List(Contributor(agent = Unidentifiable(Agent(s"${idx}-${agent.label}")))),
          items = List(defaultItem)
>>>>>>> Another method cleaned up
      ))

  def createInvisibleWorks(count: Int,
                           start: Int = 1): Seq[IdentifiedInvisibleWork] =
    (start to count).map(
      (idx: Int) => invisibleWorkWith(s"$idx-$canonicalId")
    )

  def unidentifiedWorkWithDefaults(
    sourceIdentifier: SourceIdentifier = sourceIdentifier,
    version: Int = 1,
    title: String = title,
    contributors: List[Contributor[MaybeDisplayable[AbstractAgent]]] = List(),
    items: List[Identifiable[Item]] = List()
  ): UnidentifiedWork =
    UnidentifiedWork(
      sourceIdentifier = sourceIdentifier,
      version = version,
      title = title,
      contributors = contributors,
      items = items
    )

  def identifiedWorkWith(
    canonicalId: String = (Random.alphanumeric take 10 mkString) toLowerCase,
    sourceIdentifier: SourceIdentifier = sourceIdentifier,
    otherIdentifiers: List[SourceIdentifier] = List(),
    version: Int = 1,
    title: String = title,
    workType: Option[WorkType] = None,
    description: Option[String] = None,
    physicalDescription: Option[String] = None,
    extent: Option[String] = None,
    lettering: Option[String] = None,
    createdDate: Option[Period] = None,
    subjects: List[Subject[Displayable[AbstractConcept]]] = List(),
    genres: List[Genre[Displayable[AbstractConcept]]] = List(),
    contributors: List[Contributor[Displayable[AbstractAgent]]] = List(),
    thumbnail: Option[Location] = None,
    production: List[ProductionEvent[Displayable[AbstractAgent]]] = List(),
    language: Option[Language] = None,
    items: List[Identified[Item]] = List()
  ): IdentifiedWork =
    IdentifiedWork(
      canonicalId = canonicalId,
      sourceIdentifier = sourceIdentifier,
      otherIdentifiers = otherIdentifiers,
      mergeCandidates = List(),
      version = version,
      title = title,
      workType = workType,
      description = description,
      physicalDescription = physicalDescription,
      lettering = lettering,
      extent = extent,
      createdDate = createdDate,
      subjects = subjects,
      genres = genres,
      contributors = contributors,
      thumbnail = thumbnail,
      production = production,
      language = language,
      dimensions = None,
      items = items
    )

  def invisibleWorkWith(canonicalId: String = (Random.alphanumeric take 10 mkString) toLowerCase): IdentifiedInvisibleWork = {
    IdentifiedInvisibleWork(
      sourceIdentifier = sourceIdentifier,
      version = 1,
      canonicalId = canonicalId
    )
  }
}
