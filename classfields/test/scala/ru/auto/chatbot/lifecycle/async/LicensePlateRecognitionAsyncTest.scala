package ru.auto.chatbot.lifecycle.async

import org.mockito.Mockito.{verify, verifyZeroInteractions}
import ru.auto.chatbot.basic_model.RecognizedNumber
import ru.auto.chatbot.lifecycle.Events.{Msg, Ping, TimeOut}
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.ButtonCode.{NO, YES}
import ru.auto.chatbot.model.MessageCode.{BAD_PHOTO, BASIC_ERROR_MESSAGE, GRZ_DOUBLECHECK, GRZ_MANUAL_TYPE_IN}
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{LICENCE_PLATE_PHOTO_AWAIT, LICENSE_PLATE_RECOGNITION_ASYNC, LICENSE_PLATE_TYPE_IN_AWAIT, RECOGNIZED_LICENSE_PLATE_APPROVE_AWAIT}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-01.
  */
class LicensePlateRecognitionAsyncTest extends MessageProcessorSuit {

  test("LICENSE_PLATE_RECOGNITION_ASYNC ping 1st bad photo") {

    when(yavisionManager.recognizeLicensePlate(?)).thenReturn(Future.successful(RecognizedNumber()))

    val state = State(step = LICENSE_PLATE_RECOGNITION_ASYNC)
    val res = Await.result(fsm.transition(Ping(""), state), 10.seconds)

    res.step shouldBe LICENCE_PLATE_PHOTO_AWAIT
    res.licensePlateRecognitionAttempts shouldBe 1
    res.isAsync shouldBe false

    verify(yavisionManager).recognizeLicensePlate(?)
    verify(chatManager).sendMessage(?, eq(BAD_PHOTO), eq(Seq()))

  }

  test("LICENSE_PLATE_RECOGNITION_ASYNC ping 2nd bad photo") {

    when(yavisionManager.recognizeLicensePlate(?)).thenReturn(Future.successful(RecognizedNumber()))

    val state = State(step = LICENSE_PLATE_RECOGNITION_ASYNC, licensePlateRecognitionAttempts = 1)
    val res = Await.result(fsm.transition(Ping(""), state), 10.seconds)

    res.step shouldBe LICENSE_PLATE_TYPE_IN_AWAIT
    res.licensePlateRecognitionAttempts shouldBe 2
    res.isAsync shouldBe false

    verify(yavisionManager).recognizeLicensePlate(?)
    verify(chatManager).sendMessage(?, eq(GRZ_MANUAL_TYPE_IN), eq(Seq()))
  }

  test("LICENSE_PLATE_RECOGNITION_ASYNC ping recognized license plate") {

    when(yavisionManager.recognizeLicensePlate(?)).thenReturn(Future.successful(RecognizedNumber(number = "test1")))
    val res = Await.result(
      fsm
        .transition(Ping(""), State(step = LICENSE_PLATE_RECOGNITION_ASYNC, licensePlateOffer = "test")),
      10.seconds
    )

    res.step shouldBe RECOGNIZED_LICENSE_PLATE_APPROVE_AWAIT
    res.isAsync shouldBe false

    verify(yavisionManager).recognizeLicensePlate(?)
    verify(chatManager).sendMessage(?, eq(GRZ_DOUBLECHECK), eq(Seq(YES, NO)))

  }

  test("LICENSE_PLATE_RECOGNITION_ASYNC not ping event") {

    val state = State(step = LICENSE_PLATE_RECOGNITION_ASYNC, isAsync = true)
    val res = Await.result(fsm.transition(Msg("", ""), state), 10.seconds)

    res.step shouldBe LICENSE_PLATE_RECOGNITION_ASYNC
    res.isAsync shouldBe true

    verifyZeroInteractions(yavisionManager)
    verify(chatManager).sendMessage(?, eq(BASIC_ERROR_MESSAGE), eq(Seq()))

  }

  test("LICENSE_PLATE_RECOGNITION_ASYNC time out event") {

    val state = State(step = LICENSE_PLATE_RECOGNITION_ASYNC, isAsync = true)
    val res = Await.result(fsm.transition(TimeOut(""), state), 10.seconds)

    res.step shouldBe LICENSE_PLATE_TYPE_IN_AWAIT
    res.isAsync shouldBe false

    verifyZeroInteractions(yavisionManager)
    verify(chatManager).sendMessage(?, eq(GRZ_MANUAL_TYPE_IN), eq(Seq()))

  }

}
