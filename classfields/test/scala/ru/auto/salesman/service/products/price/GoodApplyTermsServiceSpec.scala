package ru.auto.salesman.service.products.price

import cats.data.NonEmptyList
import org.joda.time.DateTime
import ru.auto.api.ApiOfferModel.Section.USED
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.salesman.dao.GoodsDao
import ru.auto.salesman.dao.GoodsDao.Record
import ru.auto.salesman.environment.IsoDateTimeFormatter
import ru.auto.salesman.model
import ru.auto.salesman.model.ProductId.{
  Fresh,
  Placement,
  Premium,
  PremiumOffer,
  Special,
  Turbo
}
import ru.auto.salesman.model.{
  ActivateDate,
  FirstActivateDate,
  Funds,
  GoodStatuses,
  OfferCurrencies,
  ProductId,
  RegionId
}
import ru.auto.salesman.service.GoodsDecider.ProductContext
import ru.auto.salesman.service.PriceEstimateService.PriceRequest
import ru.auto.salesman.service.impl.GoodsDeciderImpl.{GoodApplyTerms, ZeroPrice}
import ru.auto.salesman.service.impl.moisha.MoishaPriceExtractor
import ru.auto.salesman.service.impl.moisha.model.{
  MoishaInterval,
  MoishaPoint,
  MoishaProduct,
  MoishaRequestId,
  MoishaResponse
}
import ru.auto.salesman.service.{GoodsDaoProvider, GoodsDecider, PriceEstimateService}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.dao.gens.goodRecordGen
import ru.auto.salesman.test.data.MoishaTestDataGen
import ru.auto.salesman.test.model.gens.OfferModelGenerators._
import ru.auto.salesman.test.model.gens.autoruOfferIdGen
import ru.auto.salesman.util.{AutomatedContext, DateTimeInterval}

class GoodApplyTermsServiceSpec extends BaseSpec {

  private val priceEstimateService = mock[PriceEstimateService]
  private val goodsDaoProvider = mock[GoodsDaoProvider]
  private val goodDao = mock[GoodsDao]

  private val productsPriceService =
    new GoodApplyTermsService(priceEstimateService, goodsDaoProvider)

  import GoodApplyTermsServiceSpec._

  implicit val rc = AutomatedContext("test")

  "getPrice()" should {

    "get price for first placement only for main region" in {
      mockDao(goods = Nil)
      val response = priceResponse(firstPlacementPrice, Placement)
      mockExtraction(response.response)
      (priceEstimateService
        .estimate(_: PriceRequest))
        .expects(*)
        .returningZ(response)

      (priceEstimateService
        .estimateBatch(_: NonEmptyList[PriceRequest]))
        .expects(*)
        .never()

      val price = productsPriceService
        .getTerms(
          request,
          testRegionId,
          activateDate
        )
        .success
        .value

      price shouldBe GoodApplyTerms(firstPlacementPrice, 0, oneDayDeadline)
    }

    "get price for turbo only for main region" in {
      mockDao(Nil)
      val response = priceResponse(turboPrice, Turbo, duration = 7)
      mockExtraction(response.response)
      (priceEstimateService
        .estimate(_: PriceRequest))
        .expects(*)
        .returningZ(response)

      (priceEstimateService
        .estimateBatch(_: NonEmptyList[PriceRequest]))
        .expects(*)
        .never()

      val price = productsPriceService
        .getTerms(
          request.copy(context = ProductContext(Turbo)),
          testRegionId,
          activateDate
        )
        .success
        .value

      price shouldBe GoodApplyTerms(turboPrice, 0, sevenDaysDeadline)
    }

    "get zero price for experimental dealer ids" in {
      val previousPlacement = good(
        Placement,
        FirstActivateDate(jodaActivateDate.minusMonths(1)),
        offerBillingDeadline = jodaActivateDate.minusMonths(1).plusDays(1)
      )
      mockDao(List(previousPlacement))
      val response = priceResponse(firstPlacementPrice, Placement)
      mockExtraction(response.response)
      (priceEstimateService
        .estimate(_: PriceRequest))
        .expects(*)
        .returningZ(response)

      val price = productsPriceService
        .getTerms(
          request.copy(clientId = 1136),
          testRegionId,
          activateDate
        )
        .success
        .value

      price shouldBe GoodApplyTerms(firstPlacementPrice, 0, oneDayDeadline)
    }

    "get price for premium only for main region" in {
      mockDao(Nil)
      val response = priceResponse(premiumPrice, Premium)
      mockExtraction(response.response)
      (priceEstimateService
        .estimate(_: PriceRequest))
        .expects(*)
        .returningZ(response)

      (priceEstimateService
        .estimateBatch(_: NonEmptyList[PriceRequest]))
        .expects(*)
        .never()

      val price = productsPriceService
        .getTerms(
          request.copy(context = ProductContext(Premium)),
          testRegionId,
          activateDate
        )
        .success
        .value

      price shouldBe GoodApplyTerms(premiumPrice, 0, oneDayDeadline)
    }

    "get prolongation price for placement for all regions on second placement day" in {
      val previousPlacement = good(
        Placement,
        FirstActivateDate(jodaActivateDate.minusMonths(1)),
        offerBillingDeadline = jodaActivateDate.minusMonths(1).plusDays(1)
      )
      mockDao(List(previousPlacement))

      val response = priceResponse(placementProlongationPrice, Placement)
      (priceEstimateService
        .estimate(_: PriceRequest))
        .expects(*)
        .returningZ(response)

      mockExtraction(response.response)

      val moishaResponse = MoishaResponse(
        List(
          MoishaPoint(
            "asd",
            MoishaInterval(DateTime.now(), DateTime.now()),
            MoishaProduct(Placement.toString, Nil, placementProlongationPrice)
          )
        ),
        MoishaRequestId(priceRequestId = None)
      )

      (priceEstimateService
        .estimateBatch(_: NonEmptyList[PriceRequest]))
        .expects(*)
        .returningZ(
          Range(0, deliveryRegionsCount).map(_ => moishaResponse).toList
        )

      val price = productsPriceService
        .getTerms(
          request,
          testRegionId,
          activateDate
        )
        .success
        .value

      price shouldBe GoodApplyTerms(
        placementProlongationPrice,
        placementProlongationPrice * deliveryRegionsCount,
        oneDayDeadline
      )
    }

    "get duration from moisha but return customPrice if present" in {
      mockDao(Nil)
      val customPrice = 123
      val product = Turbo
      val response = priceResponse(customPrice, product, duration = 7)

      mockExtraction(response.response)

      (priceEstimateService
        .estimate(_: PriceRequest))
        .expects(*)
        .returningZ(response)

      val price = productsPriceService
        .getTerms(
          request.copy(
            customPrice = Some(customPrice),
            context = ProductContext(product)
          ),
          testRegionId,
          activateDate
        )
        .success
        .value

      price shouldBe GoodApplyTerms(
        customPrice,
        deliveryRegionsPrice = ZeroPrice,
        sevenDaysDeadline
      )
    }

    "make sure deliveryRegions = ZeroPrice if customPrice is present" in {
      mockDao(Nil)
      val customPrice = 123

      val moishaResponse = MoishaResponse(
        List(
          MoishaPoint(
            "asd",
            MoishaInterval(DateTime.now(), DateTime.now()),
            MoishaProduct(PremiumOffer.toString, Nil, customPrice)
          )
        ),
        MoishaRequestId(priceRequestId = None)
      )

      (priceEstimateService
        .estimateBatch(_: NonEmptyList[PriceRequest]))
        .expects(*)
        .returningZ(
          Range(0, deliveryRegionsCount).map(_ => moishaResponse).toList
        )

      // `Fresh` product type will trigger `deliveryRegionsPrice` calculation
      val product = Fresh
      val response = priceResponse(customPrice, product, duration = 7)

      mockExtraction(response.response)

      (priceEstimateService
        .estimate(_: PriceRequest))
        .expects(*)
        .returningZ(response)

      val price = productsPriceService
        .getTerms(
          request.copy(
            customPrice = Some(customPrice),
            context = ProductContext(product)
          ),
          testRegionId,
          activateDate
        )
        .success
        .value

      price shouldBe GoodApplyTerms(
        customPrice,
        deliveryRegionsPrice = ZeroPrice, // should zero out provided `customPrice`
        sevenDaysDeadline
      )
    }

    "get price only for main region if no delivery regions present" in {
      mockDao(Nil)
      val response = priceResponse(customPrice, Special)
      mockExtraction(response.response)
      (priceEstimateService
        .estimate(_: PriceRequest))
        .expects(*)
        .returningZ(response)

      (priceEstimateService
        .estimateBatch(_: NonEmptyList[PriceRequest]))
        .expects(*)
        .never()

      val price = productsPriceService
        .getTerms(
          request.copy(
            context = ProductContext(Special),
            offer = testOffer.toBuilder.clearDeliveryInfo().build()
          ),
          testRegionId,
          activateDate
        )
        .success
        .value

      price shouldBe GoodApplyTerms(customPrice, 0, oneDayDeadline)
    }

    "get zero price and duration from billed non-expired service" in {
      // jodaActivateDate == 2019-10-05T10:28:00+03:00
      val previousActivateDate =
        FirstActivateDate(DateTime.parse("2019-10-04T23:22:21+03:00"))
      val previousDeadline = DateTime.parse("2019-10-05T23:22:21+03:00")
      mockDao(List(good(Placement, previousActivateDate, previousDeadline)))
      val price = productsPriceService
        .getTerms(
          request,
          testRegionId,
          activateDate
        )
        .success
        .value
      price shouldBe GoodApplyTerms(
        mainRegionPrice = 0,
        deliveryRegionsPrice = 0,
        previousDeadline
      )
    }

    "get price and duration from moisha on prolong" in {
      val previousActivateDate =
        FirstActivateDate(jodaActivateDate.minusDays(1))
      val previousDeadline = jodaActivateDate
      mockDao(List(good(Premium, previousActivateDate, previousDeadline)))
      val response = priceResponse(premiumPrice, Premium)
      mockExtraction(response.response)
      (priceEstimateService
        .estimate(_: PriceRequest))
        .expects(*)
        .returningZ(response)

      val price = productsPriceService
        .getTerms(
          request.copy(context = ProductContext(Premium)),
          testRegionId,
          activateDate
        )
        .success
        .value

      price shouldBe GoodApplyTerms(premiumPrice, 0, oneDayDeadline)
    }
  }

  def mockExtraction(data: Array[Byte]): Unit =
    (priceEstimateService.extractor _)
      .expects(*)
      .anyNumberOfTimes()
      .returning(new MoishaPriceExtractor(data))

  private def mockDao(goods: List[Record]): Unit = {
    (goodsDaoProvider.chooseDao _).expects(*).returning(goodDao)
    (goodDao.get _).expects(*).returningT(goods)
  }

}

object GoodApplyTermsServiceSpec {

  val firstPlacementPrice = 600
  val turboPrice = 900
  val premiumPrice = 600
  val placementProlongationPrice = 250
  val customPrice = 360

  val testClientId = 20101

  val testOfferPriceRur = 1000000

  val testOffer =
    offerGen(
      autoruOfferIdGen(),
      Category.CARS,
      offerSectionGen = USED,
      PriceInfoGen = priceInfoGen(testOfferPriceRur.toDouble, OfferCurrencies.RUR)
    ).next

  private val jodaActivateDate = DateTime.parse("2019-10-05T10:28:00+03:00")
  val firstActivateDate = FirstActivateDate(jodaActivateDate)
  val activateDate = ActivateDate(jodaActivateDate)
  val oneDayDeadline: DateTime = DateTime.parse("2019-10-06T10:28:00+03:00")
  val sevenDaysDeadline: DateTime = DateTime.parse("2019-10-12T10:28:00+03:00")

  val request = GoodsDecider.Request(
    testClientId,
    testOffer,
    ProductContext(Placement),
    firstActivateDate,
    offerBillingDeadline = None,
    customPrice = None
  )

  val deliveryRegionsCount =
    testOffer.getDeliveryInfo.getDeliveryRegionsList.size()

  val expectedPrice = deliveryRegionsCount * 100

  val testRegionId: RegionId = RegionId(1)

  def good(
      product: ProductId,
      firstActivateDate: FirstActivateDate,
      offerBillingDeadline: DateTime
  ): Record =
    goodRecordGen(
      productGen = product,
      firstActivateDateGen = firstActivateDate,
      offerBillingDeadlineGen = offerBillingDeadline
    ).next

  val record = Record(
    1,
    123,
    "123",
    model.OfferCategories.Cars,
    Section.NEW,
    20101,
    ProductId.Placement,
    GoodStatuses.Active,
    createDate = jodaActivateDate,
    "",
    None,
    firstActivateDate,
    None,
    offerBillingDeadline = Some(DateTime.now.plusDays(1)),
    None,
    None
  )

  def priceResponse(price: Funds, product: ProductId, duration: Int = 1) =
    new PriceEstimateService.PriceResponse(
      testJsonPriceFor(product, price, duration),
      jodaActivateDate
    )

  lazy val testProduct: ProductId = ProductId.Special
  lazy val testGoodPrice: Funds = 2000

  lazy val today = DateTimeInterval.wholeDay(jodaActivateDate)

  private def testJsonPriceFor(
      product: ProductId,
      price: Funds,
      duration: Int
  ) =
    MoishaTestDataGen.testJsonPriceFor(
      ProductId.alias(product),
      price,
      IsoDateTimeFormatter.print(today.from),
      IsoDateTimeFormatter.print(today.to),
      duration
    )
}
