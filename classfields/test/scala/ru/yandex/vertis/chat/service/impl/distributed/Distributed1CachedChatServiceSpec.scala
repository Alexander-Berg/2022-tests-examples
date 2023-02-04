package ru.yandex.vertis.chat.service.impl.distributed

import ru.yandex.vertis.chat.components.dao.chat.cached.CachedChatService

/**
  * Specs on [[CachedChatService]] within singular "distributed" environment
  * with single instance.
  *
  * @author dimas
  */
class Distributed1CachedChatServiceSpec extends DistributedCachedChatServiceSpecBase {

  def numberOfInstances: Int = 1
}
