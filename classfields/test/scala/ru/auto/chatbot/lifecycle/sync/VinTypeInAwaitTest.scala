package ru.auto.chatbot.lifecycle.sync

import org.mockito.Mockito.verify
import ru.auto.chatbot.lifecycle.Events.Msg
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.MessageCode._
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{COMPARE_VIN_AND_GRZ_ASYNC, VIN_TYPE_IN_AWAIT}

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-01.
  */
class VinTypeInAwaitTest extends MessageProcessorSuit {

  test("COMPARE_VIN_AND_GRZ_ASYNC vin") {
    val state = State(step = VIN_TYPE_IN_AWAIT)
    val vin = "SVH651DP0HA000844"
    val res = Await.result(fsm.transition(Msg("", vin), state), 10.seconds)

    res.isAsync shouldBe true
    res.step shouldBe COMPARE_VIN_AND_GRZ_ASYNC
    res.vinUser shouldBe vin

    verify(chatManager).sendMessage(?, eq(CHECK_RUN), ?)
  }

  test("short vin error") {
    val state = State(step = VIN_TYPE_IN_AWAIT)
    val vin = "abc"
    val res = Await.result(fsm.transition(Msg("", vin), state), 10.seconds)

    res.isAsync shouldBe false
    res.step shouldBe VIN_TYPE_IN_AWAIT
    res.vinUser shouldBe ""

    verify(chatManager).sendMessage(?, eq(SHORT_VIN), ?)
  }

  test("contains cyrillic in vin error") {
    val state = State(step = VIN_TYPE_IN_AWAIT)
    val vin = "SVЯ651DP0HA000844"
    val res = Await.result(fsm.transition(Msg("", vin), state), 10.seconds)

    res.isAsync shouldBe false
    res.step shouldBe VIN_TYPE_IN_AWAIT
    res.vinUser shouldBe ""

    verify(chatManager).sendMessage(?, eq(CYRILLIC_VIN), ?)
  }

  test("wrong symbols in vin error") {
    val state = State(step = VIN_TYPE_IN_AWAIT)
    val vin = "SVI651DP0HA000844"
    val res = Await.result(fsm.transition(Msg("", vin), state), 10.seconds)

    res.isAsync shouldBe false
    res.step shouldBe VIN_TYPE_IN_AWAIT
    res.vinUser shouldBe ""

    verify(chatManager).sendMessage(?, eq(WRONG_VIN_SYMBOLS), ?)
  }

}
