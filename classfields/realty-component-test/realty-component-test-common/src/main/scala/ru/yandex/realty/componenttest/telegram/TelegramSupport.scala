package ru.yandex.realty.componenttest.telegram

import java.util.concurrent.atomic.AtomicLong

import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.matching.{MatchResult, ValueMatcher}
import junit.framework.Assert
import org.scalacheck.Gen
import play.api.libs.json.{Json, OFormat, Reads}
import ru.yandex.realty.clients.telegram.model.{Chat, Message, Response, SendMessage, User}
import ru.yandex.realty.componenttest.json.JsonSupport

import scala.util.control.NonFatal

trait TelegramSupport extends JsonSupport {

  private val telegramMessageTracker = new DefaultTelegramMessageTracker

  val telegramMessageIdGen: Gen[Int] =
    Gen.posNum[Int]

  val telegramChatIdGen: Gen[Long] =
    Gen.resultOf(uniqueTelegramChatId _)

  val nextTelegramChatId: AtomicLong =
    new AtomicLong(15000)

  def uniqueTelegramChatId(v: AnyVal): Long =
    nextTelegramChatId.incrementAndGet()

  def matchSendMessage(chatId: Long, expectedText: Option[String]): ValueMatcher[Request] = (request: Request) => {
    val message = parseSendMessageFromJsonString(request.getBodyAsString)
    observeSendMessage(message)
    MatchResult.of(
      chatId.toString == message.chatId &&
        expectedText.forall(_ == message.text)
    )
  }

  def telegramSuccessResponse(messageId: Int, chatId: Long): Response[Message] =
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

  def observeSendMessage(message: SendMessage): Unit =
    telegramMessageTracker.observe(message)

  protected def checkTelegramMessageSent(message: => SendMessage): Unit =
    try {
      Assert.assertTrue(telegramMessageTracker.isMessageSent(message))
    } catch {
      case NonFatal(_) =>
        telegramMessageTracker.lastMessage
          .map(Assert.assertEquals(message, _))
          .getOrElse(Assert.fail("No messages were sent"))
    }

  protected def checkNoTelegramMessageSent(messageMatcher: SendMessage => Boolean): Unit = {
    Assert.assertFalse(telegramMessageTracker.isMessageSent(messageMatcher))
  }

  implicit val SendMessageReads: Reads[SendMessage] = SendMessage.SendMessageFormat
  implicit val ChatFormat: OFormat[Chat] = Json.format[Chat]
  implicit val UserFormat: OFormat[User] = Json.format[User]

}
