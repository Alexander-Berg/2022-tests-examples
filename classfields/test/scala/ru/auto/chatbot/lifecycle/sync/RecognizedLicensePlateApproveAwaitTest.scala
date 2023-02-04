package ru.auto.chatbot.lifecycle.sync

import org.mockito.Mockito.verify
import ru.auto.chatbot.lifecycle.Events.{No, Yes}
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.MessageCode.{CHECK_RUN, GRZ_MANUAL_TYPE_IN}
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{LICENSE_PLATE_TYPE_IN_AWAIT, RECOGNIZED_LICENSE_PLATE_APPROVE_AWAIT, VIN_BY_LICENSE_PLATE_ASYNC}

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-01.
  */
class RecognizedLicensePlateApproveAwaitTest extends MessageProcessorSuit {

  test("RECOGNIZED_LICENSE_PLATE_APPROVE_AWAIT yes") {
    val state = State(step = RECOGNIZED_LICENSE_PLATE_APPROVE_AWAIT)
    val res = Await.result(fsm.transition(Yes(""), state), 10.seconds)

    res.step shouldBe VIN_BY_LICENSE_PLATE_ASYNC
    res.isAsync shouldBe true

    verify(chatManager).sendMessage(?, eq(CHECK_RUN), ?)
  }

  test("RECOGNIZED_LICENSE_PLATE_APPROVE_AWAIT no") {
    val state = State(step = RECOGNIZED_LICENSE_PLATE_APPROVE_AWAIT)
    val res = Await.result(fsm.transition(No(""), state), 10.seconds)

    res.step shouldBe LICENSE_PLATE_TYPE_IN_AWAIT
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(GRZ_MANUAL_TYPE_IN), ?)
  }

}
