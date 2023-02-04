package bootstrap.test

import bootstrap.logging.{BLogger, LevelConfigService}
import bootstrap.metrics.Registry
import bootstrap.otel.OpenTelemetry
import bootstrap.tracing.*
import zio.*
import zio.test.*

abstract class BootstrapSpec[R : EnvironmentTag]
    extends ZIOSpec[BootstrapSpec.BaseEnv & R] {
  type BaseEnv = BootstrapSpec.BaseEnv

  final override lazy val bootstrap
    : ZLayer[ZIOAppArgs & Scope, Nothing, Environment] = ZLayer
    .makeSome[Scope, Environment](
      Live.default,
      Console.live,
      System.live,
      TestConfig.default,
      Random.live,
      Annotations.live,
      Clock.live,
      LevelConfigService.fromYamlResource("blogger.yml"),
      BLogger.live,
      OpenTelemetry.empty,
      Registry.live,
      Span.live,
      RLive,
    )

  def RLive: ZLayer[BaseEnv, Nothing, R]

}

object BootstrapSpec {

  type BaseEnv =
    Scope &
      Annotations &
      Live &
      TestConfig &
      Clock &
      Registry &
      OpenTelemetry &
      $

}
