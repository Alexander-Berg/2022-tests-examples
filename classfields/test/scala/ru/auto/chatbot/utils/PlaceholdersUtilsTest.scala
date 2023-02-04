package ru.auto.chatbot.utils

import org.scalatest.FunSuite
import ru.auto.chatbot.state_model.State

/**
  * Created by sievmi on 29.03.19
  */
class PlaceholdersUtilsTest extends FunSuite {

  test("no placeholders in text") {
    val state = State(offerId = "123", offerMark = "mark", offerModel = "model", mileageOffer = 100, yearOffer = 2000)
    val text = "text without any placeholders"

    assert(PlaceholdersUtils.fillPlaceholders(state, text) === text)
  }

  test("replace mileage and year from offer") {
    val state = State(offerId = "123", offerMark = "mark", offerModel = "model", mileageOffer = 100, yearOffer = 2000)
    val text = "mileage = {offer_mileage} year = {offer_year}"
    val expectedText = "mileage = 100 year = 2000"

    assert(PlaceholdersUtils.fillPlaceholders(state, text) === expectedText)
  }
}
