package ru.auto.chatbot.lifecycle.sync

import org.mockito.Mockito.{times, verify}
import ru.auto.chatbot.app.TestContext
import ru.auto.chatbot.lifecycle.Events._
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.ButtonCode._
import ru.auto.chatbot.model.MessageCode.{BASIC_ERROR_MESSAGE, FINAL_ADVICE, TIME_OUT}
import ru.auto.chatbot.model.questions.BunkerCloseQuestion.CloseQuestionPart
import ru.auto.chatbot.model.questions.BunkerOpenQuestion.OpenQuestionPart
import ru.auto.chatbot.state_model.{QuestionAnswer, State}
import ru.auto.chatbot.state_model.Step.{COMMENT_AWAIT, OPEN_QUESTION_ANSWER_AWAIT, QUESTION_ANSWER_AWAIT, TIME_OUT_CONTINUE_AWAIT}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-01.
  */
class QuestionAnswerAwaitTest extends MessageProcessorSuit {

  import TestContext._

  test("QUESTION_ANSWER_AWAIT why so important") {
    val state = State(questionId = 5, step = QUESTION_ANSWER_AWAIT)
    val res = Await.result(fsm.transition(WhySoImportant(""), state), 10.seconds)

    res.step shouldBe QUESTION_ANSWER_AWAIT
    res.isAsync shouldBe false
    res.questionId shouldBe 5
    res.previousMessage shouldBe None

    verify(chatManager).sendMessage(?, eq(BASIC_ERROR_MESSAGE), eq(Seq()))
  }

  test("QUESTION_ANSWER_AWAIT how to check") {
    val state = State(questionId = 5, step = QUESTION_ANSWER_AWAIT)
    val res = Await.result(fsm.transition(HowToCheck(""), state), 10.seconds)

    res.step shouldBe QUESTION_ANSWER_AWAIT
    res.isAsync shouldBe false
    res.questionId shouldBe 5
    res.previousMessage.get shouldBe sentMessage

    verify(chatManager).sendCloseQuestion(?, eq(5), eq(CloseQuestionPart.WHERE), eq(Seq(YES, NO, I_DONT_KNOW, BACK)))
  }

  test("QUESTION_ANSWER_AWAIT how to check firs question") {
    val state = State(questionId = 1, step = QUESTION_ANSWER_AWAIT)
    val res = Await.result(fsm.transition(HowToCheck(""), state), 10.seconds)

    res.step shouldBe QUESTION_ANSWER_AWAIT
    res.isAsync shouldBe false
    res.questionId shouldBe 1
    res.previousMessage.get shouldBe sentMessage

    verify(chatManager).sendCloseQuestion(?, eq(1), eq(CloseQuestionPart.WHERE), eq(Seq(YES, NO, I_DONT_KNOW)))

  }

  test("QUESTION_ANSWER_AWAIT Yes") {
    val state = State(questionId = 5, step = QUESTION_ANSWER_AWAIT)
    val res = Await.result(fsm.transition(Yes(""), state), 10.seconds)
    val question = bunkerQuestionsVariations.getCloseQuestions(state).questions(5)
    val answer = QuestionAnswer(question.yesAnswer.text, question.yesAnswer.status)

    res.step shouldBe QUESTION_ANSWER_AWAIT
    res.isAsync shouldBe false
    res.questionId shouldBe 6
    res.previousMessage.get shouldBe sentMessage
    res.questionAnswers("5") shouldBe answer

    verify(chatManager).sendCloseQuestion(
      ?,
      eq(6),
      eq(CloseQuestionPart.QUESTION),
      eq(Seq(YES, NO, I_DONT_KNOW, HOW_TO_CHECK, BACK))
    )
  }

  test("QUESTION_ANSWER_AWAIT last question") {
    val state = State(
      questionId = bunkerQuestionsVariations.getCloseQuestions(State()).lastQuestionId,
      step = QUESTION_ANSWER_AWAIT
    )
    val res = Await.result(fsm.transition(No(""), state), 10.seconds)

    res.step shouldBe OPEN_QUESTION_ANSWER_AWAIT
    res.isAsync shouldBe false
    res.questionId shouldBe bunkerQuestionsVariations.getCloseQuestions(state).lastQuestionId
    res.openQuiestionId shouldBe bunkerQuestionsVariations.getCloseQuestions(state).firstQuestionId
    res.previousMessage.get shouldBe sentMessage

    verify(chatManager).sendOpenQuestion(
      ?,
      eq(bunkerQuestionsVariations.getOpenQuestions(state).firstQuestionId),
      eq(OpenQuestionPart.QUESTION),
      eq(Seq(CONTINUE, BACK))
    )
  }

  test("OPEN_QUESTION_ANSWER_AWAIT continue") {
    val state = State(openQuiestionId = 1, step = OPEN_QUESTION_ANSWER_AWAIT)
    val res = Await.result(fsm.transition(Continue(""), state), 10.seconds)

    res.step shouldBe OPEN_QUESTION_ANSWER_AWAIT
    res.isAsync shouldBe false
    res.openQuiestionId shouldBe 2
    res.previousMessage.get shouldBe sentMessage

    verify(chatManager).sendOpenQuestion(?, eq(2), eq(OpenQuestionPart.QUESTION), eq(Seq(CONTINUE, BACK)))
  }

  test("OPEN_QUESTION_ANSWER_AWAIT why so important") {
    val state = State(openQuiestionId = 2, step = OPEN_QUESTION_ANSWER_AWAIT)
    val res = Await.result(fsm.transition(WhySoImportant(""), state), 10.seconds)

    res.step shouldBe OPEN_QUESTION_ANSWER_AWAIT
    res.isAsync shouldBe false
    res.openQuiestionId shouldBe 2
    res.previousMessage shouldBe None

    verify(chatManager).sendMessage(?, eq(BASIC_ERROR_MESSAGE), eq(Seq()))
  }

  test("OPEN_QUESTION_ANSWER_AWAIT last question") {
    val state = State(
      openQuiestionId = bunkerQuestionsVariations.getOpenQuestions(State()).lastQuestionId,
      step = OPEN_QUESTION_ANSWER_AWAIT
    )
    val res = Await.result(fsm.transition(Continue(""), state), 10.seconds)

    res.step shouldBe COMMENT_AWAIT
    res.isAsync shouldBe false
    res.openQuiestionId shouldBe bunkerQuestionsVariations.getOpenQuestions(state).lastQuestionId
    res.previousMessage.get shouldBe sentMessage

    verify(chatManager).sendMessage(?, eq(FINAL_ADVICE), eq(Seq()))
  }

  test("OPEN_QUESTION_ANSWER_AWAIT msg") {
    val state = State(openQuiestionId = 1, step = OPEN_QUESTION_ANSWER_AWAIT)
    val res = Await.result(fsm.transition(Msg("", "text"), state), 10.seconds)

    res.step shouldBe OPEN_QUESTION_ANSWER_AWAIT
    res.isAsync shouldBe false
    res.openQuiestionId shouldBe 2
    res.previousMessage.get shouldBe sentMessage

    verify(chatManager).sendOpenQuestion(?, eq(2), eq(OpenQuestionPart.QUESTION), eq(Seq(CONTINUE, BACK)))
  }

  test("QUESTION_ANSWER_AWAIT time out event") {
    val state = State(
      questionId = 5,
      step = QUESTION_ANSWER_AWAIT,
      timeOutedStep = QUESTION_ANSWER_AWAIT,
      previousMessage = Some(sentMessage)
    )

    val res = Await.result(fsm.transition(TimeOut(""), state), 10.seconds)

    res.step shouldBe TIME_OUT_CONTINUE_AWAIT
    res.isAsync shouldBe false
    res.questionId shouldBe 5
    res.previousStep shouldBe QUESTION_ANSWER_AWAIT
    res.previousMessage.get shouldBe sentMessage

    verify(chatManager).sendMessage(?, eq(TIME_OUT), eq(Seq(CONTINUE, CANCEL)))
  }

  test("QUESTION_ANSWER_AWAIT back first question") {
    val state = State(questionId = 1, step = QUESTION_ANSWER_AWAIT)
    val res = Await.result(fsm.transition(WhySoImportant(""), state), 10.seconds)

    res.step shouldBe QUESTION_ANSWER_AWAIT
    res.isAsync shouldBe false
    res.questionId shouldBe 1
    res.previousMessage shouldBe None

    verify(chatManager).sendMessage(?, eq(BASIC_ERROR_MESSAGE), eq(Seq()))
  }

  test("QUESTION_ANSWER_AWAIT back") {
    val state = State(questionId = 5, step = QUESTION_ANSWER_AWAIT)
    val res = Await.result(fsm.transition(Back(""), state), 10.seconds)

    res.step shouldBe QUESTION_ANSWER_AWAIT
    res.isAsync shouldBe false
    res.questionId shouldBe 4
    res.previousMessage.get shouldBe sentMessage

    verify(chatManager).sendCloseQuestion(
      ?,
      eq(4),
      eq(CloseQuestionPart.QUESTION),
      eq(Seq(YES, NO, I_DONT_KNOW, HOW_TO_CHECK, BACK))
    )
  }

  test("QUESTION_ANSWER_AWAIT change my opinion") {
    val state = State(questionId = 5, step = QUESTION_ANSWER_AWAIT)

    val question = bunkerQuestionsVariations.getCloseQuestions(state).questions(5)
    val answer = QuestionAnswer(question.noAnswer.text, question.noAnswer.status)

    val res = Await.result(
      fsm
        .transition(Yes(""), state)
        .flatMap(
          fsm
            .transition(Back(""), _)
            .flatMap(fsm.transition(No(""), _))
        ),
      10.seconds
    )

    res.step shouldBe QUESTION_ANSWER_AWAIT
    res.isAsync shouldBe false
    res.questionId shouldBe 6
    res.previousMessage.get shouldBe sentMessage
    res.questionAnswers("5") shouldBe answer

    verify(chatManager, times(2)).sendCloseQuestion(
      ?,
      eq(6),
      eq(CloseQuestionPart.QUESTION),
      eq(Seq(YES, NO, I_DONT_KNOW, HOW_TO_CHECK, BACK))
    )
    verify(chatManager).sendCloseQuestion(
      ?,
      eq(5),
      eq(CloseQuestionPart.QUESTION),
      eq(Seq(YES, NO, I_DONT_KNOW, HOW_TO_CHECK, BACK))
    )

  }
}
