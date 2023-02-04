package ru.auto.chatbot.lifecycle.sync

import org.mockito.Mockito.verify
import ru.auto.chatbot.app.TestContext
import ru.auto.chatbot.lifecycle.Events.{AltStartMessage, No, Yes}
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.ButtonCode.{GOING_TO_CHECKUP, HOW_TO_CHECK, NO, YES}
import ru.auto.chatbot.model.MessageCode.{CHECKING_LINK, WELL_OKAY_THEN}
import ru.auto.chatbot.model.questions.BunkerCloseQuestion.CloseQuestionPart
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step._

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-01.
  */
class QuestionAgreeAwaitTest extends MessageProcessorSuit {

  import TestContext._

  test("QUESTION_AGREE_AWAIT yes") {
    val state = State(step = QUESTION_AGREE_AWAIT)
    val res = Await.result(fsm.transition(Yes(""), state), 10.seconds)

    res.step shouldBe QUESTION_ANSWER_AWAIT
    res.isAsync shouldBe false
    res.questionId shouldBe bunkerQuestionsVariations.getCloseQuestions(state).firstQuestionId

    verify(chatManager).sendCloseQuestion(
      ?,
      eq(bunkerQuestionsVariations.getCloseQuestions(state).firstQuestionId),
      eq(CloseQuestionPart.QUESTION),
      eq(Seq(YES, NO, HOW_TO_CHECK))
    )
  }

  test("QUESTION_AGREE_AWAIT no") {
    val state = State(step = QUESTION_AGREE_AWAIT)
    val res = Await.result(fsm.transition(No(""), state), 10.seconds)

    res.step shouldBe IDLE
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(WELL_OKAY_THEN), eq(Seq(GOING_TO_CHECKUP)))
  }

  test("QUESTION_AGREE_AWAIT get alt_start_message event") {
    val state = Await.result(
      fsm.transition(AltStartMessage("10", "123-abc"), State(step = QUESTION_AGREE_AWAIT, vinByLicensePlate = "123")),
      10.seconds
    )
    state.step shouldBe GET_OFFER_INFORMATION_ASYNC
    state.isAsync shouldBe true
    state.offerId shouldBe "123-abc"

    verify(chatManager).sendMessage(?, eq(CHECKING_LINK), eq(Seq()))
  }

}
