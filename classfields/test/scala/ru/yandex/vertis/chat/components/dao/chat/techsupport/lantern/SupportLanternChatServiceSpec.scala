package ru.yandex.vertis.chat.components.dao.chat.techsupport.lantern

import org.joda.time.DateTime
import org.mockito.Mockito._
import org.scalatest.OptionValues
import org.scalatest.concurrent.Eventually
import ru.yandex.vertis.chat.common.techsupport.TechSupportUtils
import ru.yandex.vertis.chat.components.dao.chat.techsupport.lantern.message.auto.{AutoBunkerOfflineMessage, AutoBunkerOverloadMessage}
import ru.yandex.vertis.chat.components.dao.chat.techsupport.lantern.message.{BunkerOfflineMessage, BunkerOverloadMessage}
import ru.yandex.vertis.chat.components.dao.lantern.{JvmLanternShowService, LanternShowService, LoggingLanternShowService}
import ru.yandex.vertis.chat.components.domains.DomainAware
import ru.yandex.vertis.chat.components.time.{FuncTimeServiceImpl, TimeService}
import ru.yandex.vertis.chat.components.workersfactory.SimpleWorkersFactorySupport
import ru.yandex.vertis.chat.model.{ModelGenerators, Window}
import ru.yandex.vertis.chat.service.impl.ChatServiceWrapper
import ru.yandex.vertis.chat.service.{ChatService, CreateMessageParameters, SendMessageResult, TestOperationalSupport}
import ru.yandex.vertis.chat.util.DMap
import ru.yandex.vertis.chat.util.test.RequestContextAware
import ru.yandex.vertis.chat.{Domain, Domains, RequestContext, SpecBase}
import ru.yandex.vertis.generators.ProducerProvider._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eeq}

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.Future

/**
  * TODO
  *
  * @author aborunov
  */
class SupportLanternChatServiceSpec
  extends SpecBase
  with RequestContextAware
  with MockitoSupport
  with OptionValues
  with Eventually {

  private class Fixture {
    val mockedChatService = mock[ChatService]

    val time = new AtomicReference(DateTime.now())

    val overloadMessage = new AtomicReference(
      AutoBunkerOverloadMessage(
        show_chat_notify = true,
        text_chat_notify = "text",
        show_chat_notify_dealer = true,
        text_chat_notify_dealer = "text",
        show_chat_notify_users_from_list = true,
        text_chat_notify_users_from_list = "text",
        users_list = Set(ModelGenerators.userId.next),
        show_chat_notify_dealers_from_list = true,
        text_chat_notify_dealers_from_list = "text",
        dealers_list = Set(ModelGenerators.dealerId.next)
      )
    )

    val offlineMessage = new AtomicReference(
      AutoBunkerOfflineMessage(show_chat_notify = true, text_chat_notify = "text")
    )

    val service: ChatService with SupportLanternChatService =
      new ChatServiceWrapper(mockedChatService)
        with SimpleWorkersFactorySupport
        with SupportLanternChatService
        with DomainAware
        with TestOperationalSupport {

        implicit override def domain: Domain = Domains.Auto

        override def sandbox: Boolean = false

        override val timeService: TimeService = new FuncTimeServiceImpl(time.get())

        override def bunkerOverloadMessage: DMap[BunkerOverloadMessage] = DMap.forAllDomains(overloadMessage.get())

        override def bunkerOfflineMessage: DMap[BunkerOfflineMessage] = DMap.forAllDomains(offlineMessage.get())

        override val lanternShowService: DMap[LanternShowService] =
          DMap.forAllDomains(new JvmLanternShowService with LoggingLanternShowService)
      }

    when(mockedChatService.getMessages(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(Seq.empty))
    when(mockedChatService.sendMessage(?)(?)).thenReturn(
      Future.successful(
        SendMessageResult(ModelGenerators.message.next, None)
      )
    )
  }

  "SupportLanternChatService" should {
    "send overload message" when {
      "private user opens tech support chat again on next day" in new Fixture {
        time.set(DateTime.now().withMillisOfDay(0).withHourOfDay(16))
        val user = ModelGenerators.userId.next
        withUserContext(user) { implicit rc =>
          val techSupportRoom = TechSupportUtils.roomId(user)
          var count = 0
          stub(mockedChatService.sendMessage(_: CreateMessageParameters)(_: RequestContext)) {
            case (params, _) =>
              count += 1
              Future.successful(SendMessageResult(ModelGenerators.message.next, None))
          }
          service.getMessages(techSupportRoom, Window(None, 200, asc = false)).futureValue
          eventually {
            count shouldBe 1
          }
          time.set(DateTime.now().withMillisOfDay(0).withHourOfDay(16).plusDays(1).plusMinutes(1))
          service.getMessages(techSupportRoom, Window(None, 200, asc = false)).futureValue
          eventually {
            verify(mockedChatService, times(2)).sendMessage(?)(?)
          }
        }
      }

      "user from list opens tech support chat" in new Fixture {
        overloadMessage.updateAndGet(
          _.copy(
            show_chat_notify = false,
            text_chat_notify = "",
            show_chat_notify_dealer = false,
            text_chat_notify_dealer = "",
            show_chat_notify_dealers_from_list = false,
            text_chat_notify_dealers_from_list = "",
            dealers_list = Set()
          )
        )
        time.set(DateTime.now().withMillisOfDay(0).withHourOfDay(16))
        val user = overloadMessage.get().users_list.head
        withUserContext(user) { implicit rc =>
          val techSupportRoom = TechSupportUtils.roomId(user)
          var overloadParams: CreateMessageParameters = null
          stub(mockedChatService.sendMessage(_: CreateMessageParameters)(_: RequestContext)) {
            case (params, _) =>
              overloadParams = params
              Future.successful(SendMessageResult(ModelGenerators.message.next, None))
          }
          service.getMessages(techSupportRoom, Window(None, 200, asc = false)).futureValue
          val overloadMessage = TechSupportUtils.privateOverloadMessage(user, "text")
          eventually {
            fix(overloadParams) shouldBe fix(overloadMessage)
          }
        }
      }

      "dealer from list opens tech support chat" in new Fixture {
        overloadMessage.updateAndGet(
          _.copy(
            show_chat_notify = false,
            text_chat_notify = "",
            show_chat_notify_dealer = false,
            text_chat_notify_dealer = "",
            show_chat_notify_users_from_list = false,
            text_chat_notify_users_from_list = "",
            users_list = Set()
          )
        )
        time.set(DateTime.now().withMillisOfDay(0).withHourOfDay(16))
        val dealer = overloadMessage.get().dealers_list.head
        withUserContext(dealer) { implicit rc =>
          val techSupportRoom = TechSupportUtils.roomId(dealer)
          var overloadParams: CreateMessageParameters = null
          stub(mockedChatService.sendMessage(_: CreateMessageParameters)(_: RequestContext)) {
            case (params, _) =>
              overloadParams = params
              Future.successful(SendMessageResult(ModelGenerators.message.next, None))
          }
          service.getMessages(techSupportRoom, Window(None, 200, asc = false)).futureValue
          val overloadMessage = TechSupportUtils.dealerOverloadMessage(dealer, "text")
          eventually {
            fix(overloadParams) shouldBe fix(overloadMessage)
          }
        }
      }

      "dealer opens tech support chat" in new Fixture {
        time.set(DateTime.now().withMillisOfDay(0).withHourOfDay(16))
        val dealer = ModelGenerators.dealerId.next
        withUserContext(dealer) { implicit rc =>
          val techSupportRoom = TechSupportUtils.roomId(dealer)
          var overloadParams: CreateMessageParameters = null
          stub(mockedChatService.sendMessage(_: CreateMessageParameters)(_: RequestContext)) {
            case (params, _) =>
              overloadParams = params
              Future.successful(SendMessageResult(ModelGenerators.message.next, None))
          }
          service.getMessages(techSupportRoom, Window(None, 200, asc = false)).futureValue
          val overloadMessage = TechSupportUtils.dealerOverloadMessage(dealer, "text")
          eventually {
            fix(overloadParams) shouldBe fix(overloadMessage)
          }
        }
      }

      "private user opens tech support chat" in new Fixture {
        time.set(DateTime.now().withMillisOfDay(0).withHourOfDay(16))
        val user = ModelGenerators.userId.next
        withUserContext(user) { implicit rc =>
          val techSupportRoom = TechSupportUtils.roomId(user)
          var overloadParams: CreateMessageParameters = null
          stub(mockedChatService.sendMessage(_: CreateMessageParameters)(_: RequestContext)) {
            case (params, _) =>
              overloadParams = params
              Future.successful(SendMessageResult(ModelGenerators.message.next, None))
          }
          service.getMessages(techSupportRoom, Window(None, 200, asc = false)).futureValue
          val overloadMessage = TechSupportUtils.privateOverloadMessage(user, "text")
          eventually {
            fix(overloadParams) shouldBe fix(overloadMessage)
          }
        }
      }
    }

    "not send overload message" when {
      "open room again very quickly" in new Fixture {
        val user = ModelGenerators.userId.next
        val techSupportRoom = TechSupportUtils.roomId(user)
        time.set(DateTime.now().withMillisOfDay(0).withHourOfDay(12))
        when(mockedChatService.getMessages(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(Seq()))
        withUserContext(user) { implicit rc =>
          var count = 0
          var overloadParams: CreateMessageParameters = null
          stub(mockedChatService.sendMessage(_: CreateMessageParameters)(_: RequestContext)) {
            case (params, _) =>
              if (params.roomLocator.flatMap(_.asSource).exists(_.parameters.participants.find(rc.user).isDefined)) {
                overloadParams = params
                count += 1
              }
              Future.successful(SendMessageResult(ModelGenerators.message.next, None))
          }
          // шлем два раза, сообщение должно быть один раз
          service.getMessages(techSupportRoom, Window(None, 200, asc = false)).futureValue
          service.getMessages(techSupportRoom, Window(None, 200, asc = false)).futureValue
          val overloadMessage = TechSupportUtils.privateOverloadMessage(user, "text")
          Thread.sleep(2000)
          eventually {
            count shouldBe 1
            val fixedParams = fix(overloadParams)
            val fixedMessage = fix(overloadMessage)
            fixedParams shouldBe fixedMessage
          }
        }
      }

      "it is not a tech support room" in new Fixture {
        time.set(DateTime.now().withMillisOfDay(0).withHourOfDay(16))
        val user = ModelGenerators.userId.next
        withUserContext(user) { implicit rc =>
          val room = ModelGenerators.roomId.next
          service.getMessages(room, Window(None, 200, asc = false)).futureValue
          Thread.sleep(2000)
          verify(mockedChatService, never()).sendMessage(?)(eeq(rc))
        }
      }

      "disabled in bunker for dealer" in new Fixture {
        time.set(DateTime.now().withMillisOfDay(0).withHourOfDay(16))
        overloadMessage.updateAndGet(_.copy(show_chat_notify_dealer = false))
        val dealer = ModelGenerators.dealerId.next
        withUserContext(dealer) { implicit rc =>
          val techSupportRoom = TechSupportUtils.roomId(dealer)
          service.getMessages(techSupportRoom, Window(None, 200, asc = false)).futureValue
          Thread.sleep(2000)
          verify(mockedChatService, never()).sendMessage(?)(eeq(rc))
        }
      }

      "text is empty in bunker for dealer" in new Fixture {
        time.set(DateTime.now().withMillisOfDay(0).withHourOfDay(16))
        overloadMessage.updateAndGet(_.copy(text_chat_notify_dealer = ""))
        val dealer = ModelGenerators.dealerId.next
        withUserContext(dealer) { implicit rc =>
          val techSupportRoom = TechSupportUtils.roomId(dealer)
          service.getMessages(techSupportRoom, Window(None, 200, asc = false)).futureValue
          Thread.sleep(2000)
          verify(mockedChatService, never()).sendMessage(?)(eeq(rc))
        }
      }

      "disabled in bunker for private user" in new Fixture {
        time.set(DateTime.now().withMillisOfDay(0).withHourOfDay(16))
        overloadMessage.updateAndGet(_.copy(show_chat_notify = false))
        val user = ModelGenerators.userId.next
        withUserContext(user) { implicit rc =>
          val techSupportRoom = TechSupportUtils.roomId(user)
          service.getMessages(techSupportRoom, Window(None, 200, asc = false)).futureValue
          Thread.sleep(2000)
          verify(mockedChatService, never()).sendMessage(?)(eeq(rc))
        }
      }

      "text is empty in bunker for private user" in new Fixture {
        time.set(DateTime.now().withMillisOfDay(0).withHourOfDay(16))
        overloadMessage.updateAndGet(_.copy(text_chat_notify = ""))
        val user = ModelGenerators.userId.next
        withUserContext(user) { implicit rc =>
          val techSupportRoom = TechSupportUtils.roomId(user)
          service.getMessages(techSupportRoom, Window(None, 200, asc = false)).futureValue
          Thread.sleep(2000)
          verify(mockedChatService, never()).sendMessage(?)(eeq(rc))
        }
      }

      "disabled in bunker for users list" in new Fixture {
        time.set(DateTime.now().withMillisOfDay(0).withHourOfDay(16))
        overloadMessage.updateAndGet(_.copy(show_chat_notify_users_from_list = false, show_chat_notify = false))
        val user = overloadMessage.get().users_list.head
        withUserContext(user) { implicit rc =>
          val techSupportRoom = TechSupportUtils.roomId(user)
          service.getMessages(techSupportRoom, Window(None, 200, asc = false)).futureValue
          Thread.sleep(2000)
          verify(mockedChatService, never()).sendMessage(?)(eeq(rc))
        }
      }

      "text is empty for users list" in new Fixture {
        time.set(DateTime.now().withMillisOfDay(0).withHourOfDay(16))
        overloadMessage.updateAndGet(_.copy(text_chat_notify_users_from_list = "", text_chat_notify = ""))
        val user = overloadMessage.get().users_list.head
        withUserContext(user) { implicit rc =>
          val techSupportRoom = TechSupportUtils.roomId(user)
          service.getMessages(techSupportRoom, Window(None, 200, asc = false)).futureValue
          Thread.sleep(2000)
          verify(mockedChatService, never()).sendMessage(?)(eeq(rc))
        }
      }

      "disabled in bunker for dealers list" in new Fixture {
        time.set(DateTime.now().withMillisOfDay(0).withHourOfDay(16))
        overloadMessage.updateAndGet(
          _.copy(show_chat_notify_dealers_from_list = false, show_chat_notify_dealer = false)
        )
        val dealer = overloadMessage.get().dealers_list.head
        withUserContext(dealer) { implicit rc =>
          val techSupportRoom = TechSupportUtils.roomId(dealer)
          service.getMessages(techSupportRoom, Window(None, 200, asc = false)).futureValue
          Thread.sleep(2000)
          verify(mockedChatService, never()).sendMessage(?)(eeq(rc))
        }
      }

      "text is empty for dealers list" in new Fixture {
        time.set(DateTime.now().withMillisOfDay(0).withHourOfDay(16))
        overloadMessage.updateAndGet(_.copy(text_chat_notify_dealers_from_list = "", text_chat_notify_dealer = ""))
        val dealer = overloadMessage.get().dealers_list.head
        withUserContext(dealer) { implicit rc =>
          val techSupportRoom = TechSupportUtils.roomId(dealer)
          service.getMessages(techSupportRoom, Window(None, 200, asc = false)).futureValue
          Thread.sleep(2000)
          verify(mockedChatService, never()).sendMessage(?)(eeq(rc))
        }
      }

      "already sent today" in new Fixture {
        val user = ModelGenerators.userId.next
        val techSupportRoom = TechSupportUtils.roomId(user)
        time.set(DateTime.now().withMillisOfDay(0).withHourOfDay(12))
        val m = ModelGenerators.message.next
        withUserContext(user) { implicit rc =>
          var count = 0
          var overloadParams: CreateMessageParameters = null
          stub(mockedChatService.sendMessage(_: CreateMessageParameters)(_: RequestContext)) {
            case (params, _) =>
              if (params.roomLocator.flatMap(_.asSource).exists(_.parameters.participants.find(rc.user).isDefined)) {
                overloadParams = params
                count += 1
              }
              Future.successful(SendMessageResult(ModelGenerators.message.next, None))
          }
          // шлем два раза, сообщение должно быть один раз
          service.getMessages(techSupportRoom, Window(None, 200, asc = false)).futureValue
          val overloadMessage = TechSupportUtils.privateOverloadMessage(user, "text")
          eventually {
            count shouldBe 1
            val fixedParams = fix(overloadParams)
            val fixedMessage = fix(overloadMessage)
            fixedParams shouldBe fixedMessage
          }
          time.set(time.get().plusHours(1))
          service.getMessages(techSupportRoom, Window(None, 200, asc = false)).futureValue
          Thread.sleep(2000L)
          eventually {
            count shouldBe 1
          }
          verify(mockedChatService).sendMessage(?)(?)
        }
      }
    }
  }

  private def fix(params: CreateMessageParameters): CreateMessageParameters = {
    params.copy(providedId = keepPrefixOnly(params))
  }

  private def keepPrefixOnly(params: CreateMessageParameters) = {
    params.providedId.map(_.split("_").head)
  }
}
