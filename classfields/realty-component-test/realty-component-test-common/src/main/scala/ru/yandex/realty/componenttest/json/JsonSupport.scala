package ru.yandex.realty.componenttest.json

import play.api.libs.json.Json
import ru.yandex.realty.clients.telegram.model.{Message, Response, SendMessage}

trait JsonSupport {

  def parseSendMessageFromJsonString(message: String): SendMessage =
    Json.parse(message).as[SendMessage]

  def toJsonString(response: Response[Message]): String =
    Json.stringify(Json.toJson(response))

}
