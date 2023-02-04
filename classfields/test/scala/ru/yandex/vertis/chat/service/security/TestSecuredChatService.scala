package ru.yandex.vertis.chat.service.security

import ru.yandex.vertis.chat.components.dao.authority.AuthorityService
import ru.yandex.vertis.chat.components.dao.chat.SecuredChatService
import ru.yandex.vertis.chat.components.dao.security.SecurityContextProvider
import ru.yandex.vertis.chat.components.domains.DomainAutoruSupport
import ru.yandex.vertis.chat.components.executioncontext.SameThreadExecutionContextSupport
import ru.yandex.vertis.chat.components.time.TimeService
import ru.yandex.vertis.chat.service.ChatService
import ru.yandex.vertis.chat.service.impl.ChatServiceWrapper
import ru.yandex.vertis.chat.util.DMap

import scala.concurrent.ExecutionContext

/**
  * TODO
  *
  * @author aborunov
  */
object TestSecuredChatService {

  /**
    * Wraps given chat service with security checks executed with support of given tracker.
    */
  def wrap(
      chatService: ChatService,
      authorityService: AuthorityService,
      timeService: TimeService
  )(implicit ec: ExecutionContext): SecuredChatService = {
    new ChatServiceWrapper(chatService)
      with SecuredChatService
      with SameThreadExecutionContextSupport
      with DomainAutoruSupport {
      override val securityContextProvider: DMap[SecurityContextProvider] =
        DMap.forAllDomains(
          new SecurityContextProvider(authorityService, timeService)
        )
    }
  }
}
