package ru.yandex.vertis.chat.service.security

import ru.yandex.vertis.chat.components.dao.authority.{AuthorityService, JvmAuthorityService}
import ru.yandex.vertis.chat.components.time.{DefaultTimeServiceImpl, TimeService}
import ru.yandex.vertis.chat.service.ChatService
import ru.yandex.vertis.chat.service.impl.jvm.{JvmChatService, JvmChatState}

/**
  * Runnable spec for secured chat with [[JvmAuthorityService]]
  */
class JvmSecuredChatServiceSpec extends SecuredChatServiceSpecBase {

  override def timeService: TimeService = new DefaultTimeServiceImpl
  private lazy val state = JvmChatState.empty()

  override def effectiveService: ChatService =
    new JvmChatService(state)

  override lazy val authorityService: AuthorityService =
    new JvmAuthorityService(state, timeService)

}
