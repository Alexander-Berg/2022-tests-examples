package ru.auto.chatbot.lifecycle.async

import org.mockito.Mockito.verify
import ru.auto.api.ResponseModel.RawVinReportResponse
import ru.auto.api.vin.VinReportModel.{ContentBlock, RawVinReport}
import ru.auto.chatbot.lifecycle.Events.Ping
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.MessageCode.{CHECK_RESULT_CHANGES, CHECK_RESULT_NO_CHANGE}
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{MILEAGE_AWAIT, RELOADED_VIN_REPORT_ASYNC}
import ru.auto.api.vin.vin_report_model.{RawVinReport => ScalaRawVinReport}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-01.
  */
class ReloadedVinReportAsyncTest extends MessageProcessorSuit {

  test("RELOADED_VIN_REPORT_ASYNC similar reports") {
    val item = ContentBlock.ContentItem.newBuilder().setType(ContentBlock.ContentItemType.PTS).build()
    val content = ContentBlock.newBuilder().addItems(item).build()
    val report = RawVinReport.newBuilder().setContent(content).build()
    val reportResponse = RawVinReportResponse.newBuilder().setReport(report).build()

    when(publicApiClient.getVinReport(?)).thenReturn(Future.successful(reportResponse))

    val state =
      State(step = RELOADED_VIN_REPORT_ASYNC, offerVinReport = Some(ScalaRawVinReport.fromJavaProto(report)))

    val res = Await.result(fsm.transition(Ping(""), state), 10.seconds)

    res.step shouldBe MILEAGE_AWAIT
    res.isAsync shouldBe false
    verify(chatManager).sendMessage(?, eq(CHECK_RESULT_NO_CHANGE), eq(Seq()))

  }

  test("RELOADED_VIN_REPORT_ASYNC different reports") {
    val ptsItem = ContentBlock.ContentItem.newBuilder().setType(ContentBlock.ContentItemType.PTS).build()
    val legalItem = ContentBlock.ContentItem.newBuilder().setType(ContentBlock.ContentItemType.LEGAL).build()
    val contentResponse = ContentBlock.newBuilder().addItems(ptsItem).addItems(legalItem).build()
    val contentOffer = ContentBlock.newBuilder().addItems(ptsItem).build()
    val reportResponse = RawVinReport.newBuilder().setContent(contentResponse).build()
    val reportOffer = RawVinReport.newBuilder().setContent(contentOffer).build()
    val report = RawVinReportResponse.newBuilder().setReport(reportResponse).build()

    when(publicApiClient.getVinReport(?)).thenReturn(Future.successful(report))
    val state = State(
      step = RELOADED_VIN_REPORT_ASYNC,
      offerVinReport = Some(ScalaRawVinReport.fromJavaProto(reportOffer))
    )

    val res = Await.result(fsm.transition(Ping(""), state), 10.seconds)

    res.step shouldBe MILEAGE_AWAIT
    res.isAsync shouldBe false
    verify(chatManager).sendMessage(?, eq(CHECK_RESULT_CHANGES), eq(Seq()))
  }

}
