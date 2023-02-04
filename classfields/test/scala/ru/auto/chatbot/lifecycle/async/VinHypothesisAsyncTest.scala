package ru.auto.chatbot.lifecycle.async

import org.mockito.Mockito.verify
import ru.auto.chatbot.lifecycle.Events.{Ping, TimeOut}
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.lifecycle.step.async.VinHypothesisAsync
import ru.auto.chatbot.model.ButtonCode.{NO, YES}
import ru.auto.chatbot.model.HypothesisResult
import ru.auto.chatbot.model.MessageCode.{CHECK_RESULT_FRAUD, GRZ_MARK_MODEL_MISMATCH}
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{CHECK_MILEAGE_AWAIT, OFFER_AWAIT, VIN_HYPOTHESIS_ASYNC}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-01.
  */
class VinHypothesisAsyncTest extends MessageProcessorSuit {

  test("VIN_HYPOTHESIS_ASYNC StolenVinResult") {
    val hypothesisResult = HypothesisResult(VinHypothesisAsync.StolenVinResult)
    when(vinDecoderClient.getHypothesis(?, ?, ?)).thenReturn(Future.successful(hypothesisResult))

    val state = State(step = VIN_HYPOTHESIS_ASYNC)

    val res = Await.result(fsm.transition(Ping(""), state), 10.seconds)

    res.step shouldBe OFFER_AWAIT
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(GRZ_MARK_MODEL_MISMATCH), eq(Seq()))
    verify(vinDecoderClient).getHypothesis(?, ?, ?)

  }

  test("VIN_HYPOTHESIS_ASYNC vin_lp ok result") {
    val hypothesisResult = HypothesisResult(VinHypothesisAsync.OkResult)
    when(vinDecoderClient.getHypothesis(?, ?, ?)).thenReturn(Future.successful(hypothesisResult))

    val state = State(
      step = VIN_HYPOTHESIS_ASYNC,
      vinByLicensePlate = "vin_lp",
      offerMarkCode = "mark_code",
      offerModelCode = "model_code"
    )

    val res = Await.result(fsm.transition(Ping(""), state), 10.seconds)

    res.step shouldBe CHECK_MILEAGE_AWAIT
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(CHECK_RESULT_FRAUD), eq(Seq(YES, NO)))
    verify(vinDecoderClient).getHypothesis(eq("vin_lp"), eq("mark_code"), eq("model_code"))

  }

  test("VIN_HYPOTHESIS_ASYNC vin_user ok result") {
    val hypothesisResult = HypothesisResult(VinHypothesisAsync.OkResult)
    when(vinDecoderClient.getHypothesis(?, ?, ?)).thenReturn(Future.successful(hypothesisResult))

    val state = State(step = VIN_HYPOTHESIS_ASYNC, vinByLicensePlate = "vin_lp", vinUser = "vin_user")

    val res = Await.result(fsm.transition(Ping(""), state), 10.seconds)

    res.step shouldBe CHECK_MILEAGE_AWAIT
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(CHECK_RESULT_FRAUD), eq(Seq(YES, NO)))
    verify(vinDecoderClient).getHypothesis(eq("vin_user"), ?, ?)

  }

  test("VIN_HYPOTHESIS_ASYNC time out") {
    val hypothesisResult = HypothesisResult(VinHypothesisAsync.OkResult)
    when(vinDecoderClient.getHypothesis(?, ?, ?)).thenReturn(Future.successful(hypothesisResult))

    val state = State(step = VIN_HYPOTHESIS_ASYNC)

    val res = Await.result(fsm.transition(TimeOut(""), state), 10.seconds)

    res.step shouldBe CHECK_MILEAGE_AWAIT
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(CHECK_RESULT_FRAUD), eq(Seq(YES, NO)))
  }

}
