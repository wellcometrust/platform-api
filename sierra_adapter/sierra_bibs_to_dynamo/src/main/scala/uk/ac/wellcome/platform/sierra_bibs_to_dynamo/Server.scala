package uk.ac.wellcome.platform.sierra_bibs_to_dynamo

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{
  CommonFilters,
  LoggingMDCFilter,
  TraceIdMDCFilter
}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.finatra.modules._
import uk.ac.wellcome.platform.sierra_bibs_to_dynamo.controllers.ManagementController
import uk.ac.wellcome.platform.sierra_bibs_to_dynamo.modules._

object ServerMain extends Server

class Server extends HttpServer {
  override val name =
    "uk.ac.wellcome.platform.sierra_bibs_to_dynamo SierraToDynamo"
  override val modules = Seq(
    PlatformDynamoConfigModule,
    DynamoClientModule,
    SierraDynamoConfigModule,
    SierraToDynamoModule,
    AmazonCloudWatchModule,
    SQSConfigModule,
    SQSClientModule,
    AkkaModule
  )

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
  }
}
