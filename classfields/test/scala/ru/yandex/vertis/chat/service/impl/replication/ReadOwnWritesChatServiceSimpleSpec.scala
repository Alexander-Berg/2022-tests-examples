package ru.yandex.vertis.chat.service.impl.replication

import ru.yandex.vertis.chat.model.ModelGenerators.anyParticipant
import ru.yandex.vertis.chat.model.Window
import ru.yandex.vertis.chat.service.ServiceGenerators._
import ru.yandex.vertis.chat.service.impl.jvm.JvmChatService
import ru.yandex.vertis.chat.service.{ChatService, ChatServiceSpecBase}

/**
  * Checks whether [[ReadOwnWritesChatService]] doesn't
  * violate [[ChatService]] behaviour.
  *
  * @author dimas
  */
class ReadOwnWritesChatServiceSimpleSpec extends ChatServiceSpecBase {

  val service: ChatService =
    new JvmChatService with ReadOwnWritesChatService

  "ReadOwnWritesChatServiceSimpleSpec" should {
    "not duplicate message on markSpam" in {
      val room = createAndCheckRoom()
      val user1 = anyParticipant(room).next
      val sendMessageResults = withUserContext(user1) { implicit rc =>
        (1 to 3).map { _ =>
          val messageParameters = sendMessageParameters(room).next.copy(author = user1)
          service.sendMessage(messageParameters).futureValue
        }
      }
      val sendMessageResult1 = sendMessageResults.init.last
      val sendMessageResult2 = sendMessageResults.last
      withUserContext(user1) { implicit rc =>
        service.markSpam(sendMessageResult1.message.id, value = true).futureValue
        service.markSpam(sendMessageResult2.message.id, value = true).futureValue
      }
      val messages = withUserContext(user1) { implicit rc =>
        val window = Window(None, 10, asc = true)
        service.getMessages(room.id, window).futureValue.toIndexedSeq
      }
      messages should have size 3
    }
  }
}
