package ru.yandex.vertis.subscriptions.service.impl

import ru.yandex.vertis.subscriptions.service.{WatchService, WatchServiceSpecBase}
import ru.yandex.vertis.subscriptions.storage.SubscriptionsDao

/**
  * Base spec on [[WatchService]] over [[SubscriptionsDao]].
  *
  * @author dimas
  */
trait WatchServiceImplSpecBase extends WatchServiceSpecBase {

  def subscriptionsDao: SubscriptionsDao

  lazy val watchService: WatchService =
    new WatchServiceImpl(subscriptionsDao, "test", sandboxMode = false)
}
