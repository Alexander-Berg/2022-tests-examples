package ru.yandex.vertis.chat.service.security

import ru.yandex.vertis.chat.components.dao.authority.{AuthorityService, JdbcAuthorityService}
import ru.yandex.vertis.chat.components.executioncontext.GlobalExecutionContextSupport
import ru.yandex.vertis.chat.components.time.{DefaultTimeServiceImpl, TimeService}
import ru.yandex.vertis.chat.service.ChatService
import ru.yandex.vertis.chat.service.impl.jdbc.{JdbcChatService, JdbcSpec}
import ru.yandex.vertis.chat.util.uuid.{RandomIdGenerator, TimeIdGenerator}

/**
  * Runnable spec for secured chat using [[JdbcAuthorityService]]
  */
class JdbcSecuredChatServiceSpec extends SecuredChatServiceSpecBase with JdbcSpec {

  override def timeService: TimeService = new DefaultTimeServiceImpl

  override def effectiveService: ChatService =
    new JdbcChatService(
      database,
      roomUuidGenerator = RandomIdGenerator,
      messageUuidGenerator = new TimeIdGenerator("localhost"),
      timeService = timeService
    ) with GlobalExecutionContextSupport

  override lazy val authorityService: AuthorityService =
    new JdbcAuthorityService(database)
}
