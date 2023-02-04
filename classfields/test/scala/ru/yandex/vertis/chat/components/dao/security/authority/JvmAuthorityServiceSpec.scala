package ru.yandex.vertis.chat.components.dao.security.authority

import ru.yandex.vertis.chat.components.dao.authority.{AuthorityService, JvmAuthorityService}
import ru.yandex.vertis.chat.components.time.{DefaultTimeServiceImpl, TimeService}
import ru.yandex.vertis.chat.service.ChatService
import ru.yandex.vertis.chat.service.impl.jvm.{JvmChatService, JvmChatState}

class JvmAuthorityServiceSpec extends AuthorityServiceSpecBase {

  override val timeService: TimeService = new DefaultTimeServiceImpl

  private val state: JvmChatState = JvmChatState.empty()

  override lazy val authorityService: AuthorityService =
    new JvmAuthorityService(state, timeService)

  override lazy val service: ChatService =
    new JvmChatService(state)
}
