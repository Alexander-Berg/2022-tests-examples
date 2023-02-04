package ru.auto.chatbot.lifecycle.sync

import org.mockito.Mockito.verify
import ru.auto.chatbot.lifecycle.Events.{No, Yes}
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.MessageCode.{EMAIL_TYPE_IN, TNX_FOR_FEEDBACK}
import ru.auto.chatbot.model.ButtonCode.{GOING_TO_CHECKUP, NO_THANK_YOU}
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{EMAIL_APPROVE_AWAIT, EMAIL_TYPE_IN_AWAIT, FINISH}

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-15.
  */
class EmailApproveAwaitTest extends MessageProcessorSuit {

  test("EMAIL_APPROVE_AWAIT yes") {
    val state = State(step = EMAIL_APPROVE_AWAIT)
    val res = Await.result(fsm.transition(Yes(""), state), 10.seconds)

    res.step shouldBe FINISH
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(TNX_FOR_FEEDBACK), eq(Seq(GOING_TO_CHECKUP)))
  }

  test("EMAIL_APPROVE_AWAIT no") {
    val state = State(step = EMAIL_APPROVE_AWAIT)
    val res = Await.result(fsm.transition(No(""), state), 10.seconds)

    res.step shouldBe EMAIL_TYPE_IN_AWAIT
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(EMAIL_TYPE_IN), eq(Seq(NO_THANK_YOU)))
  }

}
