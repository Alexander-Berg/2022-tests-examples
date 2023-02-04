package ru.yandex.realty.componenttest.telegram

import ru.yandex.realty.clients.telegram.model.SendMessage
import ru.yandex.realty.logging.Logging

trait TelegramMessageTracker {

  def isMessageSent(message: SendMessage): Boolean =
    isMessageSent(_ == message)

  def isMessageSent(matcher: SendMessage => Boolean): Boolean

}

class DefaultTelegramMessageTracker extends TelegramMessageTracker with Logging {

  private val messages = scala.collection.mutable.ArrayBuffer.empty[SendMessage]

  def lastMessage: Option[SendMessage] =
    messages.lastOption

  def observe(message: SendMessage): Unit = {
    messages += message
    log.debug("Telegram message was received: message={}", message)
  }

  override def isMessageSent(matcher: SendMessage => Boolean): Boolean =
    messages.exists(matcher)

}
