package uk.ac.wellcome.platform.goobi_reader.modules

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.{Injector, TwitterModule}
import uk.ac.wellcome.platform.goobi_reader.GlobalExecutionContext

import scala.concurrent.ExecutionContext

object ExecutionContextModule extends TwitterModule {
  @Provides
  @Singleton
  def provideExecutionContext(injector: Injector): ExecutionContext =
    GlobalExecutionContext.context
}
