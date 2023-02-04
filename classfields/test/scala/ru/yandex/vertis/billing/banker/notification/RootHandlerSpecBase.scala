package ru.yandex.vertis.billing.banker.notification

import org.scalatest.Suite
import ru.yandex.common.monitoring.ping.SignalSwitchingDecider
import ru.yandex.vertis.billing.banker.api.{HandlerSpecBase, MockedApiBackend}

/**
  * Base spec which initialize route
  *
  * @author ruslansd
  */
private[notification] trait RootHandlerSpecBase extends HandlerSpecBase with MockedApiBackend {

  this: Suite with MockedApiBackend =>

  val route = {
    val decider = new SignalSwitchingDecider()
    val handler = new HandlerImpl(registry, decider)
    seal(handler.route)
  }

}
