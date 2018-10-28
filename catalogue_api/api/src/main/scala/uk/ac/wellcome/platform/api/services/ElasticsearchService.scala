package uk.ac.wellcome.platform.api.services

import com.google.inject.{Inject, Singleton}
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.get.GetResponse
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.searches.SearchDefinition
import com.sksamuel.elastic4s.searches.queries.BoolQueryDefinition
import com.sksamuel.elastic4s.searches.queries.term.TermQueryDefinition
import com.sksamuel.elastic4s.searches.sort.FieldSortDefinition

import scala.concurrent.Future

case class ElasticsearchDocumentOptions(
  indexName: String,
  documentType: String
)

case class ElasticsearchQueryOptions(
  workTypeFilter: Option[String],
  limit: Int,
  from: Int
)

@Singleton
class ElasticsearchService @Inject()(elasticClient: HttpClient) {

  def findResultById(canonicalId: String)(documentOptions: ElasticsearchDocumentOptions): Future[GetResponse] =
    elasticClient
      .execute {
        get(canonicalId).from(s"${documentOptions.indexName}/${documentOptions.documentType}")
      }

  def listResults(sortByField: String): (ElasticsearchDocumentOptions, ElasticsearchQueryOptions) => Future[SearchResponse] =
    executeSearch(
      maybeQueryString = None,
      sortByField = Some(sortByField)
    )

  def simpleStringQueryResults(queryString: String): (ElasticsearchDocumentOptions, ElasticsearchQueryOptions) => Future[SearchResponse] =
    executeSearch(
      maybeQueryString = Some(queryString),
      sortByField = None
    )

  /** Given a set of query options, build a SearchDefinition for Elasticsearch
    * using the elastic4s query DSL, then execute the search.
    */
  private def executeSearch(
    maybeQueryString: Option[String],
    sortByField: Option[String]
  )(documentOptions: ElasticsearchDocumentOptions, queryOptions: ElasticsearchQueryOptions): Future[SearchResponse] = {
    val queryDefinition = buildQuery(
      maybeQueryString = maybeQueryString,
      workTypeFilter = queryOptions.workTypeFilter
    )

    val sortDefinitions: List[FieldSortDefinition] =
      sortByField match {
        case Some(fieldName) => List(fieldSort(fieldName))
        case None            => List()
      }

    val searchDefinition: SearchDefinition =
      search(s"${documentOptions.indexName}/${documentOptions.documentType}")
        .query(queryDefinition)
        .sortBy(sortDefinitions)
        .limit(queryOptions.limit)
        .from(queryOptions.from)

    elasticClient
      .execute { searchDefinition }
  }

  private def buildQuery(
    maybeQueryString: Option[String],
    workTypeFilter: Option[String]): BoolQueryDefinition = {
    val queries = List(
      maybeQueryString.map { simpleStringQuery }
    ).flatten

    val filters: List[TermQueryDefinition] = List(
      workTypeFilter.map { termQuery("workType.id", _) }
    ).flatten :+ termQuery("type", "IdentifiedWork")

    must(queries).filter(filters)
  }
}
