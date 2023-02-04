package ru.yandex.vertis.chat.service.impl.distributed

import ru.yandex.vertis.chat.model.ModelGenerators.anyParticipant
import ru.yandex.vertis.chat.model.{ModelGenerators, Window}
import ru.yandex.vertis.chat.service.ServiceGenerators.{sendMessageParameters, _}

/**
  * Runnable specs on [[RemoteHttpChatService]]
  * along ten HTTP Chat API servers.
  *
  * @author dimas
  */
class RemoteHttp10ChatServiceSpec extends RemoteHttpChatServiceSpecBase {

  def numberOfInstances: Int = 10

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

    "not create duplicate message with already existing idempotencyKey" in {
      val idempotencyKey = Some(ModelGenerators.idempotencyKey.next)

      val room = createAndCheckRoom()
      val userId1 = room.users.head
      val userId2 = room.users.tail.head

      val defaultMessageParameters = sendMessageParameters(room).next

      val sendMessageResult1 = withUserContext(userId1, idempotencyKey) { implicit rc =>
        val messageParameters = defaultMessageParameters.copy(author = userId1)
        service.sendMessage(messageParameters).futureValue
      }

      val sendMessageResult2 = withUserContext(userId2, idempotencyKey) { implicit rc =>
        val messageParameters = defaultMessageParameters.copy(author = userId2)
        service.sendMessage(messageParameters).futureValue
      }

      sendMessageResult1.message.id shouldBe sendMessageResult2.message.id
    }
  }
}
