package ru.yandex.vertis.billing.api

import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.model.headers.`Accept`
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.RoutingSettings
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.codahale.metrics.MetricRegistry
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.Suite
import org.scalatest.matchers.should.Matchers
import ru.yandex.common.monitoring.HealthChecks
import ru.yandex.vertis.application.deploy.Deploys
import ru.yandex.vertis.application.environment.Environments
import ru.yandex.vertis.application.runtime.{RuntimeConfig, RuntimeConfigImpl}
import ru.yandex.vertis.billing.model_core.Uid
import ru.yandex.vertis.billing.util.OperatorContext
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.ops.codahale.CodahaleRegistry
import ru.yandex.vertis.ops.prometheus.{CompositeCollector, PrometheusRegistry}

/**
  * Base spec with defaults
  *
  * @author ruslansd
  */
trait HandlerSpecBase extends Matchers with ScalatestRouteTest with AkkaDomainDirectives {

  this: Suite =>

  def basePath: String = ""

  def url(remainPath: String): String = s"$basePath$remainPath"
  val uid = Uid(111111)
  val operator = OperatorContext("test", uid)

  val defaultHeaders = addHeaders(`X-Yandex-Operator-Uid`(uid), `X-Yandex-Request-ID`(operator.id))
  val addAcceptJson = addHeader(`Accept`(MediaTypes.`application/json`))

  val ops: OperationalSupport = new OperationalSupport {
    override val healthChecks = HealthChecks.compoundRegistry()

    implicit override val codahaleRegistry: CodahaleRegistry = new MetricRegistry

    implicit override val prometheusRegistry: PrometheusRegistry = new CompositeCollector
  }

  implicit val runtimeConfig: RuntimeConfig =
    RuntimeConfigImpl(Environments.Local, "localhost", "development", Deploys.Debian, None)

  def seal(route: Route): Route = Route.seal(route)(
    routingSettings = implicitly[RoutingSettings],
    exceptionHandler = AkkaDomainExceptionHandler.rootExceptionHandler
  )

  override def testConfig: Config = ConfigFactory.empty()

}
