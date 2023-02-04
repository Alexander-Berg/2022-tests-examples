package ru.yandex.vertis.chat.service.impl

import ru.yandex.vertis.chat.components.cache.metrics.NopCacheMetricsImpl
import ru.yandex.vertis.chat.components.dao.chat.statistics.StatisticsChatService
import ru.yandex.vertis.chat.components.dao.chat.storage.{ChatStorage, JvmStorage}
import ru.yandex.vertis.chat.components.dao.statistics.{InMemoryStatisticsService, InStorageStatisticsService}
import ru.yandex.vertis.chat.components.dao.statistics.{StatisticsCallbacks, StatisticsService}
import ru.yandex.vertis.chat.components.domains.DomainAutoruSupport
import ru.yandex.vertis.chat.service.ChatStatisticsSpecBase
import ru.yandex.vertis.chat.service.impl.jvm.{JvmChatService, JvmChatState}
import ru.yandex.vertis.chat.util.DMap

import scala.concurrent.ExecutionContext

/**
  * Runnable specs on [[ru.yandex.vertis.chat.service.ChatService]].
  *
  * @author 747mmhg
  */
class CachingChatStatisticsSpec extends ChatStatisticsSpecBase {

  private val state = JvmChatState.empty()

  private val statisticsService = new InStorageStatisticsService
    with DomainAutoruSupport
    with InMemoryStatisticsService
    with NopCacheMetricsImpl {
    override val chatStorage: DMap[ChatStorage] = DMap.forAllDomains(JvmStorage(state))
  }

  private val chatService = new JvmChatService(state) with StatisticsChatService with DomainAutoruSupport {
    override def statisticsCallbacks: DMap[StatisticsCallbacks] = DMap.forAllDomains(statisticsService)

    implicit override def ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  }

  override def service: JvmChatService = chatService

  override def statistics: StatisticsService = statisticsService
}
