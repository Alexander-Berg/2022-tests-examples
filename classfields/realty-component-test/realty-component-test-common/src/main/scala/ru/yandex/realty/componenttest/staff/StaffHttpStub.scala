package ru.yandex.realty.componenttest.staff

import akka.http.scaladsl.model.StatusCodes
import play.api.libs.json.Json
import ru.yandex.realty.clients.staff.PersonsResponse
import ru.yandex.realty.clients.telegram.model.{Chat, Message, Response}
import ru.yandex.realty.componenttest.http.ExternalHttpStub
import ru.yandex.realty.componenttest.json.JsonSupport

trait StaffHttpStub extends JsonSupport {
  self: ExternalHttpStub =>

  def stubStaffSearchByAccount(): Unit = {
    val response = PersonsResponse(
      result = Seq.empty
    )
    stubGetResponse(
      "/staff/v3/persons.*",
      StatusCodes.OK.intValue,
      Json.stringify(Json.toJson(response))
    )
  }

  def staffSuccessResponse(messageId: Int, chatId: Long): Response[Message] =
    Response(
      ok = true,
      errorCode = None,
      description = None,
      result = Some(
        Message(
          messageId = messageId,
          date = 1000,
          chat = Chat(
            id = chatId,
            `type` = "Chat"
          )
        )
      )
    )

}
