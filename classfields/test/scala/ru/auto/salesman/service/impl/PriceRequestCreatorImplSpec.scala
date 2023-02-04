package ru.auto.salesman.service.impl

import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalatest.Inspectors
import ru.auto.api.ApiOfferModel.{Category, Offer, Section}
import ru.auto.api.CarsModel.CarInfo
import ru.auto.api.MotoModel.MotoInfo
import ru.auto.salesman.client.VosClient
import ru.auto.salesman.dao.ClientDao.{Filter, ForId}
import ru.auto.salesman.dao.{ClientDao, PriorityPlacementPeriodDao}
import ru.auto.salesman.environment.{today, wholeDay}
import ru.auto.salesman.model.Product.ProductPaymentStatus.NeedPayment
import ru.auto.salesman.model.ProductId.{Call, CreditApplication, SingleCreditApplication}
import ru.auto.salesman.model.UniqueProductType.{
  ApplicationCreditAccess,
  ApplicationCreditSingle
}
import ru.auto.salesman.model.offer.{AutoruOfferId, OfferIdentity}
import ru.auto.salesman.model.{
  ActiveProductNaturalKey,
  CityId,
  Client,
  ClientId,
  ClientStatuses,
  DbInstance,
  DetailedClient,
  OfferCategories,
  OfferCategory,
  OfferMark,
  Product,
  ProductId,
  ProductTariff,
  QuotaEntities,
  QuotaRequest,
  RegionId,
  Slave,
  TariffType,
  TariffTypes,
  UniqueProductType
}
import ru.auto.salesman.service.PriceEstimateService.PriceRequest
import ru.auto.salesman.service.PriceEstimateService.PriceRequest._
import ru.auto.salesman.service.PriceRequestCreator.CreditApplicationCategorySectionParsingError
import ru.auto.salesman.service.client.{ClientService, ClientServiceImpl}
import ru.auto.salesman.service.impl.PriceRequestCreatorImplSpec._
import ru.auto.salesman.service.{
  DealerFeatureService,
  DealerMarksService,
  PriceRequestCreator
}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.dao.gens.clientDetailsGen
import ru.auto.salesman.util.DateTimeInterval
import ru.yandex.vertis.util.time.DateTimeUtil

class PriceRequestCreatorImplSpec extends BaseSpec {

  val clientDao: ClientDao = mock[ClientDao]
  val clientService: ClientService = new ClientServiceImpl(clientDao)
  val vosClient: VosClient = mock[VosClient]

  val priorityPlacementPeriodDao: PriorityPlacementPeriodDao =
    mock[PriorityPlacementPeriodDao]
  val dealerFeatureService: DealerFeatureService = mock[DealerFeatureService]

  private val getClient = toMockFunction1 {
    clientDao.get(_: Filter)
  }

  private val getOffer = toMockFunction2 {
    vosClient.getOptOffer(_: OfferIdentity, _: DbInstance)
  }

  private val featureUsePriorityPlacement =
    (dealerFeatureService.callPricePriorityPlacementRatioEnabled _)
      .expects()

  private val featureGetPriorityPlacementByPeriods =
    (dealerFeatureService.priorityPlacementByPeriodsEnabled _)
      .expects()

  private val getPriorityPlacementByPeriods = toMockFunction2 {
    priorityPlacementPeriodDao.hasPriorityPlacement(_: ClientId, _: DateTime)
  }

  private val dealerMarks = mock[DealerMarksService]

  private val getDealerMarks =
    (dealerMarks
      .getMarks(_: ClientId, _: Category, _: Option[Section]))
      .expects(TestClientId, Category.CARS, Some(Section.NEW))

  val creator = new PriceRequestCreatorImpl(
    clientService,
    vosClient,
    priorityPlacementPeriodDao,
    dealerFeatureService,
    dealerMarks
  )

  "forAuctionProduct(product, client, dateTime = None)" should {
    "get price request with priority placement by periods" in {
      getDealerMarks.returningZ(TestMarks)

      getPriorityPlacementByPeriods
        .expects(TestClientId, *)
        .returningZ(true)

      featureGetPriorityPlacementByPeriods
        .returning(true)

      featureUsePriorityPlacement
        .returning(true)

      val result =
        creator
          .forPaidCallProduct(
            Call,
            client,
            offerId = None,
            dateTime = None,
            withPriorityPlacement = true
          )
          .success
          .value

      val expectedResult =
        priceRequest(
          product = Call,
          OfferCategories.Cars,
          Section.NEW,
          marks = TestMarks,
          hasPriorityPlacement = true,
          interval = today()
        )

      priceRequestsEqualIgnoreId(result, expectedResult) shouldBe true
    }

    "get price request without priority placement by periods" in {
      getDealerMarks.returningZ(TestMarks)

      getPriorityPlacementByPeriods
        .expects(TestClientId, *)
        .returningZ(false)

      featureGetPriorityPlacementByPeriods
        .returning(true)

      featureUsePriorityPlacement
        .returning(true)
        .never()

      val result =
        creator
          .forPaidCallProduct(
            Call,
            client,
            offerId = None,
            dateTime = None,
            withPriorityPlacement = true
          )
          .success
          .value

      val expectedResult =
        priceRequest(
          product = Call,
          OfferCategories.Cars,
          Section.NEW,
          marks = TestMarks,
          hasPriorityPlacement = false,
          interval = today()
        )

      priceRequestsEqualIgnoreId(result, expectedResult) shouldBe true
    }

    "get price request with priority placement by periods but disabled by feature" in {
      getDealerMarks.returningZ(TestMarks)

      getPriorityPlacementByPeriods
        .expects(TestClientId, *)
        .returningZ(true)

      featureGetPriorityPlacementByPeriods
        .returning(true)

      featureUsePriorityPlacement
        .returning(false)

      val result =
        creator
          .forPaidCallProduct(
            Call,
            client,
            offerId = None,
            dateTime = None,
            withPriorityPlacement = true
          )
          .success
          .value

      val expectedResult =
        priceRequest(
          product = Call,
          OfferCategories.Cars,
          Section.NEW,
          marks = TestMarks,
          hasPriorityPlacement = false,
          interval = today()
        )

      priceRequestsEqualIgnoreId(result, expectedResult) shouldBe true
    }

    "get price request with priority placement from client property (not by periods)" in {
      val clientWithPriorityPlacement =
        client.copy(priorityPlacement = true)

      getDealerMarks.returningZ(TestMarks)

      featureGetPriorityPlacementByPeriods
        .returning(false)

      featureUsePriorityPlacement
        .returning(true)

      val result =
        creator
          .forPaidCallProduct(
            Call,
            clientWithPriorityPlacement,
            offerId = None,
            dateTime = None,
            withPriorityPlacement = true
          )
          .success
          .value

      val expectedResult =
        priceRequest(
          product = Call,
          OfferCategories.Cars,
          Section.NEW,
          marks = TestMarks,
          hasPriorityPlacement = true,
          interval = today()
        )

      priceRequestsEqualIgnoreId(result, expectedResult) shouldBe true
    }

    "get price request with priority placement from client property = false" in {
      val clientWithoutPriorityPlacement =
        client.copy(priorityPlacement = false)

      getDealerMarks.returningZ(TestMarks)

      featureGetPriorityPlacementByPeriods
        .returning(false)

      featureUsePriorityPlacement
        .returning(true)
        .never()

      val result =
        creator
          .forPaidCallProduct(
            Call,
            clientWithoutPriorityPlacement,
            offerId = None,
            dateTime = None,
            withPriorityPlacement = true
          )
          .success
          .value

      val expectedResult =
        priceRequest(
          product = Call,
          OfferCategories.Cars,
          Section.NEW,
          marks = TestMarks,
          hasPriorityPlacement = false,
          interval = today()
        )

      priceRequestsEqualIgnoreId(result, expectedResult) shouldBe true
    }

    "get price request with interval" in {
      getDealerMarks.returningZ(TestMarks)

      getPriorityPlacementByPeriods
        .expects(TestClientId, *)
        .returningZ(true)

      featureGetPriorityPlacementByPeriods
        .returning(true)

      featureUsePriorityPlacement
        .returning(true)

      val dateTime = DateTime.now()

      val result =
        creator
          .forPaidCallProduct(
            Call,
            client,
            offerId = None,
            Some(dateTime),
            withPriorityPlacement = true
          )
          .success
          .value

      val expectedResult =
        priceRequest(
          product = Call,
          OfferCategories.Cars,
          Section.NEW,
          marks = TestMarks,
          hasPriorityPlacement = true,
          interval = wholeDay(dateTime)
        )

      priceRequestsEqualIgnoreId(result, expectedResult) shouldBe true
    }

    "get price request by forCall alias-method" in {
      getDealerMarks.returningZ(TestMarks)

      getPriorityPlacementByPeriods
        .expects(TestClientId, *)
        .returningZ(true)

      featureGetPriorityPlacementByPeriods
        .returning(true)

      featureUsePriorityPlacement
        .returning(true)

      val dateTime = DateTime.now()

      val result =
        creator
          .forCall(client, None, Some(dateTime), withPriorityPlacement = true)
          .success
          .value

      val expectedResult =
        priceRequest(
          product = Call,
          OfferCategories.Cars,
          Section.NEW,
          marks = TestMarks,
          hasPriorityPlacement = true,
          interval = wholeDay(dateTime)
        )

      priceRequestsEqualIgnoreId(result, expectedResult) shouldBe true
    }

    "get price request by forCall don't check priority placement if it not required" in {
      getDealerMarks.returningZ(TestMarks)

      featureGetPriorityPlacementByPeriods
        .never()

      featureUsePriorityPlacement
        .never()

      val dateTime = DateTime.now()

      val result =
        creator
          .forCall(client, None, Some(dateTime), withPriorityPlacement = false)
          .success
          .value

      val expectedResult =
        priceRequest(
          product = Call,
          OfferCategories.Cars,
          Section.NEW,
          marks = TestMarks,
          hasPriorityPlacement = false,
          interval = wholeDay(dateTime)
        )

      priceRequestsEqualIgnoreId(result, expectedResult) shouldBe true
    }

    "get price request by forMatchApplication alias-method. priority placement always = false" in {
      featureUsePriorityPlacement
        .never()
      featureGetPriorityPlacementByPeriods
        .never()
      getDealerMarks.returningZ(TestMarks)

      val dateTime = DateTime.now()
      val product = ProductId.MatchApplicationCarsNew

      val result =
        creator
          .forMatchApplication(product, client, Some(dateTime))
          .success
          .value

      val expectedResult =
        priceRequest(
          product,
          OfferCategories.Cars,
          Section.NEW,
          marks = TestMarks,
          hasPriorityPlacement = false,
          interval = wholeDay(dateTime)
        )

      priceRequestsEqualIgnoreId(result, expectedResult) shouldBe true
    }
  }

  "forProduct" should {

    val detailedClientMock = mock[DetailedClient]

    "fail if product with invalid target field received" in {

      featureUsePriorityPlacement
        .never()
      featureGetPriorityPlacementByPeriods
        .never()
      getDealerMarks.never()

      Inspectors.forEvery(List("cars", "cars:use", "cars:new:premium")) { targetField =>
        val product = productForKey(
          TestProductKey.copy(target = targetField)
        )

        PriceRequestCreator
          .forProductType(
            ApplicationCreditAccess,
            product.key,
            detailedClientMock,
            product.createDate,
            product.tariff
          )
          .failure
          .exception shouldBe a[CreditApplicationCategorySectionParsingError]
      }
    }

    "return proper price request" in {
      forAll(
        clientDetailsGen(clientIdGen = TestClientId),
        Gen.oneOf(Section.NEW, Section.USED)
      ) { (testClient, section) =>
        featureUsePriorityPlacement
          .never()
        featureGetPriorityPlacementByPeriods
          .never()
        getDealerMarks.never()

        val category = OfferCategories.Cars

        val expectedContext =
          AccessCreditApplicationContext(
            testClient.regionId,
            testClient.cityId,
            category,
            section
          )

        val targetString =
          s"${category.toString}:${section.name.toLowerCase}"

        val expectedRequest =
          PriceRequest(
            DefaultClientOffer,
            expectedContext,
            CreditApplication,
            today()
          )

        val product =
          productForKey(TestProductKey.copy(target = targetString))

        val actualRequest =
          PriceRequestCreator
            .forProductType(
              ApplicationCreditAccess,
              product.key,
              testClient,
              product.createDate,
              product.tariff
            )
            .success
            .value

        priceRequestsEqualIgnoreId(actualRequest, expectedRequest) shouldBe true
      }
    }

    "return proper price request with tariff in context for product type != ApplicationCreditAccess" in {
      forAll(clientDetailsGen(clientIdGen = TestClientId)) { testClient =>
        featureUsePriorityPlacement
          .never()
        featureGetPriorityPlacementByPeriods
          .never()
        getDealerMarks.never()

        val product =
          productWithTariff(
            TestProductKey.copy(uniqueProductType =
              UniqueProductType.ApplicationCreditSingle
            ),
            ru.auto.salesman.model.ProductTariff.ApplicationCreditSingleTariffCarsNew
          )

        val expectedContext =
          ClientContext(
            clientRegionId = testClient.regionId,
            offerPlacementDay = None,
            productTariff = product.tariff
          )

        val expectedRequest =
          PriceRequest(
            offer = DefaultClientOffer,
            context = expectedContext,
            product = SingleCreditApplication,
            interval = today()
          )

        val actualRequest =
          PriceRequestCreator
            .forProductType(
              ApplicationCreditSingle,
              product.key,
              testClient,
              product.createDate,
              product.tariff
            )
            .success
            .value

        priceRequestsEqualIgnoreId(actualRequest, expectedRequest) shouldBe true
      }
    }
  }

  "forQuota(quotaRequest, tariff, activateInterval)" should {
    "return priceRequest for quotaRequest with moto quota" in {
      val product = ProductId.QuotaPlacementMoto

      val quotaRequest = QuotaRequest(
        TestClientId,
        product,
        dealerQuotaRequestSettings,
        from = DateTime.now(),
        regionId = None
      )

      val activateInterval = wholeDay(DateTime.now())

      getClient
        .expects(ForId(TestClientId))
        .returningZ(List(client))

      featureGetPriorityPlacementByPeriods
        .never()

      featureUsePriorityPlacement
        .never()

      getDealerMarks.never()

      val result =
        creator
          .forQuota(quotaRequest, tariff = None, activateInterval)
          .success
          .value

      val expectedResult =
        priceRequest(
          product,
          dealerQuotaRequestSettings.size,
          client.regionId,
          marks = Seq.empty,
          tariff = None,
          activateInterval
        )

      result shouldBe expectedResult
    }

    "return priceRequest for quotaRequest with tariffType" in {
      val product = ProductId.QuotaPlacementMoto

      val quotaRequest = QuotaRequest(
        TestClientId,
        product,
        dealerQuotaRequestSettings,
        from = DateTime.now(),
        regionId = None
      )

      val activateInterval = wholeDay(DateTime.now())
      val tariffType = TariffTypes.LuxaryMsk

      getClient
        .expects(ForId(TestClientId))
        .returningZ(List(client))

      featureGetPriorityPlacementByPeriods
        .never()

      featureUsePriorityPlacement
        .never()

      getDealerMarks.never()

      val result =
        creator
          .forQuota(quotaRequest, Some(tariffType), activateInterval)
          .success
          .value

      val expectedResult =
        priceRequest(
          product,
          dealerQuotaRequestSettings.size,
          client.regionId,
          marks = Seq.empty,
          Some(tariffType),
          activateInterval
        )

      result shouldBe expectedResult
    }

    "return priceRequest for quotaRequest with cars new quota" in {
      val product = ProductId.QuotaPlacementCarsNew

      val quotaRequest = QuotaRequest(
        TestClientId,
        product,
        dealerQuotaRequestSettings,
        from = DateTime.now(),
        regionId = None
      )

      val activateInterval = wholeDay(DateTime.now())

      getClient
        .expects(ForId(TestClientId))
        .returningZ(List(client))

      featureGetPriorityPlacementByPeriods
        .never()

      featureUsePriorityPlacement
        .never()

      getDealerMarks.returningZ(TestMarks)

      val result =
        creator
          .forQuota(quotaRequest, tariff = None, activateInterval)
          .success
          .value

      val expectedResult =
        priceRequest(
          product,
          dealerQuotaRequestSettings.size,
          client.regionId,
          marks = TestMarks,
          tariff = None,
          activateInterval
        )

      result shouldBe expectedResult
    }

    "return priceRequest for quotaRequest with parts quota" in {
      val product = ProductId.QuotaPriority

      val quotaRequest = QuotaRequest(
        TestClientId,
        product,
        partsQuotaRequestSettings,
        from = DateTime.now(),
        Some(TestRegionId)
      )

      val activateInterval = wholeDay(DateTime.now())

      featureGetPriorityPlacementByPeriods
        .never()

      featureUsePriorityPlacement
        .never()

      getDealerMarks.never()

      val result =
        creator
          .forQuota(quotaRequest, tariff = None, activateInterval)
          .success
          .value

      val expectedResult =
        partsPriceRequest(
          product,
          partsQuotaRequestSettings.size,
          TestRegionId,
          activateInterval
        )

      priceRequestsEqualIgnoreId(result, expectedResult) shouldBe true
    }
  }

  "getAuctionClientOffer" should {
    "return None on empty offerId" in {
      featureGetPriorityPlacementByPeriods
        .never()

      featureUsePriorityPlacement
        .never()

      getDealerMarks.never()

      creator.getPaidCallOffer(offerId = None).success.value shouldBe None
    }

    "return AuctionClientOffer with car offer data" in {
      val offerId = AutoruOfferId("123-fff")

      featureGetPriorityPlacementByPeriods
        .never()

      featureUsePriorityPlacement
        .never()

      getDealerMarks.never()

      getOffer
        .expects(offerId, Slave)
        .returningZ {
          Some(carOffer)
        }

      val expected =
        PaidCallClientOffer(
          OfferCategories.Cars,
          Section.NEW,
          Some("OPEL"),
          Some("ASTRA")
        )

      creator
        .getPaidCallOffer(offerId = Some(offerId))
        .success
        .value shouldBe Some(expected)
    }

    "return AuctionClientOffer with moto offer data" in {
      val offerId = AutoruOfferId("123-fff")

      featureGetPriorityPlacementByPeriods
        .never()

      featureUsePriorityPlacement
        .never()

      getDealerMarks.never()

      getOffer
        .expects(offerId, Slave)
        .returningZ {
          Some(motoOffer)
        }

      val expected =
        PaidCallClientOffer(
          OfferCategories.Moto,
          Section.NEW,
          mark = None,
          model = None
        )

      creator
        .getPaidCallOffer(offerId = Some(offerId))
        .success
        .value shouldBe Some(expected)
    }

    "return None on empty offer data" in {
      val offerId = AutoruOfferId("123-fff")

      featureGetPriorityPlacementByPeriods
        .never()

      featureUsePriorityPlacement
        .never()

      getDealerMarks.never()

      getOffer
        .expects(offerId, Slave)
        .returningZ {
          None
        }

      creator
        .getPaidCallOffer(offerId = Some(offerId))
        .success
        .value shouldBe None
    }

  }

}

object PriceRequestCreatorImplSpec {

  val TestClientId: ClientId = 123L
  val TestAgencyId: ClientId = 1L
  val TestRegionId: RegionId = RegionId(100L)
  val TestMarks: List[OfferMark] = List("AUDI", "CITROEN")

  private val Now = DateTimeUtil.now()

  private val TestProductKey = ActiveProductNaturalKey(
    payer = s"dealer:$TestClientId",
    target = "cars:new",
    UniqueProductType.ApplicationCreditAccess
  )

  private val client =
    Client(
      TestClientId,
      Some(TestAgencyId),
      None,
      None,
      TestRegionId,
      CityId(1123L),
      ClientStatuses.Active,
      singlePayment = Set(),
      firstModerated = true,
      paidCallsAvailable = true,
      priorityPlacement = true
    )

  private val dealerQuotaRequestSettings =
    QuotaRequest.Settings(
      size = 10,
      days = 1L,
      price = None,
      QuotaEntities.Dealer
    )

  private val partsQuotaRequestSettings =
    QuotaRequest.Settings(
      size = 50,
      days = 7L,
      price = None,
      QuotaEntities.Parts
    )

  private val carOffer =
    Offer
      .newBuilder()
      .setCategory(Category.CARS)
      .setSection(Section.NEW)
      .setCarInfo {
        CarInfo
          .newBuilder()
          .setMark("OPEL")
          .setModel("ASTRA")
      }
      .build()

  private val motoOffer =
    Offer
      .newBuilder()
      .setCategory(Category.MOTO)
      .setSection(Section.NEW)
      .setMotoInfo {
        MotoInfo
          .newBuilder()
          .setMark("YAMAHA")
      }
      .build()

  private def priceRequest(
      product: ProductId,
      category: OfferCategory,
      section: Section,
      marks: Seq[OfferMark],
      hasPriorityPlacement: Boolean,
      interval: DateTimeInterval
  ) =
    PriceRequest(
      PaidCallClientOffer(category, section, mark = None, model = None),
      PaidCallClientContext(TestRegionId, marks.toList, hasPriorityPlacement),
      product,
      interval
    )

  private def priceRequest(
      product: ProductId,
      size: Int,
      regionId: RegionId,
      marks: Seq[OfferMark],
      tariff: Option[TariffType],
      interval: DateTimeInterval
  ) =
    PriceRequest(
      offer = None,
      QuotaContext(regionId, marks.toList, size, tariff),
      product,
      interval,
      priceRequestId = None
    )

  private def partsPriceRequest(
      product: ProductId,
      size: Int,
      regionId: RegionId,
      interval: DateTimeInterval
  ) =
    PriceRequest(
      offer = None,
      PartsQuotaContext(regionId, size),
      product,
      interval,
      priceRequestId = None
    )

  private def productForKey(key: ActiveProductNaturalKey): Product =
    Product(
      id = 1L,
      key = key,
      status = NeedPayment,
      createDate = Now,
      expireDate = None,
      context = None,
      inactiveReason = None,
      prolongable = false,
      pushed = false,
      tariff = None
    )

  private def productWithTariff(
      key: ActiveProductNaturalKey,
      productTariff: ProductTariff
  ): Product =
    productForKey(key).copy(tariff = Some(productTariff))

  private def priceRequestsEqualIgnoreId(
      req1: PriceRequest,
      req2: PriceRequest
  ): Boolean =
    req1.product == req2.product && req1.context == req2.context && req1.offer == req2.offer && req1.interval == req2.interval
}
