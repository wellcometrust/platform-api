package uk.ac.wellcome.platform.api.works.v1

import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import uk.ac.wellcome.models.work.internal._

class ApiV1FiltersTest extends ApiV1WorksTestBase {

  describe("listing works") {
    it("ignores works with no workType") {
      withV1Api {
        case (
            apiPrefix,
            indexNameV1,
            _,
            itemType,
            server: EmbeddedHttpServer) =>
          val noWorkTypeWorks = (1 to 3).map { _ =>
            createIdentifiedWorkWith(workType = None)
          }
          val matchingWork = createIdentifiedWorkWith(
            workType = Some(WorkType(id = "b", label = "Books")))

          val works = noWorkTypeWorks :+ matchingWork
          insertIntoElasticsearch(indexNameV1, itemType, works: _*)

          eventually {
            server.httpGet(
              path = s"/$apiPrefix/works?workType=b",
              andExpect = Status.Ok,
              withJsonBody = s"""
                   |{
                   |  ${resultList(apiPrefix, totalResults = 1)},
                   |  "results": [
                   |    {
                   |      "type": "Work",
                   |      "id": "${matchingWork.canonicalId}",
                   |      "title": "${matchingWork.title}",
                   |      "workType": ${workType(matchingWork.workType.get)},
                   |      "creators": [ ],
                   |      "subjects": [ ],
                   |      "genres": [ ],
                   |      "publishers": [ ],
                   |      "placesOfPublication": [ ]
                   |    }
                   |  ]
                   |}
          """.stripMargin
            )
          }
      }
    }

    it("filters out works with a different workType") {
      withV1Api {
        case (
            apiPrefix,
            indexNameV1,
            _,
            itemType,
            server: EmbeddedHttpServer) =>
          val wrongWorkTypeWorks = (1 to 3).map { _ =>
            createIdentifiedWorkWith(
              workType = Some(WorkType(id = "m", label = "Manuscripts")))
          }
          val matchingWork = createIdentifiedWorkWith(
            workType = Some(WorkType(id = "b", label = "Books")))

          val works = wrongWorkTypeWorks :+ matchingWork
          insertIntoElasticsearch(indexNameV1, itemType, works: _*)

          eventually {
            server.httpGet(
              path = s"/$apiPrefix/works?workType=b",
              andExpect = Status.Ok,
              withJsonBody = s"""
                   |{
                   |  ${resultList(apiPrefix, totalResults = 1)},
                   |  "results": [
                   |    {
                   |      "type": "Work",
                   |      "id": "${matchingWork.canonicalId}",
                   |      "title": "${matchingWork.title}",
                   |      "workType": ${workType(matchingWork.workType.get)},
                   |      "creators": [ ],
                   |      "subjects": [ ],
                   |      "genres": [ ],
                   |      "publishers": [ ],
                   |      "placesOfPublication": [ ]
                   |    }
                   |  ]
                   |}
          """.stripMargin
            )
          }
      }
    }
  }

  describe("searching works") {
    it("ignores works with no workType") {
      withV1Api {
        case (
            apiPrefix,
            indexNameV1,
            _,
            itemType,
            server: EmbeddedHttpServer) =>
          val noWorkTypeWorks = (1 to 3).map { _ =>
            createIdentifiedWorkWith(
              title = "Amazing aubergines",
              workType = None)
          }
          val matchingWork = createIdentifiedWorkWith(
            title = "Amazing aubergines",
            workType = Some(WorkType(id = "b", label = "Books")))

          val works = noWorkTypeWorks :+ matchingWork
          insertIntoElasticsearch(indexNameV1, itemType, works: _*)

          eventually {
            server.httpGet(
              path = s"/$apiPrefix/works?query=aubergines&workType=b",
              andExpect = Status.Ok,
              withJsonBody = s"""
                   |{
                   |  ${resultList(apiPrefix, totalResults = 1)},
                   |  "results": [
                   |    {
                   |      "type": "Work",
                   |      "id": "${matchingWork.canonicalId}",
                   |      "title": "${matchingWork.title}",
                   |      "workType": ${workType(matchingWork.workType.get)},
                   |      "creators": [ ],
                   |      "subjects": [ ],
                   |      "genres": [ ],
                   |      "publishers": [ ],
                   |      "placesOfPublication": [ ]
                   |    }
                   |  ]
                   |}
          """.stripMargin
            )
          }
      }
    }

    it("filters out works with a different workType") {
      withV1Api {
        case (
            apiPrefix,
            indexNameV1,
            _,
            itemType,
            server: EmbeddedHttpServer) =>
          val wrongWorkTypeWorks = (1 to 3).map { _ =>
            createIdentifiedWorkWith(
              title = "Bouncing bananas",
              workType = Some(WorkType(id = "m", label = "Manuscripts")))
          }
          val matchingWork = createIdentifiedWorkWith(
            title = "Bouncing bananas",
            workType = Some(WorkType(id = "b", label = "Books")))

          val works = wrongWorkTypeWorks :+ matchingWork
          insertIntoElasticsearch(indexNameV1, itemType, works: _*)

          eventually {
            server.httpGet(
              path = s"/$apiPrefix/works?query=bananas&workType=b",
              andExpect = Status.Ok,
              withJsonBody = s"""
                   |{
                   |  ${resultList(apiPrefix, totalResults = 1)},
                   |  "results": [
                   |    {
                   |      "type": "Work",
                   |      "id": "${matchingWork.canonicalId}",
                   |      "title": "${matchingWork.title}",
                   |      "workType": ${workType(matchingWork.workType.get)},
                   |      "creators": [ ],
                   |      "subjects": [ ],
                   |      "genres": [ ],
                   |      "publishers": [ ],
                   |      "placesOfPublication": [ ]
                   |    }
                   |  ]
                   |}
          """.stripMargin
            )
          }
      }
    }
  }
}
