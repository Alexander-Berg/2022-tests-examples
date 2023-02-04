package ru.auto.salesman.push

import ru.auto.salesman.model.{AutoRuOfferId, OfferCategories}
import ru.auto.salesman.test.BaseSpec

class DefaultPushPathResolverSpec extends BaseSpec {

  private val resolver = DefaultPushPathResolver("baseUrl")
  private val offerId = 1058223344

  "Default push path resolver" should {

    "resolve cars url" in {
      val id = AutoRuOfferId(offerId, OfferCategories.Cars)
      val result = resolver.resolve(id)
      result shouldBe "baseUrl/api/v2/cars/billing/partners/1438536/billing/autoru-1058223344?force=true"
    }

    "resolve commercial url" in {
      val id =
        AutoRuOfferId(offerId, OfferCategories.Commercial)
      val result = resolver.resolve(id)
      result shouldBe "baseUrl/api/v2/trucks/billing/partners/1438536/billing/autoru-1058223344?force=true"
    }

    "resolve commercial subcategory url" in {
      val id = AutoRuOfferId(offerId, OfferCategories.Bus)
      val result = resolver.resolve(id)
      result shouldBe "baseUrl/api/v2/trucks/billing/partners/1438536/billing/autoru-1058223344?force=true"
    }

    "resolve moto url" in {
      val id = AutoRuOfferId(offerId, OfferCategories.Moto)
      val result = resolver.resolve(id)
      result shouldBe "baseUrl/api/v2/moto/billing/partners/1438536/billing/autoru-1058223344?force=true"
    }

    "resolve moto subcategory url" in {
      val id =
        AutoRuOfferId(offerId, OfferCategories.Motorcycle)
      val result = resolver.resolve(id)
      result shouldBe "baseUrl/api/v2/moto/billing/partners/1438536/billing/autoru-1058223344?force=true"
    }
  }
}
