package ru.auto.chatbot.lifecycle.sync

import org.mockito.Mockito.verify
import ru.auto.chatbot.lifecycle.Events.Msg
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.MessageCode._
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{LICENSE_PLATE_TYPE_IN_AWAIT, VIN_BY_LICENSE_PLATE_ASYNC}

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-01.
  */
class LicensePlateTypeInAwaitTest extends MessageProcessorSuit {

  test("LICENSE_PLATE_TYPE_IN_AWAIT msg") {
    val state = State(step = LICENSE_PLATE_TYPE_IN_AWAIT)
    val res = Await.result(fsm.transition(Msg("", "м386мн799"), state), 10.seconds)

    res.isAsync shouldBe true
    res.licensePlateUser shouldBe "М386МН799"
    res.step shouldBe VIN_BY_LICENSE_PLATE_ASYNC

    verify(chatManager).sendMessage(?, eq(CHECK_RUN), ?)
  }

  test("latin letters in grz") {
    val state = State(step = LICENSE_PLATE_TYPE_IN_AWAIT)
    val res = Await.result(fsm.transition(Msg("", "testGrz"), state), 10.seconds)

    res.isAsync shouldBe false
    res.licensePlateUser shouldBe ""
    res.step shouldBe LICENSE_PLATE_TYPE_IN_AWAIT

    verify(chatManager).sendMessage(?, eq(WRONG_GRZ_SYMBOLS), ?)
  }

  test("not allowed symbols in grz") {
    val state = State(step = LICENSE_PLATE_TYPE_IN_AWAIT)
    val res = Await.result(fsm.transition(Msg("", "А777МЯ777"), state), 10.seconds)

    res.isAsync shouldBe false
    res.licensePlateUser shouldBe ""
    res.step shouldBe LICENSE_PLATE_TYPE_IN_AWAIT

    verify(chatManager).sendMessage(?, eq(WRONG_GRZ_SYMBOLS), ?)
  }

}
