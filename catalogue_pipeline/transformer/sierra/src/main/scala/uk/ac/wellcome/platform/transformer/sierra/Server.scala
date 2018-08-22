package uk.ac.wellcome.platform.transformer.sierra

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.finatra.akka.{AkkaModule, ExecutionContextModule}
import uk.ac.wellcome.finatra.controllers.ManagementController
import uk.ac.wellcome.finatra.messaging.{MessageWriterConfigModule, SNSClientModule, SQSClientModule, SQSConfigModule}
import uk.ac.wellcome.finatra.monitoring.MetricsSenderModule
import uk.ac.wellcome.finatra.storage.{S3ClientModule, S3ConfigModule}
import uk.ac.wellcome.platform.transformer.modules.TransformedBaseWorkModule
import uk.ac.wellcome.platform.transformer.sierra.modules.{SierraTransformableModule, SierraTransformerWorkerModule}

object ServerMain extends Server

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.transformer Transformer"
  override val modules = Seq(
    MessageWriterConfigModule,
    MetricsSenderModule,
    AkkaModule,
    SQSClientModule,
    SQSConfigModule,
    SNSClientModule,
    SierraTransformerWorkerModule,
    ExecutionContextModule,
    SierraTransformableModule,
    S3ClientModule,
    S3ConfigModule,
    TransformedBaseWorkModule
  )
  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
  }
}
