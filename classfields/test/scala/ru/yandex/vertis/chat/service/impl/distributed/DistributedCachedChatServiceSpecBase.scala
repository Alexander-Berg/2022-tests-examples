package ru.yandex.vertis.chat.service.impl.distributed

import ru.yandex.vertis.chat.{Domains, RequestContext}
import ru.yandex.vertis.chat.components.cache.CacheService
import ru.yandex.vertis.chat.components.cache.local.JvmCache
import ru.yandex.vertis.chat.components.cache.metrics.NopCacheMetricsImpl
import ru.yandex.vertis.chat.components.cache.userlocality.UserLocalityCacheService
import ru.yandex.vertis.chat.components.dao.chat.cached.CachedChatService
import ru.yandex.vertis.chat.components.dao.chat.userlocality.UserLocalityChatService
import ru.yandex.vertis.chat.components.domains.DomainAutoruSupport
import ru.yandex.vertis.chat.model.UserId
import ru.yandex.vertis.chat.service.impl.ChatServiceWrapper
import ru.yandex.vertis.chat.service.impl.jvm.{JvmChatService, JvmChatState}
import ru.yandex.vertis.chat.service.{ChatService, ChatServiceSpecBase}
import ru.yandex.vertis.chat.util.DMap

/**
  * Specs on [[CachedChatService]] within "distributed" environment,
  * e.g. where participating several instances.
  * The number of instances is specified by `numberOfInstances` value.
  *
  * @author dimas
  */
abstract class DistributedCachedChatServiceSpecBase extends ChatServiceSpecBase {

  /**
    * Specifies number of [[ChatService]] instances participating in test scenario.
    */
  def numberOfInstances: Int

  private val state = JvmChatState.empty()

  private val localCaches = (0 until numberOfInstances).map { _ =>
    DMap.forAllDomains(new JvmCache with NopCacheMetricsImpl with DomainAutoruSupport {
      override val persistentChatService: DMap[ChatService] = DMap.forAllDomains(new JvmChatService(state))
    })
  }.toArray

  private val cacheServiceLocator =
    new Locator[CacheService] {

      def locate(user: UserId)(implicit rc: RequestContext): CacheService = {
        localCaches(user.hashCode.abs % numberOfInstances).value(Domains.Auto)
      }
    }

  private val services = (0 until numberOfInstances).map { i =>
    val jvmChatService = new JvmChatService(state)
    new ChatServiceWrapper(jvmChatService) with CachedChatService with DomainAutoruSupport {
      override val localCacheService: DMap[JvmCache] = localCaches(i)

      override val userLocalityCacheService: DMap[CacheService] = DMap.forAllDomains(
        new UserLocalityCacheService(cacheServiceLocator)
      )
    }
  }.toList

  private val chatServiceLocator =
    new Locator[ChatService] {

      def locate(user: UserId)(implicit rc: RequestContext): ChatService = {
        services(user.hashCode.abs % numberOfInstances)
      }
    }

  val service: ChatService = new UserLocalityChatService(chatServiceLocator)
}
