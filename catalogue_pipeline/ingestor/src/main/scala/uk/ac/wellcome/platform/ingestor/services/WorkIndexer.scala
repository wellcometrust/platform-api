package uk.ac.wellcome.platform.ingestor.services

import com.sksamuel.elastic4s.Indexable
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.bulk.BulkResponse
import com.twitter.inject.Logging
import javax.inject.{Inject, Singleton}
import org.elasticsearch.index.VersionType
import uk.ac.wellcome.elasticsearch.ElasticsearchExceptionManager
import uk.ac.wellcome.models.work.internal.IdentifiedWork
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext

@Singleton
class WorkIndexer @Inject()(
  elasticClient: HttpClient
)(implicit ec: ExecutionContext)
    extends Logging
    with ElasticsearchExceptionManager {

  implicit object IdentifiedWorkIndexable extends Indexable[IdentifiedWork] {
    override def json(t: IdentifiedWork): String =
      toJson(t).get
  }

  def indexWorks(works: Seq[IdentifiedWork],
                esIndex: String,
                esType: String): Future[Seq[IdentifiedWork]] = {

    debug(s"Indexing work ${works.map(_.canonicalId).mkString(", ")}")

        val inserts = works.map { work =>
          indexInto(esIndex / esType)
            .version(work.version)
            .versionType(VersionType.EXTERNAL_GTE)
            .id(work.canonicalId)
            .doc(work)
        }

        elasticClient
          .execute {
            bulk(inserts)
          }
          .map { bulkResponse: BulkResponse =>
            val successfulIds = bulkResponse.successes.map(_.id)
            debug(s"Successfully indexed works $successfulIds")
            works.filter(w => {
              successfulIds.contains(w.canonicalId)
            })
          }
      }
}
