package ru.yandex.vertis.billing.internal_api

import org.scalatest.Suite
import ru.yandex.vertis.billing.internal_api.routes.HandlerImpl
import ru.yandex.vertis.tracing.{EndpointConfig, LocalTracingSupport, TracingSupport}

/**
  * Base spec which initialize route
  *
  * @author ruslansd
  */
private[internal_api] trait RootHandlerSpecBase extends HandlerSpecBase with MockedInternalApiBackend {

  this: Suite =>

  implicit val tracingSupport: TracingSupport =
    LocalTracingSupport(EndpointConfig("unit-test", "localhost", 0))

  lazy val route = {
    val handler = new HandlerImpl(registry)
    seal(handler.route)
  }

}
