package ru.auto.chatbot.lifecycle.sync

import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import ru.auto.chatbot.lifecycle.Events.Msg
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.ButtonCode.{NO, NO_THANK_YOU, YES}
import ru.auto.chatbot.model.MessageCode.{EMAIL_CHECK_NO, EMAIL_CHECK_YES}
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{CHECKUP_REPORT_GENERATION_ASYNC, COMMENT_AWAIT, EMAIL_APPROVE_AWAIT, EMAIL_TYPE_IN_AWAIT}

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-01.
  */
class CommentAwaitTest extends MessageProcessorSuit {

  test("COMMENT_AWAIT comment") {
    val state = State(step = COMMENT_AWAIT)
    val comment = "comment"
    val res = Await.result(fsm.transition(Msg("", comment), state), 10.seconds)

    res.isAsync shouldBe true
    res.step shouldBe CHECKUP_REPORT_GENERATION_ASYNC
    res.comment shouldBe comment

    verifyZeroInteractions(chatManager)
  }
}
