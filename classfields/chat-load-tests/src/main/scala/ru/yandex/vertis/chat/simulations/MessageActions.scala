package ru.yandex.vertis.chat.simulations

import com.google.protobuf.util.JsonFormat
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import ru.yandex.vertis.chat.service.ServiceGenerators.sendMessageParameters
import ru.yandex.vertis.chat.service.ServiceProtoFormats.CreateMessageParametersFormat

object MessageActions {

  val SendMessage =
    doIf(_.contains(RoomsKey)) {
      exec(
        http("send_message")
          .post("messages")
          .withUser()
          .withTrace()
          .body(StringBody(messageParameters()))
      )
    }

  val GetMessages =
    doIf(_.contains(RoomsKey)) {
      exec(
        http("get_messages")
          .get(s"messages?room_id=$RoomEl&count=10")
          .withUser()
          .withTrace()
          .body(StringBody(messageParameters()))
      )
    }

  val HasUnread = exec(
    http("has_unread")
      .get(s"messages/unread?user_id=$UserEl")
      .withUser()
      .withTrace()
  )

  val MarkRead =
    doIf(_.contains(RoomsKey)) {
      exec(
        http("mark_read")
          .delete(s"messages/unread?user_id=$UserEl&room_id=$RoomEl")
          .withUser()
          .withTrace()
      )
    }

  def messageParameters(): String = {
    val parameters = sendMessageParameters(RandomRoomEl, UserEl).next
    val message = CreateMessageParametersFormat.write(parameters)
    JsonFormat.printer().omittingInsignificantWhitespace().print(message)
  }
}
