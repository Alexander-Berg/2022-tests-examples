package ru.auto.chatbot.lifecycle.sync

import org.mockito.Mockito.{times, verify, verifyZeroInteractions}
import ru.auto.chatbot.lifecycle.Events._
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.ButtonCode.{BAD, GOING_TO_CHECKUP, GOOD, OK}
import ru.auto.chatbot.model.MessageCode.{BASIC_ERROR_MESSAGE, HOW_DO_YOU_LIKE_ME, LINK_SHARE, START_MESSAGE}
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{FINISH, HOW_DO_YOU_LIKE_ME_AWAIT, OFFER_AWAIT}

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-12.
  */
class FinishTest extends MessageProcessorSuit {

  test("FINISH GoingToCheckup") {
    val state = State(step = FINISH, roomId = "1")
    val res = fsm.transition(GoingToCheckup("10"), state).futureValue

    res.step shouldBe OFFER_AWAIT
    res.previousStep shouldBe FINISH
    res.previousMessageCode shouldBe messageCode

    verify(chatManager, times(1))
      .sendMessage(?, eq(LINK_SHARE), ?)
  }

  test("FINISH msg") {
    val state = State(step = FINISH, roomId = "1")
    val res = Await.result(fsm.transition(Msg("10", ""), state), 10.seconds)
    res.step shouldBe FINISH
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(START_MESSAGE), eq(Seq(GOING_TO_CHECKUP)))
  }

  test("FINISH iamhere") {
    val state = State(step = FINISH)
    val res = Await.result(fsm.transition(IamHere("10"), state), 10.seconds)
    res.step shouldBe FINISH
    verify(chatManager).sendMessage(?, eq(BASIC_ERROR_MESSAGE), ?)
  }

  test("FINISH TimeOut") {
    val state = Await.result(fsm.transition(TimeOut(""), State(step = FINISH)), 10.seconds)
    state.step shouldBe FINISH
    verifyZeroInteractions(chatManager)
  }

  test("FINISH FeedbackTime") {
    val state = Await.result(fsm.transition(FeedbackTime(""), State(step = FINISH)), 10.seconds)
    state.step shouldBe HOW_DO_YOU_LIKE_ME_AWAIT
    verify(chatManager).sendMessage(?, eq(HOW_DO_YOU_LIKE_ME), eq(Seq(GOOD, OK, BAD)))
  }
}
