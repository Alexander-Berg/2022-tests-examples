package ru.yandex.vertis.chat.service.impl.distributed

import ru.yandex.vertis.chat.components.dao.chat.cached.CachedChatService

/**
  * Specs on [[CachedChatService]] within "distributed" environment
  * with ten instances.
  *
  * @author dimas
  */
class Distributed10CachedChatServiceSpec extends DistributedCachedChatServiceSpecBase {

  def numberOfInstances: Int = 10
}
