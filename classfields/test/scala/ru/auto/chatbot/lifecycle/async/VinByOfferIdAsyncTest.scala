package ru.auto.chatbot.lifecycle.async

import org.mockito.Mockito.verify
import ru.auto.api.ResponseModel.RawVinReportResponse
import ru.auto.api.vin.VinReportModel.RawVinReport
import ru.auto.chatbot.lifecycle.Events.Ping
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.ButtonCode.I_AM_HERE
import ru.auto.chatbot.model.MessageCode.RECOMMENDATIONS_BEFORE_REVIEW
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{ON_PLACE_AWAIT, VIN_BY_OFFER_ID_ASYNC}
import ru.auto.api.vin.vin_report_model.{RawVinReport => ScalaRawVinReport}

import scala.concurrent.Future

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-01.
  */
class VinByOfferIdAsyncTest extends MessageProcessorSuit {

  test("VIN_BY_OFFER_ID_ASYNC get vin report") {
    val report = RawVinReport
      .newBuilder()
      .setVin("vin")
      .build()
    val vinReportResponse = RawVinReportResponse
      .newBuilder()
      .setReport(report)
      .build()
    when(publicApiClient.getVinReport(?)).thenReturn(Future.successful(vinReportResponse))
    val state = State(step = VIN_BY_OFFER_ID_ASYNC)
    val res = fsm.transition(Ping(""), state).futureValue

    res.offerVinReport.get shouldBe ScalaRawVinReport.fromJavaProto(report)
    res.step shouldBe ON_PLACE_AWAIT

    verify(chatManager).sendMessage(?, eq(RECOMMENDATIONS_BEFORE_REVIEW), eq(Seq(I_AM_HERE)))
  }

}
