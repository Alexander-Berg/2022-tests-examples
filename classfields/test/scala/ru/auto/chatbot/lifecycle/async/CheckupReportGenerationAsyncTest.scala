package ru.auto.chatbot.lifecycle.async

import org.mockito.Mockito.verify
import ru.auto.chatbot.lifecycle.Events.Ping
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.ButtonCode.{NO, NO_THANK_YOU, YES}
import ru.auto.chatbot.model.MessageCode.{EMAIL_CHECK_NO, EMAIL_CHECK_YES}
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{CHECKUP_REPORT_GENERATION_ASYNC, EMAIL_APPROVE_AWAIT, EMAIL_TYPE_IN_AWAIT}

import scala.concurrent.Future

class CheckupReportGenerationAsyncTest extends MessageProcessorSuit {

  test("CHECKUP_REPORT_GENERATION_ASYNC with email") {

    val expectedReportUrl = "test://report_url1"

    when(reportManager.save(?)).thenReturn(Future.successful(expectedReportUrl))

    val state = State(step = CHECKUP_REPORT_GENERATION_ASYNC, userPassportEmail = "rrr@y.ru")
    val res = fsm.transition(Ping("xxx"), state).futureValue

    verify(chatManager).sendMessage(?, eq(EMAIL_CHECK_YES), eq(Seq(YES, NO, NO_THANK_YOU)))

    res.step shouldBe EMAIL_APPROVE_AWAIT
    res.reportHtmlUrl shouldBe expectedReportUrl
    res.isAsync shouldBe false
  }

  test("CHECKUP_REPORT_GENERATION_ASYNC with empty email") {

    val expectedReportUrl = "test://report_url2"

    when(reportManager.save(?)).thenReturn(Future.successful(expectedReportUrl))

    val state = State(step = CHECKUP_REPORT_GENERATION_ASYNC)
    val res = fsm.transition(Ping("xxx"), state).futureValue

    verify(chatManager).sendMessage(?, eq(EMAIL_CHECK_NO), eq(Seq(NO_THANK_YOU)))

    res.step shouldBe EMAIL_TYPE_IN_AWAIT
    res.reportHtmlUrl shouldBe expectedReportUrl
    res.isAsync shouldBe false

  }

}
