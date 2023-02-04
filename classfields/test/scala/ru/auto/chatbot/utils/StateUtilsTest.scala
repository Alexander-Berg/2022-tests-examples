package ru.auto.chatbot.utils

import org.scalatest.FunSuite
import ru.auto.chatbot.state_model.State

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-03-29.
  */
class StateUtilsTest extends FunSuite {

  test("offer url from state") {
    val testMark = "testMark"
    val testModel = "testModel"
    val testOfferId = "123"
    val state = State(offerId = testOfferId, offerMark = testMark, offerModel = testModel)
    val res = StateUtils.getOfferLink(state).get

    assert(res == s"https://auto.ru/cars/used/sale/$testOfferId")
  }

}
