package ru.yandex.realty.services.moderation.offerpredicate

import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.common.util.currency.Currency
import ru.yandex.realty.model.offer._
import ru.yandex.realty.persistence.cassandra.OfferSupport

/**
  * @see [[OfferPredicate]]
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class OfferPredicateTest extends FlatSpec with Matchers with OfferSupport {

  val descriptionContains: OfferPredicate = DescriptionContains("abc")

  val descriptionContainsParams: Seq[(String, Boolean)] = Seq(
    ("1abc2", true),
    ("ab1c2", false),
    ("", false),
    (null, false)
  )
  descriptionContainsParams.foreach {
    case (description, expectedResult) =>
      "DescriptionContains" should s"return $expectedResult for description=$description" in {
        val offer = buildOffer("1")
        offer.setDescription(description)
        val actualResult = descriptionContains(offer)
        actualResult shouldBe expectedResult
      }
  }

  val regionIs: OfferPredicate = RegionIs("Москва")

  val regionIsParams: Seq[(String, Boolean)] = Seq(
    ("Москва", true),
    ("москва", true),
    ("Мос-ква", true),
    ("Санкт-Петербург", false),
    ("", false),
    (null, false)
  )
  regionIsParams.foreach {
    case (address, expectedResult) =>
      "RegionIs" should s"return $expectedResult for address=$address" in {
        val offer = buildOffer("1")
        offer.getLocation.setCombinedAddress(address)
        val actualResult = regionIs(offer)
        actualResult shouldBe expectedResult
      }
  }

  val isFromVos: OfferPredicate = IsFromVos

  val isFromVosParams: Seq[(Boolean, Boolean)] = Seq(
    (false, false),
    (true, true)
  )
  isFromVosParams.foreach {
    case (fromVos, expectedResult) =>
      "IsFromVos" should s"return $expectedResult for fromVos=$fromVos" in {
        val offer = buildOffer("1")
        offer.setFromVos(fromVos)
        val actualResult = isFromVos(offer)
        actualResult shouldBe expectedResult
      }
  }

  val isFromCallCenter: OfferPredicate = IsFromCallCenter

  val isFromCallCenterParams: Seq[(Boolean, Boolean)] = Seq(
    (false, false),
    (true, true)
  )
  isFromCallCenterParams.foreach {
    case (callCenter, expectedResult) =>
      "IsFromCallCenter" should s"return $expectedResult for callCenter=$callCenter" in {
        val offer = buildOffer("1")
        offer.setCallCenter(callCenter)
        val actualResult = isFromCallCenter(offer)
        actualResult shouldBe expectedResult
      }
  }

  val isFromVerifier: OfferPredicate = IsFromVerifier

  val isFromVerifierParams: Seq[(SalesAgentCategory, Boolean)] = Seq(
    (SalesAgentCategory.VERIFIER, true),
    (SalesAgentCategory.UNKNOWN, false),
    (null, false)
  )
  isFromVerifierParams.foreach {
    case (category, expectedResult) =>
      "IsFromVerifier" should s"return $expectedResult for salesAgentCategory=$category" in {
        val offer = buildOffer("1")
        offer.getSaleAgent.setCategory(category)
        val actualResult = isFromVerifier(offer)
        actualResult shouldBe expectedResult
      }
  }

  val isActiveApartmentRentPerMonth: OfferPredicate = IsActiveApartmentRentPerMonth

  val isActiveApartmentRentPerMonthParams: Seq[(OfferType, CategoryType, PricingPeriod, Boolean)] = Seq(
    (OfferType.RENT, CategoryType.APARTMENT, PricingPeriod.PER_MONTH, true),
    (OfferType.SELL, CategoryType.APARTMENT, PricingPeriod.PER_MONTH, false),
    (null, CategoryType.APARTMENT, PricingPeriod.PER_MONTH, false),
    (OfferType.RENT, CategoryType.HOUSE, PricingPeriod.PER_MONTH, false),
    (OfferType.RENT, null, PricingPeriod.PER_MONTH, false),
    (OfferType.RENT, CategoryType.APARTMENT, PricingPeriod.PER_DAY, false)
  )
  isActiveApartmentRentPerMonthParams.foreach {
    case (offerType, categoryType, pricingPeriod, expectedResult) =>
      "IsActiveApartmentRentPerMonth" should
        s"return $expectedResult for ${(offerType, categoryType, pricingPeriod)}" in {
        val offer = buildOffer("1")
        offer.setOfferType(offerType)
        offer.setCategoryType(categoryType)
        val priceInfo = PriceInfo.createUnsafe(Currency.RUR, 1.0f, pricingPeriod, AreaUnit.ARE)
        val areaPrice = new AreaPrice(priceInfo, null)
        offer.getTransaction.setAreaPrice(areaPrice)
        val actualResult = isActiveApartmentRentPerMonth(offer)
        actualResult shouldBe expectedResult
      }
  }

  val andPredicate: OfferPredicate =
    OfferPredicate.and(DescriptionContains("1"), DescriptionContains("2"))

  val andPredicateParams: Seq[(String, Boolean)] = Seq(
    ("12", true),
    ("1", false),
    ("2", false),
    ("", false)
  )
  andPredicateParams.foreach {
    case (description, expectedResult) =>
      "and-operator" should s"return $expectedResult for description=$description" in {
        val offer = buildOffer("1")
        offer.setDescription(description)
        val actualResult = andPredicate(offer)
        actualResult shouldBe expectedResult
      }
  }

  val orPredicate: OfferPredicate =
    OfferPredicate.or(DescriptionContains("1"), DescriptionContains("2"))

  val orPredicateParams: Seq[(String, Boolean)] = Seq(
    ("12", true),
    ("1", true),
    ("2", true),
    ("", false)
  )
  orPredicateParams.foreach {
    case (description, expectedResult) =>
      "or-operator" should s"return $expectedResult for description=$description" in {
        val offer = buildOffer("1")
        offer.setDescription(description)
        val actualResult = orPredicate(offer)
        actualResult shouldBe expectedResult
      }
  }

  val notPredicate: OfferPredicate =
    OfferPredicate.not(DescriptionContains("1"))

  val notPredicateParams: Seq[(String, Boolean)] = Seq(
    ("1", false),
    ("2", true)
  )
  notPredicateParams.foreach {
    case (description, expectedResult) =>
      "not-operator" should s"return $expectedResult for description=$description" in {
        val offer = buildOffer("1")
        offer.setDescription(description)
        val actualResult = notPredicate(offer)
        actualResult shouldBe expectedResult
      }
  }
}
