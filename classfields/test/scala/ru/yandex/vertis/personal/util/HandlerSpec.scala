package ru.yandex.vertis.personal.util

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import com.codahale.metrics.MetricRegistry
import ru.yandex.common.monitoring.HealthChecks
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.ops.prometheus.{OperationalAwareRegistry, PrometheusRegistry, SimpleCompositeCollector}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import ru.yandex.vertis.personal.util.instrumented.PersonalRequestContextWrapper

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 29.06.16
  */
trait HandlerSpec
  extends BaseSpec
  with ScalatestRouteTest
  with PersonalRequestContextWrapper
  with HttpExceptionHandler {

  implicit def actorRefFactory: ActorSystem = system

  val operationalSupport: OperationalSupport = new OperationalSupport {

    implicit override val prometheusRegistry: PrometheusRegistry = {
      new OperationalAwareRegistry(
        new SimpleCompositeCollector,
        "test",
        "test",
        "test",
        ""
      )
    }

    implicit override val codahaleRegistry: MetricRegistry = new MetricRegistry

    override val healthChecks = HealthChecks.compoundRegistry()

  }

  def dummyRoute(response: String): Route =
    ctx => complete(s"${ctx.unmatchedPath} $response")(ctx)

  def sealRoute(route: Route): Route = {
    wrapRequest {
      Route.seal(route)(exceptionHandler = rootExceptionHandler)
    }
  }

}
