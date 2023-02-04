package ru.auto.chatbot.lifecycle.async

import org.mockito.Mockito.{verify, verifyZeroInteractions}
import ru.auto.api.vin.ResponseModel.{VinDecoderError, VinResponse}
import ru.auto.chatbot.lifecycle.Events.{Ping, TimeOut}
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.MessageCode.{INVALID_GRZ, TIME_OUT_CHECK_YOURSELF}
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{COMPARE_VIN_AND_GRZ_ASYNC, VIN_BY_LICENSE_PLATE_ASYNC, VIN_TYPE_IN_AWAIT}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-01.
  */
class VinByLicensePlateAsyncTest extends MessageProcessorSuit {

  test("VIN_BY_LICENSE_PLATE_ASYNC ping event get vin") {
    val vinResponse = VinResponse
      .newBuilder()
      .setVin("vin")
      .build()

    when(vinDecoderClient.getVinByLicensePlate(?)).thenReturn(Future.successful(vinResponse))

    val res =
      Await.result(fsm.transition(Ping(""), State(step = VIN_BY_LICENSE_PLATE_ASYNC, isAsync = true)), 10.seconds)

    res.step shouldBe COMPARE_VIN_AND_GRZ_ASYNC
    res.isAsync shouldBe true
    res.vinByLicensePlate shouldBe vinResponse.getVin
    verifyZeroInteractions(chatManager)
    verify(vinDecoderClient).getVinByLicensePlate(?)
  }

  test("VIN_BY_LICENSE_PLATE_ASYNC ping event get in_progress") {
    val vinDecoderError = VinDecoderError
      .newBuilder()
      .setErrorCode(VinDecoderError.Code.IN_PROGRESS)
    val vinResponse = VinResponse
      .newBuilder()
      .setError(vinDecoderError)
      .build()

    when(vinDecoderClient.getVinByLicensePlate(?)).thenReturn(Future.successful(vinResponse))

    val res =
      Await.result(fsm.transition(Ping(""), State(step = VIN_BY_LICENSE_PLATE_ASYNC, isAsync = true)), 10.seconds)

    res.step shouldBe VIN_BY_LICENSE_PLATE_ASYNC
    res.isAsync shouldBe true
    verifyZeroInteractions(chatManager)
    verify(vinDecoderClient).getVinByLicensePlate(?)
  }

  test("VIN_BY_LICENSE_PLATE_ASYNC ping event get error") {
    val vinDecoderError = VinDecoderError
      .newBuilder()
      .setErrorCode(VinDecoderError.Code.LP_NOT_FOUND)
    val vinResponse = VinResponse
      .newBuilder()
      .setError(vinDecoderError)
      .build()

    when(vinDecoderClient.getVinByLicensePlate(?)).thenReturn(Future.successful(vinResponse))

    val state = State(step = VIN_BY_LICENSE_PLATE_ASYNC, isAsync = true)
    val res = Await.result(fsm.transition(Ping(""), state), 10.seconds)

    res.step shouldBe VIN_TYPE_IN_AWAIT
    res.isAsync shouldBe false
    verify(chatManager).sendMessage(?, eq(INVALID_GRZ), eq(Seq()))
    verify(vinDecoderClient).getVinByLicensePlate(?)
  }

  test("VIN_BY_LICENSE_PLATE_ASYNC TimeOut") {
    val state = Await.result(fsm.transition(TimeOut(""), State(step = VIN_BY_LICENSE_PLATE_ASYNC)), 10.seconds)
    state.step shouldBe VIN_TYPE_IN_AWAIT
    state.isAsync shouldBe false
    verify(chatManager).sendMessage(?, eq(TIME_OUT_CHECK_YOURSELF), eq(Seq()))
  }

  test("VIN_BY_LICENSE_PLATE_ASYNC ping event get exception") {

    when(vinDecoderClient.getVinByLicensePlate(?)).thenReturn(Future.failed(new IllegalArgumentException("")))

    val res =
      Await.result(fsm.transition(Ping(""), State(step = VIN_BY_LICENSE_PLATE_ASYNC, isAsync = true)), 10.seconds)

    res.step shouldBe VIN_BY_LICENSE_PLATE_ASYNC
    res.isAsync shouldBe true
    verifyZeroInteractions(chatManager)
    verify(vinDecoderClient).getVinByLicensePlate(?)
  }

}
