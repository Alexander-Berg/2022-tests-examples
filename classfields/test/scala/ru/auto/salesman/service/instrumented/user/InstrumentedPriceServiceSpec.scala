package ru.auto.salesman.service.instrumented.user

import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.service.instrumented.user.InstrumentedPriceService.contextLogArgs
import ru.auto.salesman.test.model.gens.user.ServiceModelGenerators
import ru.auto.salesman.test.BaseSpec

class InstrumentedPriceServiceSpec extends BaseSpec with ServiceModelGenerators {

  "contextLogArgs()" should {

    "make logs for context with empty offer" in {
      forAll(priceRequestContextGen) { context =>
        val args = contextLogArgs(context.copy(autoruOffer = None)).toMap
        args.get("user") shouldBe context.user
        args.get("offerId") shouldBe context.offerId
        args.get("autoruOffer.id") shouldBe None
        args.get("category") shouldBe context.category
        args.get("section") shouldBe context.section
        args.get("geoId") shouldBe context.geoId
        args.get("applyMoneyFeature").value shouldBe context.applyMoneyFeature
      }
    }

    "make logs for context with filled offer" in {
      forAll(priceRequestContextGen, offerGen()) { (context, offer) =>
        val args = contextLogArgs(context.copy(autoruOffer = Some(offer))).toMap
        args.get("user") shouldBe context.user
        args.get("offerId") shouldBe context.offerId
        args.get("autoruOffer.id").value shouldBe offer.getId
        args.get("category") shouldBe context.category
        args.get("section") shouldBe context.section
        args.get("geoId") shouldBe context.geoId
        args.get("applyMoneyFeature").value shouldBe context.applyMoneyFeature
      }
    }

    "make logs for context with multiple non empty offers" in {
      forAll(PriceRequestContextOffersGen) { context =>
        val args = contextLogArgs(context).toMap
        args.get("user") shouldBe context.user
        args.get("applyMoneyFeature").value shouldBe context.applyMoneyFeature
        args.get("offers.id").value shouldBe context.offers
          .map(offer => offer.getId)
          .mkString(", ")
      }
    }
  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
