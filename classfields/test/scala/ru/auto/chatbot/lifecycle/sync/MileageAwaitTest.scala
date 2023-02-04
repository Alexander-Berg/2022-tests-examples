package ru.auto.chatbot.lifecycle.sync

import org.mockito.Mockito.verify
import ru.auto.chatbot.lifecycle.Events.Msg
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.ButtonCode.{NO, YES}
import ru.auto.chatbot.model.MessageCode.{MILEAGE_NOT_OK_NO_HISTORY, MILEAGE_NOT_OK_WITH_HISTORY, WRONG_MILEAGE}
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{MILEAGE_AWAIT, QUESTION_AGREE_AWAIT}
import ru.auto.chatbot.utils.StateUtils.HistoryTag

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-01.
  */
class MileageAwaitTest extends MessageProcessorSuit {

  test("MILEAGE_AWAIT not mileage") {
    val state = State(step = MILEAGE_AWAIT)
    val res = Await.result(fsm.transition(Msg("", "test"), state), 10.seconds)

    res.step shouldBe MILEAGE_AWAIT
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(WRONG_MILEAGE), eq(Seq()))
  }

  test("MILEAGE_AWAIT correct mileage no history") {
    val state = State(step = MILEAGE_AWAIT)
    val res = Await.result(fsm.transition(Msg("", "  12270 km"), state), 10.seconds)

    res.step shouldBe QUESTION_AGREE_AWAIT
    res.isAsync shouldBe false
    res.mileageUser shouldBe 12270

    verify(chatManager).sendMessage(?, eq(MILEAGE_NOT_OK_NO_HISTORY), eq(Seq(YES, NO)))
  }

  test("MILEAGE_AWAIT correct mileage with history") {
    val state = State(step = MILEAGE_AWAIT, offerTags = Seq(HistoryTag))
    val res = Await.result(fsm.transition(Msg("", "  12270 km"), state), 10.seconds)

    res.step shouldBe QUESTION_AGREE_AWAIT
    res.isAsync shouldBe false
    res.mileageUser shouldBe 12270

    verify(chatManager).sendMessage(?, eq(MILEAGE_NOT_OK_WITH_HISTORY), eq(Seq(YES, NO)))
  }

}
