package ru.yandex.vertis.billing.banker.api

import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.model.headers.{`Accept`, `Cache-Control`, CacheDirectives, RawHeader}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.RoutingSettings
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.codahale.metrics.MetricRegistry
import org.scalatest.Suite
import org.scalatest.matchers.should.Matchers
import ru.yandex.common.monitoring.HealthChecks
import ru.yandex.vertis.application.deploy.Deploys
import ru.yandex.vertis.application.environment.Environments
import ru.yandex.vertis.application.runtime.{RuntimeConfig, RuntimeConfigImpl}
import ru.yandex.vertis.billing.banker.api.directives.DomainDirectives
import ru.yandex.vertis.billing.banker.util.UserContext
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.ops.codahale.CodahaleRegistry
import ru.yandex.vertis.ops.prometheus.{CompositeCollector, PrometheusRegistry}
import ru.yandex.vertis.util.akka.http.protobuf.Protobuf

/**
  * Base spec with defaults
  *
  * @author ruslansd
  */
trait HandlerSpecBase extends Matchers with ScalatestRouteTest with DomainDirectives {

  this: Suite =>

  def basePath: String = ""

  def url(remainPath: String): String = s"$basePath$remainPath"

  val userContext = UserContext("test-request", "test_user")

  val defaultHeaders = addHeaders(
    RawHeader(VertisRequestIdHeader, userContext.id),
    RawHeader(VertisUserHeader, userContext.user),
    `Cache-Control`(CacheDirectives.`no-cache`)
  )

  val addAcceptJson = addHeader(`Accept`(MediaTypes.`application/json`))
  val addAcceptProtobuf = addHeader(`Accept`(Protobuf.mediaType))

  implicit val ops: OperationalSupport = new OperationalSupport {
    override val healthChecks = HealthChecks.compoundRegistry()

    implicit override val codahaleRegistry: CodahaleRegistry = new MetricRegistry

    implicit override val prometheusRegistry: PrometheusRegistry = new CompositeCollector
  }

  implicit val runtimeConfig: RuntimeConfig =
    RuntimeConfigImpl(Environments.Local, "localhost", "development", Deploys.Debian, None)

  def seal(route: Route): Route = Route.seal(route)(
    routingSettings = implicitly[RoutingSettings],
    exceptionHandler = DomainExceptionHandler.rootExceptionHandler
  )

}
