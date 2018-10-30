package uk.ac.wellcome.platform.api.fixtures

import org.scalatest.Suite
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.platform.api.services.ElasticsearchService
import uk.ac.wellcome.test.fixtures.TestWith

trait ElasticsearchServiceFixture extends ElasticsearchFixtures {
  this: Suite =>
  def withElasticsearchService[R](
    testWith: TestWith[ElasticsearchService, R]): R = {
    val searchService = new ElasticsearchService(elasticClient = elasticClient)
    testWith(searchService)
  }
}
