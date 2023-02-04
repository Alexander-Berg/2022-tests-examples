package ru.auto.salesman.service.impl

import org.joda.time.DateTime
import ru.auto.api.ApiOfferModel.Section
import ru.auto.salesman.model.{
  AutoruDealer,
  FeatureCount,
  FeatureDiscount,
  FeatureDiscountTypes,
  FeatureUnits,
  Funds,
  ModifiedPrice,
  PriceModifierFeature,
  ProductId,
  PromocoderUser,
  RegionId,
  TransportCategory,
  UserTypes
}
import ru.auto.salesman.service.DealerProductPriceService.Price.{
  PlacementPrices,
  ServicePrice,
  ServicePrices
}
import ru.auto.salesman.service.DealerProductPriceService.{
  DealerProduct,
  PlacementPrice,
  PriceWithDiscount,
  ProductPrice,
  ServicePriceForRange
}
import ru.auto.salesman.service.PriceExtractor.ProductInfo
import ru.auto.salesman.service.PromocoderFeatureService.LoyaltyArgs
import ru.auto.salesman.service.impl.DeciderUtilsSpec.loyaltyDiscountFeature
import ru.auto.salesman.service.{
  PriceEstimateService,
  PriceExtractor,
  PromocoderFeatureService,
  PromocoderSource
}
import ru.auto.salesman.test.{BaseSpec, TestException}
import ru.auto.salesman.util.GeoUtils.RegMoscow
import ru.auto.salesman.util.money.Money.Kopecks

class DealerProductPriceServiceImplSpec extends BaseSpec {

  import DealerProductPriceServiceImplSpec._
  import DealerProductPriceServiceImpl._

  private val promocoder = mock[PromocoderSource]
  private val featureService = mock[PromocoderFeatureService]
  private val priceEstimator = mock[PriceEstimateService]

  private val service = new DealerProductPriceServiceImpl(
    promocoder,
    featureService,
    priceEstimator
  )

  "productsPrices" should {
    "return service prices with discount" in {
      val products = List(ProductId.Fresh, ProductId.Badge)
      val price = 10000L
      val priceWithDiscount = 8000L
      val discountPercent = 20

      val expected =
        products.zipWithIndex.map { case (product, i) =>
          val productPrice = price + i
          val productPriceWithDiscount = priceWithDiscount + i

          mockGetPrice(product, productPrice)
          mockDiscount(
            product,
            productPrice,
            productPriceWithDiscount,
            discountPercent
          )

          ProductPrice(
            product,
            ServicePrice(
              PriceWithDiscount(
                productPrice,
                productPriceWithDiscount,
                discountPercent
              )
            )
          )
        }

      mockPromocoder()

      val res = callProductPrices(products.map(DealerProduct.Service.apply))

      res should contain.theSameElementsAs(expected)
    }

    "return placement price with discount" in {
      val product = ProductId.Placement
      val placementPrice = 10000L
      val placementPriceWithDiscount = 8000L
      val prolongPrice = 3000L
      val prolongPriceWithDiscount = 1500L

      val ranges = List(1000L, 2000L)

      mockGetMultiplePrices(ranges.map(r => placementPrice + r))
      mockGetMultiplePrices(ranges.map(r => prolongPrice + r))

      val expectedPrices = ranges.map { rangeValue =>
        val rangePlacement = placementPrice + rangeValue
        val rangeProlong = prolongPrice + rangeValue
        val rangePlacementWithDiscount = placementPriceWithDiscount + rangeValue
        val rangeProlongWithDiscount = prolongPriceWithDiscount + rangeValue

        mockDiscount(
          product,
          rangePlacement,
          rangePlacementWithDiscount,
          20
        )
        mockDiscount(
          product,
          rangeProlong,
          rangeProlongWithDiscount,
          50
        )

        PlacementPrice(
          rangeValue,
          placementPrice =
            PriceWithDiscount(rangePlacement, rangePlacementWithDiscount, 20),
          prolongPrice = PriceWithDiscount(rangeProlong, rangeProlongWithDiscount, 50)
        )
      }

      val expected =
        List(
          ProductPrice(
            product,
            PlacementPrices(expectedPrices)
          )
        )

      mockPromocoder()

      val res = callProductPrices(List(DealerProduct.Placement(ranges)))

      res should contain.theSameElementsAs(expected)

    }

    "return error on promocoder error" in {
      val product = DealerProduct.Service(ProductId.Badge)

      mockPromocoderError()

      val res = service
        .productsPrices(
          List(product),
          testData.category,
          testData.section,
          testDealer,
          testData.region
        )
        .failure
        .exception

      res shouldBe an[TestException]
    }

    "return error on get price error" in {
      val product = DealerProduct.Service(ProductId.Badge)

      mockPromocoder()

      mockGetPriceError()

      val res = service
        .productsPrices(
          List(product),
          testData.category,
          testData.section,
          testDealer,
          testData.region
        )
        .failure
        .exception

      res shouldBe an[TestException]
    }

  }

  "multipleProductPrices" should {
    // essentially the same test as in productPrices, different mocks & method called
    "return correct prices for Placement" in {
      val product = ProductId.Placement
      val placementPrice = 10000L
      val placementPriceWithDiscount = 8000L
      val prolongPrice = 3000L
      val prolongPriceWithDiscount = 1500L

      val ranges = List(1000L, 2000L)

      val placementPricesWithIds = ranges.map { r =>
        val price = placementPrice + r
        PriceRequestIdWithPrice(Some(s"${product}__${r}__1"), price)
      }
      val prolongPricesWithIds = ranges.map { r =>
        val price = prolongPrice + r
        PriceRequestIdWithPrice(Some(s"${product}__${r}__2"), price)
      }

      mockGetMultiplePricesUnordered(
        placementPricesWithIds ++ prolongPricesWithIds
      )

      val expectedPrices = ranges.map { rangeValue =>
        val rangePlacement = placementPrice + rangeValue
        val rangeProlong = prolongPrice + rangeValue
        val rangePlacementWithDiscount = placementPriceWithDiscount + rangeValue
        val rangeProlongWithDiscount = prolongPriceWithDiscount + rangeValue

        mockDiscount(
          product,
          rangePlacement,
          rangePlacementWithDiscount,
          20
        )
        mockDiscount(
          product,
          rangeProlong,
          rangeProlongWithDiscount,
          50
        )

        PlacementPrice(
          rangeValue,
          placementPrice =
            PriceWithDiscount(rangePlacement, rangePlacementWithDiscount, 20),
          prolongPrice = PriceWithDiscount(rangeProlong, rangeProlongWithDiscount, 50)
        )
      }

      val expected =
        List(
          ProductPrice(
            product,
            PlacementPrices(expectedPrices)
          )
        )

      mockPromocoder()

      val res =
        callProductPricesWithRanges(List(DealerProduct.Placement(ranges)))

      res should contain.theSameElementsAs(expected)

    }

    "return correct prices for Service with range" in {
      val products = List(ProductId.Fresh, ProductId.Badge)
      val price = 10000L
      val priceWithDiscount = 8000L
      val discountPercent = 20

      val ranges = List(1000L, 2000L)
      val priceResponses = products.zipWithIndex.flatMap { case (product, i) =>
        ranges.map { range =>
          val productPrice = price + range + i
          PriceRequestIdWithPrice(
            Some(s"${product}__${range}__empty"),
            productPrice
          )
        }
      }
      mockGetMultiplePricesUnordered(priceResponses)

      val expected =
        products.zipWithIndex.map { case (product, i) =>
          val rangedPrices = ranges.map { range =>
            val productPrice = price + range + i
            val productPriceWithDiscount = priceWithDiscount + range + i

            mockDiscount(
              product,
              productPrice,
              productPriceWithDiscount,
              discountPercent
            )
            ServicePriceForRange(
              range,
              PriceWithDiscount(
                productPrice,
                productPriceWithDiscount,
                discountPercent
              )
            )
          }
          ProductPrice(
            product,
            ServicePrices(
              rangedPrices
            )
          )

        }

      mockPromocoder()
      val services = products.map { product =>
        DealerProduct.ServiceWithRanges(product, ranges)

      }

      val res = callProductPricesWithRanges(services)

      res should contain.theSameElementsAs(expected)

    }

  }

  "discountPercent" should {
    "return 0 if no features applied" in {
      discountPercent(10000, ModifiedPrice(10000, List.empty)) shouldBe 0
    }

    "return percent from feature, if only one discount feature used" in {
      val newPrice = ModifiedPrice(
        1000,
        List(
          PriceModifierFeature(
            loyaltyDiscountFeature,
            FeatureCount(1, FeatureUnits.Items),
            100
          )
        )
      )
      discountPercent(10000, newPrice) shouldBe 50
    }

    "calculate percent from price if more than one feature used" in {
      val newPrice = ModifiedPrice(
        1000,
        List(
          PriceModifierFeature(
            loyaltyDiscountFeature,
            FeatureCount(1, FeatureUnits.Items),
            100
          ),
          PriceModifierFeature(
            loyaltyDiscountFeature,
            FeatureCount(1, FeatureUnits.Items),
            100
          )
        )
      )
      discountPercent(10000, newPrice) shouldBe 10
    }

  }

  "calculatePercent" should {
    "round up percent if > .5" in {
      calculatePercent(300, 200) shouldBe 67
    }

    "round down percent if < .5" in {
      calculatePercent(300, 100) shouldBe 33
    }
  }

  private def mockPromocoder(): Unit =
    (promocoder.getFeaturesForUser _)
      .expects(PromocoderUser(testDealer.id, UserTypes.ClientUser))
      .returningZ(testFeatures)

  private def mockPromocoderError(): Unit =
    (promocoder.getFeaturesForUser _)
      .expects(PromocoderUser(testDealer.id, UserTypes.ClientUser))
      .throwingZ(new TestException())

  private def mockDiscount(
      product: ProductId,
      price: Funds,
      priceWithDiscount: Funds,
      percent: Int
  ): Unit = {
    val features =
      if (price == priceWithDiscount) List.empty
      else
        testFeatures.map(feature =>
          PriceModifierFeature(
            feature.copy(payload =
              feature.payload.copy(discount =
                Some(FeatureDiscount(FeatureDiscountTypes.Percent, percent))
              )
            ),
            FeatureCount(1, FeatureUnits.Items),
            price - priceWithDiscount
          )
        )

    (featureService.modifyPriceWithLoyaltyDiscount _)
      .expects(testFeatures, product, price, Some(testData.loyaltyArgs))
      .returningZ(ModifiedPrice(priceWithDiscount, features))
  }

  private def mockGetPrice(product: ProductId, price: Funds): Unit = {
    val priceExtractor = mock[PriceExtractor]

    (priceEstimator
      .estimate(_: PriceEstimateService.PriceRequest))
      .expects(*)
      .returningZ(SyntheticPriceResponse)

    (priceEstimator
      .extractor(_: PriceEstimateService.PriceResponse))
      .expects(SyntheticPriceResponse)
      .returning(priceExtractor)

    (priceExtractor
      .productInfo(_: ProductId, _: DateTime))
      .expects(product, *)
      .returningZ(productInfo(price))

  }

  private def mockGetMultiplePrices(prices: List[Funds]): Unit =
    (priceEstimator
      .getMultiplePricesInSameOrder(_: List[PriceEstimateService.PriceRequest]))
      .expects(*)
      .returningZ(prices)

  private def mockGetMultiplePricesUnordered(
      priceRequestIdsWithPrices: Seq[PriceRequestIdWithPrice]
  ): Unit =
    (priceEstimator
      .getMultiplePrices(_: List[PriceEstimateService.PriceRequest]))
      .expects(*)
      .returningZ(priceRequestIdsWithPrices)

  private def mockGetPriceError(): Unit = (priceEstimator
    .estimate(_: PriceEstimateService.PriceRequest))
    .expects(*)
    .throwingZ(new TestException())

  private def callProductPrices(products: List[DealerProduct]) = service
    .productsPrices(
      products,
      testData.category,
      testData.section,
      testDealer,
      testData.region
    )
    .success
    .value

  private def callProductPricesWithRanges(
      products: List[DealerProduct]
  ): Seq[ProductPrice] = service
    .productPricesWithRanges(
      products,
      testData.category,
      testData.section,
      testDealer,
      testData.region
    )
    .success
    .value

}

object DealerProductPriceServiceImplSpec {

  private val testFeatures = List(loyaltyDiscountFeature)

  private val SyntheticPriceResponse =
    new PriceEstimateService.PriceResponse(new Array[Byte](0), DateTime.now())

  private def productInfo(price: Funds): ProductInfo = ProductInfo(
    ProductId.Placement,
    Kopecks(price),
    prolongPrice = None,
    duration = None,
    tariff = None,
    appliedExperiment = None,
    policyId = None
  )

  private val testData = TestData()

  private val testDealer = AutoruDealer(1)

  final case class TestData(
      category: TransportCategory,
      section: Section,
      region: RegionId
  ) {

    val loyaltyArgs: LoyaltyArgs =
      LoyaltyArgs(category.protoParent, section, region)
  }

  object TestData {

    def apply(): TestData =
      TestData(TransportCategory.Cars, Section.USED, RegMoscow)
  }

}
