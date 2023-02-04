package ru.auto.chatbot.utils

import org.scalatest.FunSuite
import ru.auto.chatbot.app.TestContext
import ru.auto.chatbot.lifecycle.Events.{Back, Cancel, Msg}
import ru.yandex.vertis.chat.model.api.ApiModel.{Message, MessagePayload}

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-05-15.
  */
class IncomingMessageUtilsTest extends FunSuite {

  import TestContext._

  private val incomingMessageUtils = new IncomingMessageUtils(bunkerButtons, "")

  test("cancel button to event") {
    val payload1 = MessagePayload.newBuilder().setValue("Завершить осмотр")
    val message1 = Message
      .newBuilder()
      .setId("1")
      .setPayload(payload1)
      .build()

    assert(incomingMessageUtils.toEvent(message1) == Cancel("1"))

    val payload2 = MessagePayload.newBuilder().setValue("С начала")
    val message2 = Message
      .newBuilder()
      .setId("2")
      .setPayload(payload2)
      .build()

    assert(incomingMessageUtils.toEvent(message2) == Cancel("2"))

    val payload3 = MessagePayload.newBuilder().setValue("Горшочек не вари")
    val message3 = Message
      .newBuilder()
      .setId("3")
      .setPayload(payload3)
      .build()

    assert(incomingMessageUtils.toEvent(message3) == Msg("3", "горшочек не вари"))
  }

  test("back button to event") {
    val payload1 = MessagePayload.newBuilder().setValue("Назад")
    val message1 = Message
      .newBuilder()
      .setId("1")
      .setPayload(payload1)
      .build()

    assert(incomingMessageUtils.toEvent(message1) == Back("1"))

    val payload2 = MessagePayload.newBuilder().setValue("Отмена")
    val message2 = Message
      .newBuilder()
      .setId("2")
      .setPayload(payload2)
      .build()

    assert(incomingMessageUtils.toEvent(message2) == Back("2"))
  }

}
