package ru.auto.chatbot.lifecycle.sync

import org.mockito.Mockito.{verify, verifyZeroInteractions}
import ru.auto.chatbot.lifecycle.Events.{Bad, Good, Ok, TimeOut}
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.ButtonCode.GOING_TO_CHECKUP
import ru.auto.chatbot.model.MessageCode.{YOU_ARE_OKAY, YOU_DONT_LIKE_ME, YOU_LIKE_ME}
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{FEEDBACK_AWAIT, HOW_DO_YOU_LIKE_ME_AWAIT, IDLE}

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-12.
  */
class HowDoYouLikeMeAwaitTest extends MessageProcessorSuit {

  test("HOW_DO_YOU_LIKE_ME_AWAIT good") {
    val state = Await.result(fsm.transition(Good(""), State(step = HOW_DO_YOU_LIKE_ME_AWAIT)), 10.seconds)
    state.step shouldBe IDLE
    state.feedbackChoice shouldBe "Good"
    verify(chatManager).sendMessage(?, eq(YOU_LIKE_ME), eq(Seq(GOING_TO_CHECKUP)))
  }

  test("HOW_DO_YOU_LIKE_ME_AWAIT ok") {
    val state = Await.result(fsm.transition(Ok(""), State(step = HOW_DO_YOU_LIKE_ME_AWAIT)), 10.seconds)
    state.step shouldBe FEEDBACK_AWAIT
    state.feedbackChoice shouldBe "Ok"
    verify(chatManager).sendMessage(?, eq(YOU_ARE_OKAY), eq(Seq()))
  }

  test("HOW_DO_YOU_LIKE_ME_AWAIT bad") {
    val state = Await.result(fsm.transition(Bad(""), State(step = HOW_DO_YOU_LIKE_ME_AWAIT)), 10.seconds)
    state.step shouldBe FEEDBACK_AWAIT
    state.feedbackChoice shouldBe "Bad"
    verify(chatManager).sendMessage(?, eq(YOU_DONT_LIKE_ME), eq(Seq()))
  }

  test("HOW_DO_YOU_LIKE_ME_AWAIT TimeOut") {
    val state = Await.result(fsm.transition(TimeOut(""), State(step = HOW_DO_YOU_LIKE_ME_AWAIT)), 10.seconds)
    state.step shouldBe HOW_DO_YOU_LIKE_ME_AWAIT
    verifyZeroInteractions(chatManager)
  }

}
