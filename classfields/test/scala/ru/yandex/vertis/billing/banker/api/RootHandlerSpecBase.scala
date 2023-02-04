package ru.yandex.vertis.billing.banker.api

import akka.http.scaladsl.server.Route
import org.scalatest.Suite
import ru.yandex.common.monitoring.ping.SignalSwitchingDecider
import ru.yandex.vertis.tracing.{EndpointConfig, LocalTracingSupport, TracingSupport}

/**
  * Base spec which initialize route
  *
  * @author ruslansd
  */
private[api] trait RootHandlerSpecBase extends HandlerSpecBase with MockedApiBackend {

  this: Suite =>

  implicit val tracingSupport: TracingSupport =
    LocalTracingSupport(EndpointConfig("unit-test", "localhost", 0))

  lazy val route: Route = {
    val decider = new SignalSwitchingDecider()
    val handler = new HandlerImpl(decider, registry)
    seal(handler.route)
  }

}
