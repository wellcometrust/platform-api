package uk.ac.wellcome.platform.api.services

import com.sksamuel.elastic4s.Index
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal.{IdentifiedBaseWork, WorkType}
import uk.ac.wellcome.platform.api.fixtures.{
  ElasticsearchServiceFixture,
  WorksServiceFixture
}
import uk.ac.wellcome.platform.api.generators.SearchOptionsGenerators
import uk.ac.wellcome.platform.api.models.{ResultList, WorkTypeFilter}

import scala.concurrent.Future

class WorksServiceTest
    extends FunSpec
    with ElasticsearchServiceFixture
    with WorksServiceFixture
    with Matchers
    with ScalaFutures
    with SearchOptionsGenerators
    with WorksGenerators {

  describe("listWorks") {
    it("gets records in Elasticsearch") {
      val works = createIdentifiedWorks(count = 2)

      assertListResultIsCorrect(
        allWorks = works,
        expectedWorks = works,
        expectedTotalResults = 2
      )
    }

    it("returns 0 pages when no results are available") {
      assertListResultIsCorrect(
        allWorks = Seq(),
        expectedWorks = Seq(),
        expectedTotalResults = 0
      )
    }

    it("returns an empty result set when asked for a page that does not exist") {
      assertListResultIsCorrect(
        allWorks = createIdentifiedWorks(count = 3),
        expectedWorks = Seq(),
        expectedTotalResults = 3,
        worksSearchOptions = createWorksSearchOptionsWith(pageNumber = 4)
      )
    }

    it("filters records by workType") {
      val work1 = createIdentifiedWorkWith(
        workType = Some(WorkType(id = "b", label = "Books"))
      )
      val work2 = createIdentifiedWorkWith(
        workType = Some(WorkType(id = "b", label = "Books"))
      )
      val workWithWrongWorkType = createIdentifiedWorkWith(
        workType = Some(WorkType(id = "m", label = "Manuscripts"))
      )

      assertListResultIsCorrect(
        allWorks = Seq(work1, work2, workWithWrongWorkType),
        expectedWorks = Seq(work1, work2),
        expectedTotalResults = 2,
        worksSearchOptions = createWorksSearchOptionsWith(
          filters = List(WorkTypeFilter(workTypeId = "b"))
        )
      )
    }

    it("filters records by multiple workTypes") {
      val work1 = createIdentifiedWorkWith(
        workType = Some(WorkType(id = "b", label = "Books"))
      )
      val work2 = createIdentifiedWorkWith(
        workType = Some(WorkType(id = "b", label = "Books"))
      )
      val work3 = createIdentifiedWorkWith(
        workType = Some(WorkType(id = "a", label = "Archives"))
      )
      val workWithWrongWorkType = createIdentifiedWorkWith(
        workType = Some(WorkType(id = "m", label = "Manuscripts"))
      )

      assertListResultIsCorrect(
        allWorks = Seq(work1, work2, work3, workWithWrongWorkType),
        expectedWorks = Seq(work1, work2, work3),
        expectedTotalResults = 3,
        worksSearchOptions = createWorksSearchOptionsWith(
          filters = List(WorkTypeFilter(List("b", "a")))
        )
      )
    }
  }

  describe("findWorkById") {
    it("gets a DisplayWork by id") {
      withLocalWorksIndex { index: Index =>
        withElasticsearchService { searchService =>
          withWorksService(searchService) { worksService =>
            val work = createIdentifiedWork

            insertIntoElasticsearch(index, work)

            val indexAndType = createIndexAndType(index = index)

            val recordsFuture = worksService.findWorkById(
              canonicalId = work.canonicalId)(indexAndType)

            whenReady(recordsFuture) { records =>
              records.isDefined shouldBe true
              records.get shouldBe work
            }
          }
        }
      }
    }

    it("returns a future of None if it cannot get a record by id") {
      withLocalWorksIndex { index: Index =>
        withElasticsearchService { searchService =>
          withWorksService(searchService) { worksService =>
            val indexAndType = createIndexAndType(index = index)

            val recordsFuture =
              worksService.findWorkById(canonicalId = "1234")(indexAndType)

            whenReady(recordsFuture) { record =>
              record shouldBe None
            }
          }
        }
      }
    }
  }

  describe("searchWorks") {
    it("only finds results that match a query if doing a full-text search") {
      val workDodo = createIdentifiedWorkWith(
        title = "A drawing of a dodo"
      )
      val workMouse = createIdentifiedWorkWith(
        title = "A mezzotint of a mouse"
      )

      assertSearchResultIsCorrect(
        query = "cat"
      )(
        allWorks = List(workDodo, workMouse),
        expectedWorks = List(),
        expectedTotalResults = 0
      )

      assertSearchResultIsCorrect(
        query = "dodo"
      )(
        allWorks = List(workDodo, workMouse),
        expectedWorks = List(workDodo),
        expectedTotalResults = 1
      )
    }

    it("doesn't throw an exception if passed an invalid query string") {
      val workEmu = createIdentifiedWorkWith(
        title = "An etching of an emu"
      )

      assertSearchResultIsCorrect(
        query =
          "emu \"unmatched quotes are a lexical error in the Elasticsearch parser"
      )(
        allWorks = List(workEmu),
        expectedWorks = List(workEmu),
        expectedTotalResults = 1
      )
    }

    it("filters searches by workType") {
      val matchingWork = createIdentifiedWorkWith(
        title = "Animated artichokes",
        workType = Some(WorkType(id = "b", label = "Books"))
      )
      val workWithWrongTitle = createIdentifiedWorkWith(
        title = "Bouncing bananas",
        workType = Some(WorkType(id = "b", label = "Books"))
      )
      val workWithWrongWorkType = createIdentifiedWorkWith(
        title = "Animated artichokes",
        workType = Some(WorkType(id = "m", label = "Manuscripts"))
      )

      assertSearchResultIsCorrect(
        query = "artichokes"
      )(
        allWorks = List(matchingWork, workWithWrongTitle, workWithWrongWorkType),
        expectedWorks = List(matchingWork),
        expectedTotalResults = 1,
        worksSearchOptions = createWorksSearchOptionsWith(
          filters = List(WorkTypeFilter(workTypeId = "b"))
        )
      )
    }

    it("filters searches by multiple workTypes") {
      val work1 = createIdentifiedWorkWith(
        title = "Animated artichokes",
        workType = Some(WorkType(id = "b", label = "Books"))
      )
      val workWithWrongTitle = createIdentifiedWorkWith(
        title = "Bouncing bananas",
        workType = Some(WorkType(id = "b", label = "Books"))
      )
      val work2 = createIdentifiedWorkWith(
        title = "Animated artichokes",
        workType = Some(WorkType(id = "m", label = "Manuscripts"))
      )
      val workWithWrongWorkType = createIdentifiedWorkWith(
        title = "Animated artichokes",
        workType = Some(WorkType(id = "a", label = "Archives"))
      )

      assertSearchResultIsCorrect(
        query = "artichokes"
      )(
        allWorks = List(work1, workWithWrongTitle, work2, workWithWrongWorkType),
        expectedWorks = List(work1, work2),
        expectedTotalResults = 2,
        worksSearchOptions = createWorksSearchOptionsWith(
          filters = List(WorkTypeFilter(List("b", "m")))
        )
      )
    }
  }

  private def assertListResultIsCorrect(
    allWorks: Seq[IdentifiedBaseWork],
    expectedWorks: Seq[IdentifiedBaseWork],
    expectedTotalResults: Int,
    worksSearchOptions: WorksSearchOptions = createWorksSearchOptions
  ) =
    assertResultIsCorrect(
      (worksService: WorksService) => worksService.listWorks
    )(allWorks, expectedWorks, expectedTotalResults, worksSearchOptions)

  private def assertSearchResultIsCorrect(query: String)(
    allWorks: Seq[IdentifiedBaseWork],
    expectedWorks: Seq[IdentifiedBaseWork],
    expectedTotalResults: Int,
    worksSearchOptions: WorksSearchOptions = createWorksSearchOptions
  ) =
    assertResultIsCorrect(
      (worksService: WorksService) => worksService.searchWorks(query = query)
    )(allWorks, expectedWorks, expectedTotalResults, worksSearchOptions)

  private def assertResultIsCorrect(
    partialSearchFunction: WorksService => (
      Index,
      WorksSearchOptions) => Future[ResultList]
  )(
    allWorks: Seq[IdentifiedBaseWork],
    expectedWorks: Seq[IdentifiedBaseWork],
    expectedTotalResults: Int,
    worksSearchOptions: WorksSearchOptions
  ): Assertion =
    withLocalWorksIndex { index: Index =>
      withElasticsearchService { searchService =>
        withWorksService(searchService) { worksService =>
          if (allWorks.nonEmpty) {
            insertIntoElasticsearch(index, allWorks: _*)
          }

          val future = partialSearchFunction(worksService)(
            index,
            worksSearchOptions)

          whenReady(future) { works =>
            works.results should contain theSameElementsAs expectedWorks
            works.totalResults shouldBe expectedTotalResults
          }
        }
      }
    }
}
