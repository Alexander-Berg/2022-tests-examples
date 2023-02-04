package ru.auto.chatbot.lifecycle.async

import org.mockito.Mockito.{verify, verifyZeroInteractions}
import ru.auto.chatbot.lifecycle.Events.Ping
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{RELOADED_VIN_REPORT_ASYNC, VIN_RELOAD_ASYNC}
import ru.auto.api.vin.vin_report_model.{RawVinReport => ScalaRawVinReport}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-01.
  */
class VinReloadAsyncTest extends MessageProcessorSuit {

  test("VIN_RELOAD_ASYNC ping first interaction") {
    val report = ScalaRawVinReport(vin = "test")
    val state = State(step = VIN_RELOAD_ASYNC, isAsync = true, offerVinReport = Some(report))
    when(vinDecoderClient.reload(?)).thenReturn(Future.unit)
    val res = Await.result(fsm.transition(Ping(""), state), 10.seconds)

    res.step shouldBe VIN_RELOAD_ASYNC
    res.isAsync shouldBe true
    verify(vinDecoderClient).reload(?)
  }

  test("VIN_RELOAD_ASYNC ping less 3 minutes") {
    val report = ScalaRawVinReport(vin = "test")
    val vinReloadedTime = (System.currentTimeMillis().millis - 2.minutes).toMillis
    val state = State(
      step = VIN_RELOAD_ASYNC,
      isAsync = true,
      offerVinReport = Some(report),
      vinReloadedTime = vinReloadedTime
    )
    val res = Await.result(fsm.transition(Ping(""), state), 10.seconds)

    res.step shouldBe VIN_RELOAD_ASYNC
    res.isAsync shouldBe true
    verifyZeroInteractions(vinDecoderClient)
  }

  test("VIN_RELOAD_ASYNC ping more 3 minutes") {
    val report = ScalaRawVinReport(vin = "test")
    val vinReloadedTime = (System.currentTimeMillis().millis - 4.minutes).toMillis
    val state = State(
      step = VIN_RELOAD_ASYNC,
      isAsync = true,
      offerVinReport = Some(report),
      vinReloadedTime = vinReloadedTime
    )
    val res = Await.result(fsm.transition(Ping(""), state), 10.seconds)

    res.step shouldBe RELOADED_VIN_REPORT_ASYNC
    res.isAsync shouldBe true
    verifyZeroInteractions(vinDecoderClient)
  }

}
