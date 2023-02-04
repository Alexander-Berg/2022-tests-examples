package bootstrap.test

import bootstrap.logging.{BLogger, LevelConfigService}
import bootstrap.metrics.Registry
import bootstrap.otel.OpenTelemetry
import bootstrap.tracing.*
import zio.{Clock, ZLayer}

// TODO сделать наследника от ZIOSpecDefault, в котором будет подниматься нужный контекст
object TestBase {

  lazy val bootstrap: ZLayer[Any, Nothing, $ & Registry & Clock] = ZLayer
    .make[$ & Registry & Clock](
      Clock.live,
      LevelConfigService.fromYamlResource("blogger.yml"),
      BLogger.live,
      OpenTelemetry.empty,
      Registry.live,
      Span.live,
    )

}
