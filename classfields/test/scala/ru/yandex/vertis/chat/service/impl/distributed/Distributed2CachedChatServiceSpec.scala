package ru.yandex.vertis.chat.service.impl.distributed

import ru.yandex.vertis.chat.components.dao.chat.cached.CachedChatService

/**
  * Specs on [[CachedChatService]] within "distributed" environment
  * with two instances.
  *
  * @author dimas
  */
class Distributed2CachedChatServiceSpec extends DistributedCachedChatServiceSpecBase {

  def numberOfInstances: Int = 2
}
