package ru.yandex.vertis.subscriptions.service.impl

import ru.yandex.vertis.subscriptions.TestExecutionContext
import ru.yandex.vertis.subscriptions.core.plugin.TrivialRequestParser
import ru.yandex.vertis.subscriptions.model.Services
import ru.yandex.vertis.subscriptions.service.{OwnerLinksService, SubscriptionService, SubscriptionServiceSpecBase}
import ru.yandex.vertis.subscriptions.storage.memory.{SubscriptionsDao => JvmSubscriptionDao}

/**
  * Base spec for [[LinkedSubscriptionService]].
  *
  * @author dimas
  */
trait LinkedSubscriptionServiceSpecBase extends SubscriptionServiceSpecBase with TestExecutionContext {

  private def effectiveService: SubscriptionService =
    new SubscriptionServiceImpl(new JvmSubscriptionDao, Services.Auto, TrivialRequestParser, sandboxMode = true)

  def ownerLinksService: OwnerLinksService

  lazy val service: SubscriptionService =
    new LinkedSubscriptionService(effectiveService, ownerLinksService)

}
