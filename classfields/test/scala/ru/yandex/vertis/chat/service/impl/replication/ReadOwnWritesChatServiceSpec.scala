package ru.yandex.vertis.chat.service.impl.replication

import ru.yandex.vertis.chat.SpecBase
import ru.yandex.vertis.chat.model.ModelGenerators.userId
import ru.yandex.vertis.chat.model._
import ru.yandex.vertis.chat.service.ServiceGenerators._
import ru.yandex.vertis.chat.service.impl.ChatServiceWrapper
import ru.yandex.vertis.chat.service.{ChatService, SendMessageResult}
import ru.yandex.vertis.chat.util.test.RequestContextAware

import scala.concurrent.duration.DurationInt

/**
  * Examines effects of [[ReadOwnWritesChatService]] over
  * implementation with replication lag.
  *
  * @author dimas
  */
class ReadOwnWritesChatServiceSpec extends SpecBase with RequestContextAware {

  val laggedService: ChatService =
    new ReplicatedChatService(50.milliseconds)

  val readOwnWritesService: ChatService =
    new ChatServiceWrapper(laggedService) with ReadOwnWritesChatService

  "ReadOwnWritesChatService" should {
    "enforce get own messages upon replication lag presence" in {
      val room = withUserContext(userId.next) { rc =>
        readOwnWritesService
          .createRoom(createRoomParameters.next)(rc)
          .futureValue
      }

      val SendMessageResult(message, _, _) =
        readOwnWritesService
          .sendMessage(sendMessageParameters(room).next)
          .futureValue

      val w = Window(None, 100, asc = true)

      withUserContext(message.author) { rc =>
        laggedService.getMessages(room.id, w)(rc).futureValue should be(empty)
        readOwnWritesService.getMessages(room.id, w)(rc).futureValue should be(Seq(message))
      }
    }
  }

}
