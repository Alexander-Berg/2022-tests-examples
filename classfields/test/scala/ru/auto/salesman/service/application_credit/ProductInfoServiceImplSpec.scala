package ru.auto.salesman.service.application_credit

import org.joda.time.DateTime
import org.scalamock.matchers.MockParameter
import ru.auto.salesman.model.ProductTariff.ApplicationCreditSingleTariffCarsUsed
import ru.auto.salesman.model.{
  ActiveProductNaturalKey,
  AutoruDealer,
  ProductDuration,
  ProductId
}
import ru.auto.salesman.products.ProductsOuterClass
import ru.auto.salesman.service.PriceExtractor.ProductInfo
import ru.auto.salesman.service.application_credit.ProductInfoServiceImplSpec.PriceEstimateServiceMockingHelper
import ru.auto.salesman.service.application_credit.ZioTestUtil.runTestWithFixedTime
import ru.auto.salesman.service.{
  DetailedClientSource,
  PriceEstimateService,
  PriceExtractor
}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.ProductGenerators
import ru.auto.salesman.util.money.Money.Kopecks
import ru.yandex.vertis.scalatest._

class ProductInfoServiceImplSpec
    extends BaseSpec
    with ProductGenerators
    with BetterEitherValues
    with PriceEstimateServiceMockingHelper {
  private val clientSource = mock[DetailedClientSource]
  private val priceEstimator = mock[PriceEstimateService]

  private val productInfoService =
    new ProductInfoServiceImpl(clientSource, priceEstimator)

  val dealer = AutoruDealer("dealer:123")

  val protoKey = ProductsOuterClass.ActiveProductNaturalKey.newBuilder
    .setDomain("application-credit")
    .setPayer(dealer.toString)
    .setProductType("access")
    .setTarget(TariffScope.CarsUsed.toProductTarget)
    .build

  val product = ActiveProductNaturalKey(protoKey)
    .map(key => activeProductGen.map(_.copy(key = key)))
    .right
    .value

  val client = buildDetailedClient(dealer.id)

  val productInfo = PriceExtractor.ProductInfo(
    ProductId.CreditApplication,
    Kopecks(777L),
    None,
    Some(ProductDuration.days(15)),
    None,
    appliedExperiment = None,
    policyId = None
  )

  "CreditTariffPriceService" should {

    "get price product info for access tariff" in {
      forAll(product) { p =>
        (clientSource.unsafeResolve _)
          .expects(dealer.id, false)
          .returningZ(client)

        mockGetProductInfo(
          instance = priceEstimator,
          in = *,
          out = productInfo
        )

        productInfoService
          .getAccessProductInfo(p.key, Some(p.createDate))
          .success
          .value shouldBe productInfo
      }
    }

    "get price product info for access tariff when date is empty" in {
      forAll(product) { p =>
        val now = DateTime.now()
        runTestWithFixedTime(now) {
          (clientSource.unsafeResolve _)
            .expects(dealer.id, false)
            .returningZ(client)

          mockGetProductInfo(
            instance = priceEstimator,
            in = *,
            out = productInfo
          )

          productInfoService
            .getAccessProductInfo(p.key, None)
            .map(_ shouldBe productInfo)
        }
      }
    }

    "get price product info for single tariff" in {
      mockGetProductInfo(
        instance = priceEstimator,
        in = *,
        out = productInfo
      )

      productInfoService
        .getSingleProductInfo(client, ApplicationCreditSingleTariffCarsUsed)
        .success
        .value shouldBe productInfo
    }
  }
}

object ProductInfoServiceImplSpec {

  trait PriceEstimateServiceMockingHelper { _: BaseSpec =>

    private val SyntheticPriceResponse =
      new PriceEstimateService.PriceResponse(new Array[Byte](0), DateTime.now())

    def mockGetProductInfo(
        instance: PriceEstimateService,
        in: MockParameter[PriceEstimateService.PriceRequest],
        out: ProductInfo
    ): Unit = {
      val priceExtractor = mock[PriceExtractor]

      (instance
        .estimate(_: PriceEstimateService.PriceRequest))
        .expects(in)
        .returningZ(SyntheticPriceResponse)

      (instance
        .extractor(_: PriceEstimateService.PriceResponse))
        .expects(SyntheticPriceResponse)
        .returning(priceExtractor)

      (priceExtractor
        .productInfo(_: ProductId, _: DateTime))
        .expects(*, *)
        .returningZ(out)
    }

  }

}
