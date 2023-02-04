package ru.yandex.realty.componenttest.telegram

import akka.http.scaladsl.model.StatusCodes
import ru.yandex.realty.componenttest.http.ExternalHttpStub
import ru.yandex.vertis.generators.ProducerProvider._

trait TelegramHttpStub extends TelegramSupport {
  self: ExternalHttpStub =>

  protected def stubTelegramSendMessage(
    chatId: Long,
    expectedMessageText: Option[String] = None
  ): Unit = {
    val response = telegramSuccessResponse(telegramMessageIdGen.next, chatId)
    stubPostJsonResponse(
      "/telegram/bot\\S+/sendMessage",
      StatusCodes.OK.intValue,
      response,
      requestMatcher = Some(matchSendMessage(chatId, expectedMessageText))
    )
  }

  protected def stubTelegramEditMessage(
    chatId: Long,
    expectedMessageText: Option[String] = None
  ): Unit = {
    val response = telegramSuccessResponse(telegramMessageIdGen.next, chatId)

    stubPostJsonResponse(
      "/telegram/bot\\S+/editMessageText",
      StatusCodes.OK.intValue,
      response,
      requestMatcher = Some(matchSendMessage(chatId, expectedMessageText))
    )
  }

}
