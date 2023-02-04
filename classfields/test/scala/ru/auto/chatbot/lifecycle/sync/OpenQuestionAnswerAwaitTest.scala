package ru.auto.chatbot.lifecycle.sync

import org.mockito.Mockito.{times, verify}
import ru.auto.chatbot.lifecycle.Events.{Back, Continue, Msg, TimeOut}
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.ButtonCode._
import ru.auto.chatbot.model.MessageCode.TIME_OUT
import ru.auto.chatbot.model.questions.BunkerCloseQuestion.CloseQuestionPart
import ru.auto.chatbot.model.questions.BunkerOpenQuestion.OpenQuestionPart
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{OPEN_QUESTION_ANSWER_AWAIT, QUESTION_ANSWER_AWAIT, TIME_OUT_CONTINUE_AWAIT}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-05.
  */
class OpenQuestionAnswerAwaitTest extends MessageProcessorSuit {

  import ru.auto.chatbot.app.TestContext._

  test("OPEN_QUESTION_ANSWER_AWAIT time out") {
    val state = State(step = OPEN_QUESTION_ANSWER_AWAIT, previousMessage = Some(sentMessage))

    val res = Await.result(fsm.transition(TimeOut(""), state), 10.seconds)

    res.step shouldBe TIME_OUT_CONTINUE_AWAIT
    res.isAsync shouldBe false
    res.previousStep shouldBe OPEN_QUESTION_ANSWER_AWAIT
    res.previousMessage.get shouldBe sentMessage

    verify(chatManager).sendMessage(?, eq(TIME_OUT), eq(Seq(CONTINUE, CANCEL)))
  }

  test("OPEN_QUESTION_ANSWER_AWAIT continue") {
    val state = State(step = OPEN_QUESTION_ANSWER_AWAIT, openQuiestionId = 1, questionId = 21)

    val res = Await.result(fsm.transition(Continue(""), state), 10.seconds)

    res.step shouldBe OPEN_QUESTION_ANSWER_AWAIT
    res.openQuiestionId shouldBe 2
    res.questionId shouldBe 21
    res.isAsync shouldBe false
    res.previousStep shouldBe OPEN_QUESTION_ANSWER_AWAIT
    res.previousMessage.get shouldBe sentMessage
    res.openQuestionAnswers("1") shouldBe ""

    verify(chatManager).sendOpenQuestion(?, eq(2), eq(OpenQuestionPart.QUESTION), eq(Seq(CONTINUE, BACK)))
  }

  test("OPEN_QUESTION_ANSWER_AWAIT answer") {
    val state = State(step = OPEN_QUESTION_ANSWER_AWAIT, openQuiestionId = 1, questionId = 21)

    val res = Await.result(fsm.transition(Msg("", "test"), state), 10.seconds)

    res.step shouldBe OPEN_QUESTION_ANSWER_AWAIT
    res.openQuiestionId shouldBe 2
    res.questionId shouldBe 21
    res.isAsync shouldBe false
    res.previousStep shouldBe OPEN_QUESTION_ANSWER_AWAIT
    res.previousMessage.get shouldBe sentMessage
    res.openQuestionAnswers("1") shouldBe "test"

    verify(chatManager).sendOpenQuestion(?, eq(2), eq(OpenQuestionPart.QUESTION), eq(Seq(CONTINUE, BACK)))
  }

  test("OPEN_QUESTION_ANSWER_AWAIT change my mind") {
    val state = State(step = OPEN_QUESTION_ANSWER_AWAIT, openQuiestionId = 1, questionId = 21)

    val res = Await.result(
      fsm
        .transition(Continue(""), state)
        .flatMap(
          fsm
            .transition(Back(""), _)
            .flatMap(fsm.transition(Msg("", "test"), _))
        ),
      10.seconds
    )

    res.step shouldBe OPEN_QUESTION_ANSWER_AWAIT
    res.openQuiestionId shouldBe 2
    res.questionId shouldBe 21
    res.isAsync shouldBe false
    res.previousStep shouldBe OPEN_QUESTION_ANSWER_AWAIT
    res.previousMessage.get shouldBe sentMessage
    res.openQuestionAnswers("1") shouldBe "test"

    verify(chatManager, times(2)).sendOpenQuestion(?, eq(2), eq(OpenQuestionPart.QUESTION), eq(Seq(CONTINUE, BACK)))
    verify(chatManager, times(1)).sendOpenQuestion(
      ?,
      eq(bunkerQuestionsVariations.getOpenQuestions(state).firstQuestionId),
      eq(OpenQuestionPart.QUESTION),
      eq(Seq(CONTINUE, BACK))
    )
  }

  test("OPEN_QUESTION_ANSWER_AWAIT back") {
    val state = State(step = OPEN_QUESTION_ANSWER_AWAIT, openQuiestionId = 2, questionId = 21)

    val res = Await.result(fsm.transition(Back(""), state), 10.seconds)

    res.step shouldBe OPEN_QUESTION_ANSWER_AWAIT
    res.openQuiestionId shouldBe 1
    res.questionId shouldBe 21
    res.isAsync shouldBe false
    res.previousStep shouldBe OPEN_QUESTION_ANSWER_AWAIT
    res.previousMessage.get shouldBe sentMessage

    verify(chatManager).sendOpenQuestion(?, eq(1), eq(OpenQuestionPart.QUESTION), eq(Seq(CONTINUE, BACK)))
  }

  test("OPEN_QUESTION_ANSWER_AWAIT back first open message") {
    val state = State(step = OPEN_QUESTION_ANSWER_AWAIT, openQuiestionId = 1, questionId = 21)

    val res = Await.result(fsm.transition(Back(""), state), 10.seconds)

    res.step shouldBe QUESTION_ANSWER_AWAIT
    res.openQuiestionId shouldBe 0
    res.questionId shouldBe 21
    res.isAsync shouldBe false
    res.previousStep shouldBe OPEN_QUESTION_ANSWER_AWAIT
    res.previousMessage.get shouldBe sentMessage

    verify(chatManager).sendCloseQuestion(
      ?,
      eq(bunkerQuestionsVariations.getCloseQuestions(state).lastQuestionId),
      eq(CloseQuestionPart.QUESTION),
      eq(Seq(YES, NO, I_DONT_KNOW, HOW_TO_CHECK, BACK))
    )
  }

}
