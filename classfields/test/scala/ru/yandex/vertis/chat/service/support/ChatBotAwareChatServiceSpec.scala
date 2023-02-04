package ru.yandex.vertis.chat.service.support

import ru.yandex.vertis.chat.common.chatbot.ChatBotUtils
import ru.yandex.vertis.chat.components.clients.pushnoy.PushnoyClient
import ru.yandex.vertis.chat.components.dao.chat.chatbot.ChatBotAwareChatService
import ru.yandex.vertis.chat.components.dao.chat.chatbot.data.ChatBotData
import ru.yandex.vertis.chat.components.executioncontext.SameThreadExecutionContextSupport
import ru.yandex.vertis.chat.components.time.{SetTimeServiceImpl, TimeService}
import ru.yandex.vertis.chat.components.workersfactory.workers.{TestWorkersFactory, WorkersFactory}
import ru.yandex.vertis.chat.model.ModelGenerators
import ru.yandex.vertis.chat.service.ServiceGenerators._
import ru.yandex.vertis.chat.service.impl.ChatServiceWrapper
import ru.yandex.vertis.chat.service.impl.jvm.JvmChatService
import ru.yandex.vertis.chat.service.{ChatService, ChatServiceSpecBase, LoggingChatService, TestOperationalSupport}
import ru.yandex.vertis.chat.util.DMap
import ru.yandex.vertis.chat.{Domain, Domains}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

/**
  *
  * @author Rustam Guseyn-zade
  */
class ChatBotAwareChatServiceSpec extends ChatServiceSpecBase with MockitoSupport {

  private val pushnoy = mock[PushnoyClient]

  val service: ChatService =
    new JvmChatService()
      with ChatBotAwareChatService
      with LoggingChatService
      with TestOperationalSupport
      with SameThreadExecutionContextSupport {

      override def pushnoyClient: DMap[PushnoyClient] = DMap.forAllDomains(pushnoy)

      override def chatBotData(implicit domain: Domain) = mock[ChatBotData]

      override def workersFactory: WorkersFactory = new TestWorkersFactory

      override def timeService: TimeService = new SetTimeServiceImpl

      implicit override def domain: Domain = Domains.Auto
    }

  "ChatBotAwareChatService" should {
    val m = mock[ChatService]
    val chatBotAware = new ChatServiceWrapper(m)

    "provide chat bot room on getRoomsById request, if it was not provided by db" in pendingUntilFixed {
      val user = ModelGenerators.userId.next
      val chatBotRoom = ChatBotUtils.chatBotRoom(user)
      when(m.getRoomsByIds(?)(?)).thenReturn(Future.successful(Seq()))

      withUserContext(user) { implicit rc =>
        chatBotAware.getRoomsByIds(Iterable(chatBotRoom.id)).futureValue shouldBe Seq(chatBotRoom)
      }
    }
  }

}
