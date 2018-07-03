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

  private def randomAlphanumeric(length: Int) =
    (Random.alphanumeric take length mkString) toLowerCase

  private def createCanonicalId = randomAlphanumeric(10)

  private def createSourceIdentifier = SourceIdentifier(
    identifierType = IdentifierType("miro-image-number"),
    value = randomAlphanumeric(10),
    ontologyType = "Work"
  )

  private def createTitle = randomAlphanumeric(100)

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
        workWith(
          canonicalId = s"${idx}-${canonicalId}",
          title = s"${idx}-${title}",
          description = s"${idx}-${description}",
          lettering = s"${idx}-${lettering}",
          createdDate = Period(s"${idx}-${period.label}"),
          creator = Agent(s"${idx}-${agent.label}"),
          items = createItems(count = 2)
      ))

  def createIdentifiedInvisibleWorks(count: Int): Seq[IdentifiedInvisibleWork] =
    (1 to count).map { _ => createIdentifiedInvisibleWork }

  def createIdentifiedInvisibleWork: IdentifiedInvisibleWork =
    IdentifiedInvisibleWork(
      sourceIdentifier = createSourceIdentifier,
      version = 1,
      canonicalId = createCanonicalId
    )

  def workWith(
    canonicalId: String,
    title: String,
    otherIdentifiers: List[SourceIdentifier] = List(),
    items: List[Identified[Item]] = List()
  ): IdentifiedWork =
    IdentifiedWork(
      title = title,
      sourceIdentifier = sourceIdentifier,
      version = 1,
      otherIdentifiers = otherIdentifiers,
      canonicalId = canonicalId,
      items = items)

  def identifiedWorkWith(
    canonicalId: String,
    title: String,
    thumbnail: Location
  ): IdentifiedWork =
    IdentifiedWork(
      title = title,
      sourceIdentifier = sourceIdentifier,
      version = 1,
      canonicalId = canonicalId,
      thumbnail = Some(thumbnail)
    )

  def workWith(canonicalId: String,
               title: String,
               description: String,
               lettering: String,
               createdDate: Period,
               creator: Agent,
               items: List[Identified[Item]]): IdentifiedWork =
    IdentifiedWork(
      title = title,
      sourceIdentifier = sourceIdentifier,
      version = 1,
      canonicalId = canonicalId,
      workType = Some(workType),
      description = Some(description),
      lettering = Some(lettering),
      createdDate = Some(createdDate),
      contributors = List(
        Contributor(agent = Unidentifiable(creator))
      ),
      production = List(),
      items = items
    )

  def createIdentifiedWorkWith(): IdentifiedWork =
    IdentifiedWork(
      canonicalId = createCanonicalId,
      sourceIdentifier = sourceIdentifier,
      title = createTitle,
      version = 1
    )

  def createIdentifiedWork: IdentifiedWork = createIdentifiedWorkWith()
}
