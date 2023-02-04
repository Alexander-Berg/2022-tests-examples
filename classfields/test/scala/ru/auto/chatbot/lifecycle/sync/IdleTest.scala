package ru.auto.chatbot.lifecycle.sync

import org.mockito.Mockito.{times, verify, verifyZeroInteractions}
import ru.auto.chatbot.lifecycle.Events.{GoingToCheckup, IamHere, Msg, TimeOut}
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.ButtonCode.GOING_TO_CHECKUP
import ru.auto.chatbot.model.MessageCode.{BASIC_ERROR_MESSAGE, LINK_SHARE, START_MESSAGE}
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{IDLE, OFFER_AWAIT}

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-01.
  */
class IdleTest extends MessageProcessorSuit {

  test("IDLE Begin") {
    val state = State(step = IDLE, roomId = "1")
    val res = fsm.transition(GoingToCheckup("10"), state).futureValue

    res.step shouldBe OFFER_AWAIT
    res.previousStep shouldBe IDLE
    res.previousMessageCode shouldBe messageCode

    verify(chatManager, times(1))
      .sendMessage(?, eq(LINK_SHARE), ?)
  }

  test("IDLE msg") {
    val state = State(step = IDLE, roomId = "1")
    val res = Await.result(fsm.transition(Msg("10", ""), state), 10.seconds)
    res.step shouldBe IDLE
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(START_MESSAGE), eq(Seq(GOING_TO_CHECKUP)))
  }

  test("IDLE iamhere") {
    val state = State(step = IDLE)
    val res = Await.result(fsm.transition(IamHere("10"), state), 10.seconds)
    res.step shouldBe IDLE
    verify(chatManager).sendMessage(?, eq(BASIC_ERROR_MESSAGE), ?)
  }

  test("IDLE TimeOut") {
    val state = Await.result(fsm.transition(TimeOut(""), State(step = IDLE)), 10.seconds)
    state.step shouldBe IDLE
    verifyZeroInteractions(chatManager)
  }
}
