package ru.yandex.vertis.parsing.auto.dao.model

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.parsing.auto.ParsingAutoModel.ParsedOffer
import ru.yandex.vertis.parsing.auto.util.TestDataUtils.{testAvitoCarsUrl, testRow}

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class ParsedRowTest extends FunSuite {
  test("markModelWithNames: no mark model") {
    val url: String = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS)

    parsedOffer.getOfferBuilder.getCarInfoBuilder.getMarkInfoBuilder.setName("Honda")
    parsedOffer.getOfferBuilder.getCarInfoBuilder.getMarkInfoBuilder.setRuName("Хонда")
    parsedOffer.getOfferBuilder.getCarInfoBuilder.getModelInfoBuilder.setName("Partner")
    parsedOffer.getOfferBuilder.getCarInfoBuilder.getModelInfoBuilder.setRuName("Партнер")

    val result = parsedRow.markModelWithNames
    assert(result.isEmpty)
  }

  test("markModelWithNames: no ru names") {
    val url: String = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS)

    parsedOffer.getOfferBuilder.getCarInfoBuilder.setMark("HONDA")
    parsedOffer.getOfferBuilder.getCarInfoBuilder.getMarkInfoBuilder.setName("Honda")
    parsedOffer.getOfferBuilder.getCarInfoBuilder.setModel("PARTNER")
    parsedOffer.getOfferBuilder.getCarInfoBuilder.getModelInfoBuilder.setName("Partner")

    val result = parsedRow.markModelWithNames
    assert(result.nonEmpty)
    assert(result.get.mark.value == "HONDA")
    assert(result.get.markName.value == "Honda")
    assert(result.get.ruMarkName.value == "Honda")
    assert(result.get.model.value == "PARTNER")
    assert(result.get.modelName.value == "Partner")
    assert(result.get.ruModelName.value == "Partner")
  }

  test("markModelWithNames: no names") {
    val url: String = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS)

    parsedOffer.getOfferBuilder.getCarInfoBuilder.setMark("HONDA")
    parsedOffer.getOfferBuilder.getCarInfoBuilder.getMarkInfoBuilder.setRuName("Хонда")
    parsedOffer.getOfferBuilder.getCarInfoBuilder.setModel("PARTNER")
    parsedOffer.getOfferBuilder.getCarInfoBuilder.getModelInfoBuilder.setRuName("Партнер")

    val result = parsedRow.markModelWithNames
    assert(result.nonEmpty)
    assert(result.get.mark.value == "HONDA")
    assert(result.get.markName.value == "HONDA")
    assert(result.get.ruMarkName.value == "Хонда")
    assert(result.get.model.value == "PARTNER")
    assert(result.get.modelName.value == "PARTNER")
    assert(result.get.ruModelName.value == "Партнер")
  }

  test("markModelWithNames: no names and ru names") {
    val url: String = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS)

    parsedOffer.getOfferBuilder.getCarInfoBuilder.setMark("HONDA")
    parsedOffer.getOfferBuilder.getCarInfoBuilder.setModel("PARTNER")

    val result = parsedRow.markModelWithNames
    assert(result.nonEmpty)
    assert(result.get.mark.value == "HONDA")
    assert(result.get.markName.value == "HONDA")
    assert(result.get.ruMarkName.value == "HONDA")
    assert(result.get.model.value == "PARTNER")
    assert(result.get.modelName.value == "PARTNER")
    assert(result.get.ruModelName.value == "PARTNER")
  }

  test("markModelWithNames") {
    val url: String = testAvitoCarsUrl
    val parsedOffer = ParsedOffer.newBuilder()

    def parsedRow: ParsedRow = testRow(url, parsedOffer, category = Category.CARS)

    parsedOffer.getOfferBuilder.getCarInfoBuilder.setMark("HONDA")
    parsedOffer.getOfferBuilder.getCarInfoBuilder.getMarkInfoBuilder.setName("Honda")
    parsedOffer.getOfferBuilder.getCarInfoBuilder.getMarkInfoBuilder.setRuName("Хонда")
    parsedOffer.getOfferBuilder.getCarInfoBuilder.setModel("PARTNER")
    parsedOffer.getOfferBuilder.getCarInfoBuilder.getModelInfoBuilder.setName("Partner")
    parsedOffer.getOfferBuilder.getCarInfoBuilder.getModelInfoBuilder.setRuName("Партнер")

    val result = parsedRow.markModelWithNames
    assert(result.nonEmpty)
    assert(result.get.mark.value == "HONDA")
    assert(result.get.markName.value == "Honda")
    assert(result.get.ruMarkName.value == "Хонда")
    assert(result.get.model.value == "PARTNER")
    assert(result.get.modelName.value == "Partner")
    assert(result.get.ruModelName.value == "Партнер")
  }
}
