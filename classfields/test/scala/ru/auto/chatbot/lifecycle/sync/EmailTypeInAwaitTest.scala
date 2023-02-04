package ru.auto.chatbot.lifecycle.sync

import org.mockito.Mockito.verify
import ru.auto.chatbot.lifecycle.Events.{Msg, NoThankYou}
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.ButtonCode.GOING_TO_CHECKUP
import ru.auto.chatbot.model.MessageCode.TNX_FOR_FEEDBACK
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{EMAIL_TYPE_IN_AWAIT, FINISH}

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-15.
  */
class EmailTypeInAwaitTest extends MessageProcessorSuit {

  test("EMAIL_TYPE_IN_AWAIT NoThankYou") {
    val state = State(step = EMAIL_TYPE_IN_AWAIT)

    val res = Await.result(fsm.transition(NoThankYou(""), state), 10.seconds)

    res.step shouldBe FINISH
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(TNX_FOR_FEEDBACK), eq(Seq(GOING_TO_CHECKUP)))
  }

  test("EMAIL_TYPE_IN_AWAIT email") {
    val email = "bbblala@afaf. ru"
    val state = State(step = EMAIL_TYPE_IN_AWAIT)
    val res = Await.result(fsm.transition(Msg("", email), state), 10.seconds)

    res.step shouldBe FINISH
    res.isAsync shouldBe false
    res.sendLetter shouldBe true
    res.userTypedEmail shouldBe email.replaceAll(" ", "")

    verify(chatManager).sendMessage(?, eq(TNX_FOR_FEEDBACK), eq(Seq(GOING_TO_CHECKUP)))
  }
}
