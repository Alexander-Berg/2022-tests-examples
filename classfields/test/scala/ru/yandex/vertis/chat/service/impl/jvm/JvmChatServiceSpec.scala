package ru.yandex.vertis.chat.service.impl.jvm

import org.scalatest.OptionValues
import ru.yandex.vertis.MimeType
import ru.yandex.vertis.chat.model.{MessagePayload, Participants}
import ru.yandex.vertis.chat.service.ServiceGenerators._
import ru.yandex.vertis.chat.service._
import ru.yandex.vertis.generators.BasicGenerators.readableString

/**
  * Runnable specs on [[JvmChatService]].
  *
  * @author dimas
  */
class JvmChatServiceSpec extends ChatServiceSpecBase with OptionValues {
  override val service: ChatService = new JvmChatService

  "JvmChatService" should {
    "correctly return mute value in getRoom and getRooms" in {
      val roomId = readableString.next
      val create = CreateRoomParameters(Some(roomId), Participants.fromUserIds("1", "2", "3"))
      service.createRoom(create).futureValue
      val muteUser = "2"

      service.mute(roomId, muteUser, mute = true)
      service.getRoom(roomId).futureValue.participants.find(muteUser).value.muted shouldBe true
      service.getRooms(muteUser).futureValue.head.participants.find(muteUser).value.muted shouldBe true
    }

    "block and unblock room by user" in {
      val roomId = readableString.next
      val create = CreateRoomParameters(Some(roomId), Participants.fromUserIds("4", "5", "6"))
      service.createRoom(create).futureValue
      val blockerUser = "5"

      service.blockRoom(roomId, blockerUser, value = true)
      service.getRoom(roomId).futureValue.participants.find(blockerUser).value.blocked shouldBe true
      val rooms = service.getRooms(blockerUser).futureValue
      (rooms should have).length(1)
      rooms.head.participants.find(blockerUser).value.blocked shouldBe true
    }

    "correctly sendMessage to roomLocator" in {
      val roomId = readableString.next
      val roomParameters = CreateRoomParameters(Some(roomId), Participants.fromUserIds("7", "8", "9"))
      val messageParameters = CreateMessageParameters(
        "",
        author = "7",
        payload = MessagePayload(MimeType.TEXT_PLAIN, "test"),
        attachments = Seq.empty,
        providedId = None,
        isSpam = false,
        roomLocator = Some(RoomLocator.Source(roomParameters))
      )
      val result1 = service.sendMessage(messageParameters).futureValue
      result1.previousMessage shouldBe empty
      val result2 = service.sendMessage(messageParameters).futureValue
      result2.previousMessage shouldBe Some(result1.message)
    }
  }
}
