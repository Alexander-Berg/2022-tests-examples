package ru.yandex.vertis.chat.service.impl.jvm

import ru.yandex.vertis.chat.components.dao.chat.storage.{ChatStorage, JvmStorage}
import ru.yandex.vertis.chat.components.dao.statistics.{InStorageStatisticsService, StatisticsService}
import ru.yandex.vertis.chat.components.domains.DomainAutoruSupport
import ru.yandex.vertis.chat.service.ChatStatisticsSpecBase
import ru.yandex.vertis.chat.util.DMap

class JvmChatStatisticsSpec extends ChatStatisticsSpecBase {

  private val state = JvmChatState.empty()

  private val jvmService = new JvmChatService(state)

  private val storage = JvmStorage(state)

  private val jvmStatistics = new InStorageStatisticsService with DomainAutoruSupport {
    override def chatStorage: DMap[ChatStorage] = DMap.forAllDomains(storage)
  }

  override def service: JvmChatService = jvmService

  override def statistics: StatisticsService = jvmStatistics
}
