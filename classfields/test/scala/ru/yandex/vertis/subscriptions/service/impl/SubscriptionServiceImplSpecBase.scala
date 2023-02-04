package ru.yandex.vertis.subscriptions.service.impl

import ru.yandex.vertis.subscriptions.core.plugin.TrivialRequestParser
import ru.yandex.vertis.subscriptions.model.Services
import ru.yandex.vertis.subscriptions.service.{SubscriptionService, SubscriptionServiceSpecBase}
import ru.yandex.vertis.subscriptions.storage.SubscriptionsDao

/**
  * Base specs for [[SubscriptionServiceImpl]].
  *
  * @author dimas
  */
trait SubscriptionServiceImplSpecBase extends SubscriptionServiceSpecBase {

  def dao: SubscriptionsDao

  lazy val service: SubscriptionService =
    new SubscriptionServiceImpl(dao, Services.Auto, TrivialRequestParser, sandboxMode = true)
}
