package ru.yandex.vertis.billing.banker.api.base_admin

import org.scalatest.Suite
import ru.yandex.common.monitoring.ping.SignalSwitchingDecider
import ru.yandex.vertis.billing.banker.api.HandlerImpl

/**
  * Base spec which initialize route
  *
  * @author ruslansd
  */
private[api] trait RootHandlerSpecBase extends HandlerSpecBase with MockedAdminBackend {

  this: Suite =>

  val route = {
    val decider = new SignalSwitchingDecider()
    val handler = new HandlerImpl(decider, registry)
    seal(handler.route)
  }

}
