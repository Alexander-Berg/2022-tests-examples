package ru.auto.chatbot.lifecycle.sync

import org.mockito.Mockito.{verify, verifyZeroInteractions}
import ru.auto.chatbot.lifecycle.Events.{Cancel, Continue, TimeOut}
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.ButtonCode.GOING_TO_CHECKUP
import ru.auto.chatbot.model.MessageCode.WELL_OKAY_THEN
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{IDLE, OFFER_AWAIT, TIME_OUT_CONTINUE_AWAIT}
import ru.yandex.vertis.chat.model.api.api_model.{CreateMessageParameters, MessagePayload, MessageProperties, MessagePropertyType}
import ru.yandex.vertis.mime.MimeType

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-02.
  */
class TimeOutContinueAwaitTest extends MessageProcessorSuit {

  test("TIME_OUT_CONTINUE_AWAIT continue") {
    val messageRequest = {
      val payload = MessagePayload(contentType = MimeType.TEXT_HTML, value = "test text")

      val propertyType = MessagePropertyType.MESSAGE_PROPERTY_TYPE_UNKNOWN

      val properties = MessageProperties(`type` = propertyType)

      CreateMessageParameters(
        roomId = "test_room",
        userId = "test_bot",
        payload = Some(payload),
        properties = Some(properties)
      )
    }
    when(chatManager.sendMessageByParameters(?)).thenReturn(Future.successful(messageRequest))

    val state =
      State(step = TIME_OUT_CONTINUE_AWAIT, previousMessage = Some(messageRequest), timeOutedStep = OFFER_AWAIT)
    val res = Await.result(fsm.transition(Continue(""), state), 10.seconds)

    res.step shouldBe OFFER_AWAIT
    res.isAsync shouldBe false

    verify(chatManager).sendMessageByParameters(messageRequest)

  }

  test("TIME_OUT_CONTINUE_AWAIT cancel") {

    when(chatManager.sendMessage(?, ?, ?)).thenReturn(Future.successful(sendMessageResponse))

    val state = State(step = TIME_OUT_CONTINUE_AWAIT, timeOutedStep = OFFER_AWAIT)
    val res = Await.result(fsm.transition(Cancel(""), state), 10.seconds)

    res.step shouldBe IDLE
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(WELL_OKAY_THEN), eq(Seq(GOING_TO_CHECKUP)))
  }

  test("TIME_OUT_CONTINUE_AWAIT time out") {
    val messageRequest = {
      val payload = MessagePayload(contentType = MimeType.TEXT_HTML, value = "test text")

      val propertyType = MessagePropertyType.MESSAGE_PROPERTY_TYPE_UNKNOWN

      val properties = MessageProperties(`type` = propertyType)

      CreateMessageParameters(
        roomId = "test_room",
        userId = "test_bot",
        payload = Some(payload),
        properties = Some(properties)
      )
    }
    when(chatManager.sendMessageByParameters(?)).thenReturn(Future.successful(messageRequest))

    val state =
      State(step = TIME_OUT_CONTINUE_AWAIT, previousMessage = Some(messageRequest), timeOutedStep = OFFER_AWAIT)
    val res = Await.result(fsm.transition(TimeOut(""), state), 10.seconds)

    res.step shouldBe TIME_OUT_CONTINUE_AWAIT
    res.isAsync shouldBe false
    res.previousMessage shouldBe Some(messageRequest)
    res.timeOutedStep shouldBe OFFER_AWAIT

    verifyZeroInteractions(chatManager)
  }
}
