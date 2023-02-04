package ru.auto.salesman.service.price

import ru.auto.salesman.model._
import ru.auto.salesman.service.price.PriceExtractorSpec.{Input, InvalidInput, ValidInput}
import ru.auto.salesman.util.DateTimeInterval
import ru.auto.salesman.environment._
import ru.auto.salesman.service.PriceExtractor
import ru.auto.salesman.test.BaseSpec

trait PriceExtractorSpec extends BaseSpec {

  def newExtractor(input: Input*): PriceExtractor

  "PriceExtractor" should {
    "fail on bad input" in {
      val extractor = newExtractor(InvalidInput)
      extractor.price(ProductId.Fresh, now()).failure
    }

    val price = 200
    val todayNoon = now().withHourOfDay(12)
    val product = ProductId.Placement
    val input = ValidInput(
      product,
      wholeDay(todayNoon),
      200
    )

    "fail on not found product" in {
      newExtractor(input)
        .price(ProductId.Fresh, todayNoon)
        .failure
        .exception shouldBe a[NoSuchElementException]
    }

    "fail on not found time" in {
      newExtractor(input)
        .price(product, todayNoon.plusDays(2))
        .failure
        .exception shouldBe a[NoSuchElementException]
    }

    "return correct price" in {
      val otherProduct = ProductId.Fresh
      val price2 = 100
      val time2 = todayNoon
      val input2 = ValidInput(otherProduct, wholeDay(time2), price2)
      val price3 = 150
      val time3 = todayNoon.plusDays(1)
      val input3 = ValidInput(otherProduct, wholeDay(time3), price3)

      val extractor = newExtractor(input, input2, input3)
      extractor.price(product, todayNoon).success
      extractor.price(otherProduct, time2).success
      extractor.price(otherProduct, time3).success
    }

    "fail on ambiguous result" in {
      val inputAnotherPrice = input.copy(price = price * 2)
      newExtractor(input, inputAnotherPrice)
        .price(product, todayNoon)
        .failure
        .exception shouldBe an[IllegalStateException]

      val inputIntersectIntervals = input.copy(
        interval = DateTimeInterval(todayNoon.minusHours(1), todayNoon.plusHours(1))
      )
      newExtractor(input, inputIntersectIntervals)
        .price(product, todayNoon)
        .failure
        .exception shouldBe an[IllegalStateException]
    }
  }
}

object PriceExtractorSpec {
  sealed trait Input

  case object InvalidInput extends Input

  case class ValidInput(
      product: ProductId,
      interval: DateTimeInterval,
      price: Funds
  ) extends Input

}
