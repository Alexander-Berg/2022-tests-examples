package ru.yandex.vertis.chat.service.security

import ru.yandex.vertis.chat.components.cache.metrics.NopCacheMetricsImpl
import ru.yandex.vertis.chat.components.dao.authority.{AuthorityService, CachingAuthorityService, JvmAuthorityService}
import ru.yandex.vertis.chat.components.domains.DomainAutoruSupport
import ru.yandex.vertis.chat.components.time.{DefaultTimeServiceImpl, TimeService}
import ru.yandex.vertis.chat.service.impl.jvm.{JvmChatService, JvmChatState}
import ru.yandex.vertis.chat.service.{ChatService, LoggingChatService}

/**
  * Runnable spec for secured chat with [[CachingAuthorityService]]
  *
  * @author 747mmhg
  */
class CachingSecuredChatServiceSpec extends SecuredChatServiceSpecBase {

  override def timeService: TimeService = new DefaultTimeServiceImpl

  private lazy val state = JvmChatState.empty()

  override def effectiveService: ChatService =
    new JvmChatService(state) with LoggingChatService {
      override protected def loggerClass: Class[_] = classOf[ChatService]
    }

  override lazy val authorityService: AuthorityService =
    new JvmAuthorityService(state, timeService)
      with CachingAuthorityService
      with NopCacheMetricsImpl
      with DomainAutoruSupport
}
