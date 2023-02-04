package ru.yandex.vertis.chat.service.security

import org.scalatest.OptionValues
import ru.yandex.vertis.MimeType
import ru.yandex.vertis.chat.{Domains, SpecBase}
import ru.yandex.vertis.chat.common.techsupport.TechSupportUtils
import ru.yandex.vertis.chat.components.app.{Application, TestApplication}
import ru.yandex.vertis.chat.components.app.config.LocalAppConfig
import ru.yandex.vertis.chat.components.cache.metrics.NopCacheMetricsImpl
import ru.yandex.vertis.chat.components.clients.jivosite.TechSupportClient
import ru.yandex.vertis.chat.components.clients.passport.{NopPassportClient, PassportClient}
import ru.yandex.vertis.chat.components.clients.pushnoy.{NopPushnoyClient, PushnoyClient}
import ru.yandex.vertis.chat.components.clients.techsupport.{TechSupportDestinationDecider, VertisTechSupportService}
import ru.yandex.vertis.chat.components.dao.authority.{AuthorityService, CachingAuthorityService, JvmAuthorityService}
import ru.yandex.vertis.chat.components.dao.chat.SecuredChatService
import ru.yandex.vertis.chat.components.dao.chat.techsupport.TechSupportChatService
import ru.yandex.vertis.chat.components.dao.scheduledactions.ScheduledActions
import ru.yandex.vertis.chat.components.dao.security.SecurityContextProvider
import ru.yandex.vertis.chat.components.dao.techsupport.polls.messages.{PollMessages, PollMessagesService}
import ru.yandex.vertis.chat.components.domains.DomainAutoruSupport
import ru.yandex.vertis.chat.components.executioncontext.SameThreadExecutionContextSupport
import ru.yandex.vertis.chat.components.time.DefaultTimeServiceImpl
import ru.yandex.vertis.chat.config.StaticLocatorConfig
import ru.yandex.vertis.chat.model.{MessagePayload, Window}
import ru.yandex.vertis.chat.service._
import ru.yandex.vertis.chat.service.impl.ChatServiceWrapper
import ru.yandex.vertis.chat.service.impl.jvm.{JvmChatService, JvmChatState}
import ru.yandex.vertis.chat.service.support.polls.TestPollMessagesService
import ru.yandex.vertis.chat.util.DMap
import ru.yandex.vertis.chat.util.test.RequestContextAware
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

/**
  * TODO
  *
  * @author aborunov
  */
class CachingSecuredTechSupportChatServiceSpec
  extends SpecBase
  with ChatServiceTestKit
  with RequestContextAware
  with OptionValues
  with MockitoSupport {
  private lazy val state = JvmChatState.empty()
  private val timeService = new DefaultTimeServiceImpl

  private val authorityService: AuthorityService =
    new JvmAuthorityService(state, timeService) with CachingAuthorityService with NopCacheMetricsImpl

  private val mockedTechSupportClient = mock[TechSupportClient]

  private val mockedScheduledActions = mock[ScheduledActions]

  private val mockedTechsupportDestinationDecider =
    mock[TechSupportDestinationDecider]

  private val mockedTechsupportEventService = mock[VertisTechSupportService]
  private val staticLocatorConfig = new StaticLocatorConfig
  private val domain = Domains.Auto
  private val config = new LocalAppConfig(staticLocatorConfig, domain)
  private val application = new TestApplication(config)

  val service: ChatService =
    new ChatServiceWrapper(new JvmChatService(state))
      with SecuredChatService
      with TechSupportChatService
      with SameThreadExecutionContextSupport
      with LoggingChatService
      with DomainAutoruSupport {
      override protected def loggerClass: Class[_] = classOf[ChatService]

      override val vertisTechSupportService: DMap[VertisTechSupportService] =
        DMap.forAllDomains(mockedTechsupportEventService)

      override val passportClient: DMap[PassportClient] =
        DMap.forAllDomains(NopPassportClient)

      override val pushnoyClient: DMap[PushnoyClient] =
        DMap.forAllDomains(NopPushnoyClient)

      implicit override def mdsReadUrlPrefix: String =
        "//avatars.mdst.yandex.net"

      override val pollMessagesService: DMap[PollMessagesService] =
        DMap.forAllDomains(new TestPollMessagesService(PollMessages()))

      override val securityContextProvider: DMap[SecurityContextProvider] =
        DMap.forAllDomains(
          new SecurityContextProvider(authorityService, timeService)
        )

      override def techSupportClient: DMap[TechSupportClient] =
        DMap.forAllDomains(mockedTechSupportClient)

      override val scheduledActions: DMap[ScheduledActions] =
        DMap.forAllDomains(mockedScheduledActions)

      override val techSupportDestinationDecider: DMap[TechSupportDestinationDecider] =
        DMap.forAllDomains(mockedTechsupportDestinationDecider)

      override def app: Application = application
    }

  "CachingSecuredChatService" should {
    "provide correct messaging with tech support" in {
      when(mockedTechSupportClient.send(?, ?)(?)).thenReturn(Future.unit)
      when(mockedTechsupportDestinationDecider.destination(?)(?))
        .thenReturn(TechSupportDestinationDecider.Destination.JivositePrivate)
      val user1 = "user:22045034"
      val techSupportRoom = TechSupportUtils.roomId(user1)

      val messages0 = withUserContext(user1) { implicit rc =>
        service
          .getMessages(techSupportRoom, Window(None, 100, asc = false))
          .futureValue
      }

      messages0.length shouldBe 0

      withUserContext(user1) { implicit rc =>
        service
          .sendMessage(
            CreateMessageParameters(
              room = techSupportRoom,
              author = user1,
              payload = MessagePayload(MimeType.TEXT_PLAIN, "message1"),
              attachments = Seq.empty,
              providedId = None
            )
          )
          .futureValue
      }

      val messages1 = withUserContext(user1) { implicit rc =>
        service
          .getMessages(techSupportRoom, Window(None, 100, asc = false))
          .futureValue
      }

      messages1.length shouldBe 1
    }
  }
}
