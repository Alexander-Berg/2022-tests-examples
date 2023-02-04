package ru.yandex.vertis.chat.components.dao.chat.chatbot

import org.scalatest.FunSuite
import ru.yandex.vertis.chat.common.chatbot.ChatBotUtils
import ru.yandex.vertis.chat.model.ModelGenerators._
import ru.yandex.vertis.chat.service.ServiceGenerators._
import ru.yandex.vertis.chat.common.chatbot.ChatBotUtils.RichCreateMessageParameters
import ru.yandex.vertis.chat.components.clients.pushnoy.DeviceInfo
import ru.yandex.vertis.chat.util.test.RequestContextAware

/**
  * TODO
  *
  * @author aborunov
  */
class ChatBotUtilsSpec extends FunSuite with RequestContextAware {
  test("ensureCorrectMessageToChatBot") {
    val user = userId.next
    val room = ChatBotUtils.roomId(user)
    val messageParams = sendMessageParameters(room, user).next

    withUserContextFromPlatform(user, Some("ios")) { implicit rc =>
      val updatedMessageParams =
        messageParams.ensureCorrectMessageToChatBot(Some(DeviceInfo(Some("ios"), Some("9.0.0.8970"))), rc)
      assert(updatedMessageParams.properties.getUserAppVersion == "IOS 9.0.0.8970")
    }

    withUserContextFromPlatform(user, None) { implicit rc =>
      val updatedMessageParams = messageParams.ensureCorrectMessageToChatBot(None, rc)
      assert(updatedMessageParams.properties.getUserAppVersion == "N/A")
    }
  }
}
