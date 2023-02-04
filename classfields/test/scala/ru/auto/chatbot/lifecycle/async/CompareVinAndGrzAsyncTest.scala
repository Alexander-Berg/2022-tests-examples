package ru.auto.chatbot.lifecycle.async

import org.mockito.Mockito.{verify, verifyZeroInteractions}
import ru.auto.chatbot.lifecycle.Events.Ping
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.MessageCode.CHECK_RESULT_NO_CHANGE
import ru.auto.chatbot.model.ButtonCode.{NO, YES}
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{CHECK_MILEAGE_AWAIT, COMPARE_VIN_AND_GRZ_ASYNC, MILEAGE_AWAIT, VIN_HYPOTHESIS_ASYNC}

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-01.
  */
class CompareVinAndGrzAsyncTest extends MessageProcessorSuit {

  test("COMPARE_VIN_AND_GRZ_ASYNC similar vins") {
    val state = State(step = COMPARE_VIN_AND_GRZ_ASYNC, vinOffer = "VIN", vinByLicensePlate = "vin")
    val res = Await.result(fsm.transition(Ping(""), state), 10.seconds)

    res.step shouldBe CHECK_MILEAGE_AWAIT
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(CHECK_RESULT_NO_CHANGE), eq(Seq(YES, NO)))

  }

  test("COMPARE_VIN_AND_GRZ_ASYNC differrent vins") {
    val state = State(step = COMPARE_VIN_AND_GRZ_ASYNC, vinOffer = "vin1", vinByLicensePlate = "vin2")
    val res = Await.result(fsm.transition(Ping(""), state), 10.seconds)

    res.step shouldBe VIN_HYPOTHESIS_ASYNC
    res.isAsync shouldBe true

    verifyZeroInteractions(chatManager)

  }

  test("COMPARE_VIN_AND_GRZ_ASYNC similar grz") {
    val state = State(step = COMPARE_VIN_AND_GRZ_ASYNC, licensePlateOffer = "GRZ", licensePlateRecognized = "grz")
    val res = Await.result(fsm.transition(Ping(""), state), 10.seconds)

    res.step shouldBe CHECK_MILEAGE_AWAIT
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(CHECK_RESULT_NO_CHANGE), eq(Seq(YES, NO)))

  }

  test("COMPARE_VIN_AND_GRZ_ASYNC different grz") {
    val state = State(step = COMPARE_VIN_AND_GRZ_ASYNC, licensePlateOffer = "grz1", licensePlateRecognized = "grz2")
    val res = Await.result(fsm.transition(Ping(""), state), 10.seconds)

    res.step shouldBe VIN_HYPOTHESIS_ASYNC
    res.isAsync shouldBe true

    verifyZeroInteractions(chatManager)

  }

}
