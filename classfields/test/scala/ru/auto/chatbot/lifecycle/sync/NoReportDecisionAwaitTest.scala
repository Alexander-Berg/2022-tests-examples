package ru.auto.chatbot.lifecycle.sync

import org.mockito.Mockito.verify
import ru.auto.chatbot.lifecycle.Events.{No, Yes}
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.ButtonCode.{GOING_TO_CHECKUP, NO, YES}
import ru.auto.chatbot.model.MessageCode.{CHECK_MILEAGE, WELL_OKAY_THEN}
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{CHECK_MILEAGE_AWAIT, IDLE, NO_REPORT_DECISION_AWAIT}

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-01.
  */
class NoReportDecisionAwaitTest extends MessageProcessorSuit {

  test("NO_REPORT_DECISION_AWAIT no answer") {
    val state = State(step = NO_REPORT_DECISION_AWAIT)
    val res = Await.result(fsm.transition(No(""), state), 10.seconds)

    res.step shouldBe IDLE
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(WELL_OKAY_THEN), eq(Seq(GOING_TO_CHECKUP)))
  }

  test("NO_REPORT_DECISION_AWAIT yes answer") {
    val state = State(step = NO_REPORT_DECISION_AWAIT)
    val res = Await.result(fsm.transition(Yes(""), state), 10.seconds)

    res.step shouldBe CHECK_MILEAGE_AWAIT
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(CHECK_MILEAGE), eq(Seq(YES, NO)))
  }

}
