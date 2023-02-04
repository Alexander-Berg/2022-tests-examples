package ru.auto.chatbot.lifecycle.sync

import org.mockito.Mockito.{verify, verifyZeroInteractions}
import ru.auto.chatbot.lifecycle.Events.{Cancel, GoingToCheckup, SendMeCheckupReport, TimeOut}
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.MessageCode.LINK_SHARE
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{CHECKUP_REPORT_AGREE_AWAIT, CHECKUP_REPORT_GENERATION_ASYNC, IDLE}

class CheckupReportAgreeAwaitTest extends MessageProcessorSuit {

  test("CHECKUP_REPORT_AGREE_AWAIT event GoingToCheckup") {

    val state = State(step = CHECKUP_REPORT_AGREE_AWAIT)

    val res = fsm.transition(GoingToCheckup("xxx"), state).futureValue

    verify(chatManager).sendMessage(?, eq(LINK_SHARE), eq(Seq.empty))

    res.step shouldBe IDLE
    res.isAsync shouldBe false

  }

  test("CHECKUP_REPORT_AGREE_AWAIT event SendMeCheckupReport") {

    val state = State(step = CHECKUP_REPORT_AGREE_AWAIT)

    val res = fsm.transition(SendMeCheckupReport("xxx"), state).futureValue

    verifyZeroInteractions(chatManager)

    res.step shouldBe CHECKUP_REPORT_GENERATION_ASYNC
    res.isAsync shouldBe true

  }

  //делаем то же что и при GoingToCheckup
  test("CHECKUP_REPORT_AGREE_AWAIT event Cancel") {

    val state = State(step = CHECKUP_REPORT_AGREE_AWAIT)

    val res = fsm.transition(Cancel("xxx"), state).futureValue

    verify(chatManager).sendMessage(?, eq(LINK_SHARE), eq(Seq.empty))

    res.step shouldBe IDLE
    res.isAsync shouldBe false

  }

  test("CHECKUP_REPORT_AGREE_AWAIT event TimeOut") {

    val state = State(step = CHECKUP_REPORT_AGREE_AWAIT)

    val res = fsm.transition(TimeOut("xxx"), state).futureValue

    verifyZeroInteractions(chatManager)

    res.step shouldBe CHECKUP_REPORT_AGREE_AWAIT
  }
}
