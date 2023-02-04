package ru.yandex.vertis.chat.service.impl.jdbc

import ru.yandex.vertis.chat.components.dao.chat.storage.ChatStorage
import ru.yandex.vertis.chat.components.dao.statistics.{InStorageStatisticsService, StatisticsService}
import ru.yandex.vertis.chat.components.domains.DomainAutoruSupport
import ru.yandex.vertis.chat.components.executioncontext.GlobalExecutionContextSupport
import ru.yandex.vertis.chat.components.time.DefaultTimeServiceImpl
import ru.yandex.vertis.chat.service.impl.TestDomainAware
import ru.yandex.vertis.chat.service.{ChatService, ChatStatisticsSpecBase}
import ru.yandex.vertis.chat.util.DMap
import ru.yandex.vertis.chat.util.uuid.{RandomIdGenerator, TimeIdGenerator}

class JdbcChatStatisticsSpec extends ChatStatisticsSpecBase with JdbcSpec with TestDomainAware {

  private val jdbcService = new JdbcChatService(
    database,
    roomUuidGenerator = RandomIdGenerator,
    messageUuidGenerator = new TimeIdGenerator("localhost"),
    timeService = new DefaultTimeServiceImpl
  ) with GlobalExecutionContextSupport

  private val jdbcStatistics = new InStorageStatisticsService with DomainAutoruSupport {
    override val chatStorage: DMap[ChatStorage] = DMap.forAllDomains(database)
  }

  override def service: ChatService = jdbcService

  override def statistics: StatisticsService = jdbcStatistics
}
