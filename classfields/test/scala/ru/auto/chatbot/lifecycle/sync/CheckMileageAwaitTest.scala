package ru.auto.chatbot.lifecycle.sync

import org.mockito.Mockito.verify
import ru.auto.chatbot.lifecycle.Events.{AltStartMessage, No, Yes}
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.ButtonCode.{NO, YES}
import ru.auto.chatbot.model.MessageCode.{CHECKING_LINK, CHECK_RESULT_FRAUD, MILEAGE_OK_NO_HISTORY, MILEAGE_OK_WITH_HISTORY, MILEAGE_TYPE_IN}
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{CHECK_MILEAGE_AWAIT, GET_OFFER_INFORMATION_ASYNC, MILEAGE_AWAIT, QUESTION_AGREE_AWAIT}
import ru.auto.chatbot.utils.StateUtils.HistoryTag

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-11.
  */
class CheckMileageAwaitTest extends MessageProcessorSuit {

  test("CHECK_MILEAGE_AWAIT yes no history") {
    val state = State(step = CHECK_MILEAGE_AWAIT)
    val res = Await.result(fsm.transition(Yes(""), state), 10.seconds)

    res.step shouldBe QUESTION_AGREE_AWAIT
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(MILEAGE_OK_NO_HISTORY), eq(Seq(YES, NO)))
  }

  test("CHECK_MILEAGE_AWAIT yes with history") {
    val state = State(step = CHECK_MILEAGE_AWAIT, offerTags = Seq(HistoryTag))
    val res = Await.result(fsm.transition(Yes(""), state), 10.seconds)

    res.step shouldBe QUESTION_AGREE_AWAIT
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(MILEAGE_OK_WITH_HISTORY), eq(Seq(YES, NO)))
  }

  test("CHECK_MILEAGE_AWAIT yes with history but fraud") {
    val state =
      State(step = CHECK_MILEAGE_AWAIT, offerTags = Seq(HistoryTag), isSameCarReport = CHECK_RESULT_FRAUD.toString)
    val res = Await.result(fsm.transition(Yes(""), state), 10.seconds)

    res.step shouldBe QUESTION_AGREE_AWAIT
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(MILEAGE_OK_NO_HISTORY), eq(Seq(YES, NO)))
  }

  test("CHECK_MILEAGE_AWAIT no") {
    val state = State(step = CHECK_MILEAGE_AWAIT, offerTags = Seq(HistoryTag))
    val res = Await.result(fsm.transition(No(""), state), 10.seconds)

    res.step shouldBe MILEAGE_AWAIT
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(MILEAGE_TYPE_IN), eq(Seq()))
  }

  test("CHECK_MILEAGE_AWAIT AltStartMessage") {
    val offerId = "offer_id"
    val state = State(step = CHECK_MILEAGE_AWAIT, offerTags = Seq(HistoryTag))
    val res = Await.result(fsm.transition(AltStartMessage("", "offer_id"), state), 10.seconds)

    res.step shouldBe GET_OFFER_INFORMATION_ASYNC
    res.isAsync shouldBe true
    res.offerId shouldBe offerId

    verify(chatManager).sendMessage(?, eq(CHECKING_LINK), eq(Seq()))
  }

}
