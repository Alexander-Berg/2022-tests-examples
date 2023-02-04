package ru.auto.salesman.service.impl.user.autoru.price.service

import ru.auto.salesman.model.user.product.{AutoruProduct, Products}
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.service.impl.user.AutoruPriceService.GoodsPriceRequest
import ru.auto.salesman.service.impl.user.ExperimentSelectServiceImpl
import ru.auto.salesman.service.impl.user.autoru.price.service.price.request.{
  PriceRequestCaller,
  PriceRequestCreator
}
import ru.auto.salesman.service.user.ExperimentSelectService
import ru.auto.salesman.service.user.PriceService.EnrichedPriceRequestContext
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.ServiceModelGenerators
import ru.auto.salesman.util.AutomatedContext

class PriceCalculatorSpec extends BaseSpec with ServiceModelGenerators {
  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
  implicit val rc = AutomatedContext("test")
  val priceRequestCreator: PriceRequestCreator = mock[PriceRequestCreator]
  val priceRequestCaller: PriceRequestCaller = mock[PriceRequestCaller]

  val experimentSelectService: ExperimentSelectService =
    new ExperimentSelectServiceImpl()

  val priceCalculator: PriceCalculator =
    new PriceCalculator(
      priceRequestCreator,
      priceRequestCaller,
      experimentSelectService
    )

  "calculateBasePrice" should {

    def copyAndSetAutoApply(
        goodsPriceRequest: GoodsPriceRequest,
        autoApply: Boolean,
        autoRuProduct: AutoruProduct,
        enrichedPriceRequestContext: EnrichedPriceRequestContext
    ): GoodsPriceRequest = {
      val changedContent =
        goodsPriceRequest.context.copy(
          autoApply = autoApply,
          experiment = experimentSelectService
            .getExperiment(
              enrichedPriceRequestContext.allUserExperiments,
              geoIds = enrichedPriceRequestContext.geoId.toSet,
              autoru = autoRuProduct
            )
            .success
            .value
            .flatMap(_.activeExperimentId)
            .getOrElse("")
        )
      goodsPriceRequest.copy(context = changedContent)
    }

    "call priceEstimateService second time if product is AutoApplied" in {
      val enrichedProductGenerated = EnrichedProductGen.next
      val autoAppliedProduct = AutoAppliedProductGen.next
      val priceRequestContext = GoodsPriceRequestGen.next
      val productInfo1 = ProductInfoGen.next
      val productInfo2 = ProductInfoGen.next

      val enrichedProduct = enrichedProductGenerated.copy(
        source = autoAppliedProduct,
        isFreeByQuota = false
      )

      val contextGenerated = EnrichedPriceRequestContextGen.next

      val goodsPriceRequest1 = copyAndSetAutoApply(
        goodsPriceRequest = priceRequestContext,
        autoApply = false,
        autoRuProduct =
          Products.withNameOrAlias[AutoruProduct](productInfo1.product.value),
        enrichedPriceRequestContext = contextGenerated
      )
      val goodsPriceRequest2 = copyAndSetAutoApply(
        goodsPriceRequest = priceRequestContext,
        autoApply = true,
        autoRuProduct =
          Products.withNameOrAlias[AutoruProduct](productInfo2.product.value),
        enrichedPriceRequestContext = contextGenerated
      )

      val resultContext =
        contextGenerated.copy(goodsPriceRequest = Some(goodsPriceRequest1))

      (priceRequestCreator.preparePriceRequest _)
        .expects(*, *)
        .returningZ(goodsPriceRequest1)

      (priceRequestCaller.estimateProductPrice _)
        .expects(*, goodsPriceRequest1)
        .returningZ(productInfo1)

      (priceRequestCaller.estimateProductPrice _)
        .expects(*, goodsPriceRequest2)
        .returningZ(productInfo2)

      val basePrice = priceCalculator
        .calculateBasePrice(enrichedProduct, resultContext)
        .success
        .value

      val autoApplyPrice = priceCalculator
        .calculateAutoApplyPrice(enrichedProduct, resultContext)
        .success
        .value

      basePrice shouldBe productInfo1
      autoApplyPrice.value shouldBe productInfo2
    }

    "call priceEstimateService once if product isn't AutoApplied" in {
      val enrichedProductGenerated = EnrichedProductGen.next
      val notAutoAppliedProduct = NotAutoAppliedProductGen.next
      val goodsPriceRequest = GoodsPriceRequestGen.next
      val enrichedProduct = enrichedProductGenerated.copy(
        source = notAutoAppliedProduct,
        isFreeByQuota = false
      )

      val productInfo = ProductInfoGen.next

      (priceRequestCreator.preparePriceRequest _)
        .expects(*, *)
        .returningZ(goodsPriceRequest)

      (priceRequestCaller.estimateProductPrice _)
        .expects(*, goodsPriceRequest)
        .returningZ(productInfo)

      val contextGenerated = EnrichedPriceRequestContextGen.next
      val context =
        contextGenerated.copy(goodsPriceRequest = Some(goodsPriceRequest))

      val basePrice = priceCalculator
        .calculateBasePrice(enrichedProduct, context)
        .success
        .value

      val autoApplyPrice = priceCalculator
        .calculateAutoApplyPrice(enrichedProduct, context)
        .success
        .value

      basePrice shouldBe productInfo
      autoApplyPrice shouldBe None
    }
  }
}
