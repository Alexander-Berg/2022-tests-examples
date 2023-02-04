package ru.auto.salesman.service.impl.user

import ru.auto.api.ApiOfferModel.Offer
import ru.auto.salesman.client.VosClient
import ru.auto.salesman.model.user.{
  Analytics,
  ExperimentInfo,
  Price,
  PriceModifier,
  ProductPrice
}
import ru.auto.salesman.model.{
  AutoruUser,
  DeprecatedDomain,
  DeprecatedDomains,
  ExperimentId,
  Funds,
  Slave
}
import ru.auto.salesman.service.user.{PriceReducer, PriceService}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.ServiceModelGenerators
import ru.auto.salesman.util.{PriceRequestContext, RequestContext}
import ru.auto.salesman.util.AutomatedContext
import ru.auto.salesman.model.user.product.AutoruProduct

class PriceForScheduledProductCalculatorSpec
    extends BaseSpec
    with ServiceModelGenerators {
  import PriceForScheduledProductCalculatorSpec._

  implicit val rc: RequestContext = AutomatedContext("text")

  val priceService: PriceService = mock[PriceService]
  val priceReducer: PriceReducer = mock[PriceReducer]
  val vosClient: VosClient = mock[VosClient]

  val priceForScheduledProductCalculator =
    new PriceForScheduledProductCalculator(
      priceService,
      priceReducer,
      vosClient
    )

  "price for scheduled product calculator" should {
    val offer = offerGen().next

    "fail if there is no custom price from schedule and no autoApplyPrice in priceService.calculatePrices response" in {
      forAll(
        AutoAppliedProductGen,
        AutoruUserGen,
        OfferIdentityGen,
        ProductPriceGen.map(_.copy(productPriceInfo = None))
      ) { (autoAppliedProduct, user, offerId, productPrice) =>
        (priceService
          .calculateOnePrice(_: AutoruProduct, _: PriceRequestContext))
          .expects(autoAppliedProduct, *)
          .returningZ(productPrice)

        priceForScheduledProductCalculator
          .calculate(autoAppliedProduct, customPrice = None, user, offerId)
          .failure
          .exception shouldBe an[IllegalArgumentException]
      }
    }

    "reduce price using autoApplyPrice if no custom price is present in schedule and use experiment from it" in {
      forAll(
        AutoAppliedProductGen,
        AutoruUserGen,
        OfferIdentityGen,
        ProductPriceWithAutoAppliedPriceGen,
        patchedPriceGen(periodicalDiscountExclusion = None)
      ) { (autoAppliedProduct, user, offerId, productPrice, price) =>
        (vosClient.getOffer _).expects(offerId, Slave).returningZ(offer)

        val testPrice = addExperimentToProductPrice(productPrice)

        (priceService
          .calculateOnePrice(_: AutoruProduct, _: PriceRequestContext))
          .expects(autoAppliedProduct, *)
          .returningZ(testPrice)

        val autoAppliedPrice =
          testPrice.productPriceInfo.get.autoApplyPrice.get

        (priceReducer
          .reducePrice(
            _: Funds,
            _: AutoruProduct,
            _: AutoruUser,
            _: Some[Offer],
            _: Option[ExperimentInfo],
            _: Option[ExperimentId]
          ))
          .expects(
            autoAppliedPrice,
            autoAppliedProduct,
            user,
            Some(offer),
            testExperiment,
            testExperiment.flatMap(_.activeExperimentId)
          )
          .returningZ(price)

        priceForScheduledProductCalculator
          .calculate(autoAppliedProduct, customPrice = None, user, offerId)
          .success
          .value shouldBe testPrice.copy(
          price = Price(
            basePrice = autoAppliedPrice,
            effectivePrice = price.effectivePrice,
            prolongPrice = None,
            modifier = price.modifier,
            policyId = testPrice.price.policyId
          ),
          analytics = None
        )
      }
    }

    "reduce price using customPrice from schedule if it's present and don`t use experiment from autoApplyPrice" in {
      forAll(
        AutoAppliedProductGen,
        FundsGen,
        AutoruUserGen,
        OfferIdentityGen,
        ProductPriceWithAutoAppliedPriceGen,
        patchedPriceGen(periodicalDiscountExclusion = None)
      ) { (autoAppliedProduct, customPrice, user, offerId, productPrice, price) =>
        (vosClient.getOffer _).expects(offerId, Slave).returningZ(offer)

        val testPrice = addExperimentToProductPrice(productPrice)

        (priceService
          .calculateOnePrice(_: AutoruProduct, _: PriceRequestContext))
          .expects(autoAppliedProduct, *)
          .returningZ(testPrice)

        (priceReducer
          .reducePrice(
            _: Funds,
            _: AutoruProduct,
            _: AutoruUser,
            _: Option[Offer],
            _: Option[ExperimentInfo],
            _: Option[ExperimentId]
          ))
          .expects(
            customPrice,
            autoAppliedProduct,
            user,
            Some(offer),
            None,
            None
          )
          .returningZ(price)

        priceForScheduledProductCalculator
          .calculate(autoAppliedProduct, Some(customPrice), user, offerId)
          .success
          .value shouldBe testPrice.copy(
          price = Price(
            basePrice = customPrice,
            effectivePrice = price.effectivePrice,
            prolongPrice = None,
            modifier = price.modifier,
            policyId = testPrice.price.policyId
          ),
          analytics = None
        )
      }
    }

    "use periodical discount exclusion" in {
      forAll(
        AutoAppliedProductGen,
        FundsGen,
        AutoruUserGen,
        OfferIdentityGen,
        ProductPriceWithAutoAppliedPriceGen,
        patchedPriceGen(periodicalDiscountExclusion =
          Some(Analytics.UserExcludedFromDiscount("exclusion-id"))
        )
      ) { (autoAppliedProduct, customPrice, user, offerId, productPrice, price) =>
        (vosClient.getOffer _).expects(offerId, Slave).returningZ(offer)

        (priceService
          .calculateOnePrice(_: AutoruProduct, _: PriceRequestContext))
          .expects(autoAppliedProduct, *)
          .returningZ(productPrice)

        (priceReducer
          .reducePrice(
            _: Funds,
            _: AutoruProduct,
            _: AutoruUser,
            _: Option[Offer],
            _: Option[ExperimentInfo],
            _: Option[ExperimentId]
          ))
          .expects(customPrice, autoAppliedProduct, user, Some(offer), *, *)
          .returningZ(price)

        priceForScheduledProductCalculator
          .calculate(autoAppliedProduct, Some(customPrice), user, offerId)
          .success
          .value
          .analytics shouldBe Some(
          Analytics(Some(Analytics.UserExcludedFromDiscount("exclusion-id")))
        )
      }
    }
  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}

object PriceForScheduledProductCalculatorSpec {
  private val testExperiment = Some(ExperimentInfo(Some("test"), "box"))

  private def addExperimentToProductPrice(productPrice: ProductPrice) = {
    val modifier = productPrice.price.modifier match {
      case Some(m) =>
        m.copy(
          experimentInfo = testExperiment,
          appliedExperimentId = testExperiment.flatMap(_.activeExperimentId)
        )
      case _ =>
        PriceModifier(
          feature = None,
          experimentInfo = testExperiment,
          appliedExperimentId = testExperiment.flatMap(_.activeExperimentId)
        )
    }
    productPrice.copy(price = productPrice.price.copy(modifier = Some(modifier)))
  }
}
