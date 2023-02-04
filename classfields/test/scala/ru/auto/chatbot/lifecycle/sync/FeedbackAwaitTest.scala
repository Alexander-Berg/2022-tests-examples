package ru.auto.chatbot.lifecycle.sync

import org.mockito.Mockito.verify
import ru.auto.chatbot.lifecycle.Events.{Msg, TimeOut}
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.ButtonCode.{CANCEL, CONTINUE, GOING_TO_CHECKUP}
import ru.auto.chatbot.model.MessageCode.{I_SEE_BYE, TIME_OUT}
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{FEEDBACK_AWAIT, IDLE, OPEN_QUESTION_ANSWER_AWAIT, TIME_OUT_CONTINUE_AWAIT}

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-12.
  */
class FeedbackAwaitTest extends MessageProcessorSuit {

  test("FEEDBACK_AWAIT store feedback") {
    val state = State(step = FEEDBACK_AWAIT)
    val feedback = "feedback"
    val res = Await.result(fsm.transition(Msg("", feedback), state), 10.seconds)

    res.isAsync shouldBe false
    res.step shouldBe IDLE
    res.feedback shouldBe feedback

    verify(chatManager).sendMessage(?, eq(I_SEE_BYE), eq(Seq(GOING_TO_CHECKUP)))
  }

  test("FEEDBACK_AWAIT time out") {
    val state = State(step = FEEDBACK_AWAIT, previousMessage = Some(sentMessage))

    val res = Await.result(fsm.transition(TimeOut(""), state), 10.seconds)

    res.step shouldBe TIME_OUT_CONTINUE_AWAIT
    res.isAsync shouldBe false
    res.timeOutedStep shouldBe FEEDBACK_AWAIT
    res.previousStep shouldBe FEEDBACK_AWAIT
    res.previousMessage.get shouldBe sentMessage

    verify(chatManager).sendMessage(?, eq(TIME_OUT), eq(Seq(CONTINUE, CANCEL)))
  }

}
