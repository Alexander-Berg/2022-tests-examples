package ru.auto.chatbot.lifecycle.sync

import org.mockito.Mockito.{verify, verifyZeroInteractions}
import ru.auto.chatbot.lifecycle.Events.Msg
import ru.auto.chatbot.lifecycle.MessageProcessorSuit
import ru.auto.chatbot.model.MessageCode.{CHECKING_LINK, INVALID_LINK, NOT_AUTO_RU_LINK, NOT_A_LINK}
import ru.auto.chatbot.state_model.State
import ru.auto.chatbot.state_model.Step.{GET_OFFER_INFORMATION_ASYNC, OFFER_AWAIT}

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-01.
  */
class OfferAwaitTest extends MessageProcessorSuit {

  test("OFFER_AWAIT no a link") {
    val state = State(step = OFFER_AWAIT)
    val res = Await.result(fsm.transition(Msg("10", "blabla"), state), 10.seconds)

    res.step shouldBe OFFER_AWAIT
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(NOT_A_LINK), ?)
    verifyZeroInteractions(vosClient)
  }

  test("OFFER_AWAIT invalid url") {
    val state = State(step = OFFER_AWAIT)
    val res = Await.result(fsm.transition(Msg("10", "https://auto.ru/ "), state), 10.seconds)

    res.step shouldBe OFFER_AWAIT
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(INVALID_LINK), ?)
    verifyZeroInteractions(vosClient)
  }

  test("OFFER_AWAIT not autoru url") {
    val state = State(step = OFFER_AWAIT)
    val res = Await.result(fsm.transition(Msg("10", "https://yandex.ru/ "), state), 10.seconds)

    res.step shouldBe OFFER_AWAIT
    res.isAsync shouldBe false

    verify(chatManager).sendMessage(?, eq(NOT_AUTO_RU_LINK), ?)
    verifyZeroInteractions(vosClient)
  }

  test("OFFER_AWAIT process offer") {
    val offerUrl = "https://auto.ru/cars/used/sale/renault/megane/1077918705-79ff12da"
    val state = State(step = OFFER_AWAIT, userId = "test")

    val res = fsm.transition(Msg("10", offerUrl), state).futureValue

    verifyZeroInteractions(vosClient)
    verifyZeroInteractions(clusteringManager)

    res.step shouldBe GET_OFFER_INFORMATION_ASYNC
    res.isAsync shouldBe true

    verify(chatManager).sendMessage(?, eq(CHECKING_LINK), ?)
  }

}
