package ru.yandex.vertis.chat.components.dao.security.authority

import ru.yandex.vertis.chat.components.dao.authority.{AuthorityService, JdbcAuthorityService}
import ru.yandex.vertis.chat.components.executioncontext.SameThreadExecutionContextSupport
import ru.yandex.vertis.chat.components.time.TestTimeSupport
import ru.yandex.vertis.chat.service.ChatService
import ru.yandex.vertis.chat.service.impl.jdbc.{JdbcChatService, JdbcSpec}
import ru.yandex.vertis.chat.util.uuid.{RandomIdGenerator, TimeIdGenerator}

class JdbcAuthorityServiceSpec extends AuthorityServiceSpecBase with JdbcSpec with TestTimeSupport {

  override lazy val authorityService: AuthorityService =
    new JdbcAuthorityService(database)

  override lazy val service: ChatService =
    new JdbcChatService(
      database,
      RandomIdGenerator,
      new TimeIdGenerator("test"),
      timeService
    ) with SameThreadExecutionContextSupport

}
