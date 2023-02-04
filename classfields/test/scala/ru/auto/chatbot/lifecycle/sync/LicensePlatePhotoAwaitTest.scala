package ru.auto.chatbot.lifecycle.sync

import org.mockito.Mockito.verify
import ru.auto.chatbot.lifecycle.Events.{CanNotDoPhoto, ImageEvent, Msg, TimeOut}
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.ButtonCode.{CANCEL, CONTINUE, NO, YES}
import ru.auto.chatbot.model.MessageCode.{BASIC_ERROR_MESSAGE, CHECK_RUN, NO_REPORT_SORRY, TIME_OUT}
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{LICENCE_PLATE_PHOTO_AWAIT, LICENSE_PLATE_RECOGNITION_ASYNC, LICENSE_PLATE_TYPE_IN_AWAIT, NO_REPORT_DECISION_AWAIT, TIME_OUT_CONTINUE_AWAIT, VIN_BY_LICENSE_PLATE_ASYNC}

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-01.
  */
class LicensePlatePhotoAwaitTest extends MessageProcessorSuit {

  test("LICENCE_PLATE_PHOTO_AWAIT cant do photo ") {
    val state = State(step = LICENCE_PLATE_PHOTO_AWAIT)
    val res = Await.result(fsm.transition(CanNotDoPhoto(""), state), 10.seconds)

    res.step shouldBe NO_REPORT_DECISION_AWAIT
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(NO_REPORT_SORRY), eq(Seq(YES, NO)))
  }

  test("LICENCE_PLATE_PHOTO_AWAIT TimeOut") {
    val state = State(step = LICENCE_PLATE_PHOTO_AWAIT)
    val res = Await.result(fsm.transition(TimeOut(""), state), 10.seconds)

    res.step shouldBe TIME_OUT_CONTINUE_AWAIT
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(TIME_OUT), eq(Seq(CONTINUE, CANCEL)))
  }

  test("LICENCE_PLATE_PHOTO_AWAIT send photo") {
    val state = State(step = LICENCE_PLATE_PHOTO_AWAIT)
    val res = Await.result(fsm.transition(ImageEvent("", "url"), state), 10.seconds)

    res.step shouldBe LICENSE_PLATE_RECOGNITION_ASYNC
    res.isAsync shouldBe true

    verify(chatManager).sendMessage(?, eq(CHECK_RUN), ?)
  }

  test("LICENCE_PLATE_PHOTO_AWAIT msg licensePlateRecognitionAttempts = 1") {
    val state = State(step = LICENCE_PLATE_PHOTO_AWAIT, licensePlateRecognitionAttempts = 1)
    val res = Await.result(fsm.transition(Msg("", "м386мн799"), state), 10.seconds)

    res.isAsync shouldBe true
    res.licensePlateUser shouldBe "М386МН799"
    res.step shouldBe VIN_BY_LICENSE_PLATE_ASYNC

    verify(chatManager).sendMessage(?, eq(CHECK_RUN), ?)
  }

  test("LICENCE_PLATE_PHOTO_AWAIT msg licensePlateRecognitionAttempts = 0") {
    val state = State(step = LICENCE_PLATE_PHOTO_AWAIT)
    val res = Await.result(fsm.transition(Msg("", "м386мн799"), state), 10.seconds)

    res.isAsync shouldBe false
    res.licensePlateUser shouldBe ""
    res.step shouldBe LICENCE_PLATE_PHOTO_AWAIT

    verify(chatManager).sendMessage(?, eq(BASIC_ERROR_MESSAGE), ?)
  }
}
