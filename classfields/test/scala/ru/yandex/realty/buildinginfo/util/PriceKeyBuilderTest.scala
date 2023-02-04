package ru.yandex.realty.buildinginfo.util

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.model.offer.{CategoryType, OfferType, PricingPeriod, Rooms}

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 27.10.17
  */
@RunWith(classOf[JUnitRunner])
class PriceKeyBuilderTest extends FlatSpec with Matchers {
  "PriceKeyBuilder" should "correct parse" in {
    val key: String = PriceKeyBuilder(OfferType.RENT, CategoryType.APARTMENT, PricingPeriod.PER_DAY, Rooms._2)
    key match {
      case PriceKeyBuilder((t, c, p, r)) =>
        t should be(OfferType.RENT)
        c should be(CategoryType.APARTMENT)
        p should be(PricingPeriod.PER_DAY)
        r should be(Rooms._2)
      case _ =>
        fail(s"cant' parse key $key")
    }
  }
}
