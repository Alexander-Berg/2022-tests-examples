package ru.auto.salesman.service.impl

import cats.data.NonEmptyList
import org.joda.time.DateTime
import org.scalatest.BeforeAndAfter
import ru.auto.api.ApiOfferModel.Section.USED
import ru.auto.api.ApiOfferModel.{Category, Offer}
import ru.auto.salesman.client.GuardianClient.HoldStates.Ok
import ru.auto.salesman.client.PromocoderClient
import ru.auto.salesman.dao.GoodsDao.Condition.WithGoodsId
import ru.auto.salesman.dao.GoodsDao.Filter.Since
import ru.auto.salesman.dao._
import ru.auto.salesman.dao.impl.jdbc.{
  JdbcCarsGoodsQueries,
  JdbcCategorizedGoodsQueries,
  JdbcGoodsDao,
  JdbcGoodsQueries
}
import ru.auto.salesman.dao.slick.invariant.StaticQuery
import ru.auto.salesman.dao.user.ProductScheduleDao
import ru.auto.salesman.environment.{now, IsoDateTimeFormatter}
import ru.auto.salesman.model.GoodStatuses.{Active, Inactive}
import ru.auto.salesman.model.OfferStatuses.Expired
import ru.auto.salesman.model.ProductId.{Special, Turbo}
import ru.auto.salesman.model.{
  ActivateDate,
  AdsRequestType,
  CampaignSource,
  CityId,
  DetailedClient,
  ProductId,
  RegionId,
  RichCategory
}
import ru.auto.salesman.service.GoodsDecider.Action.{Activate, Deactivate}
import ru.auto.salesman.service.GoodsDecider.DeactivateReason.InactiveClient
import ru.auto.salesman.service.GoodsDecider.{Response, Request => GDRequest}
import ru.auto.salesman.service.PriceEstimateService.PriceRequest
import ru.auto.salesman.service._
import ru.auto.salesman.service.impl.moisha.model._
import ru.auto.salesman.tasks.instrumented.InstrumentedGoodsService
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.data.MoishaTestDataGen
import ru.auto.salesman.test.template.SalesJdbcSpecTemplate
import ru.auto.salesman.util.{AutomatedContext, DateTimeInterval, RequestContext}
import ru.yandex.vertis.billing.Model.Good.Custom
import ru.yandex.vertis.billing.Model._
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.util.Try

trait GoodsServicePackageSpec
    extends BaseSpec
    with SalesJdbcSpecTemplate
    with BeforeAndAfter {

  import GoodsServiceImplSpec._
  implicit private val rc = AutomatedContext("test")

  before {
    goodsDao.clean()
  }

  val decider = mock[GoodsDecider]

  private val activateDate = ActivateDate(now())

  trait DeciderDeactivated {
    (decider
      .apply(_: GDRequest)(_: RequestContext))
      .expects(*, *)
      .returningT(
        Response(Deactivate(InactiveClient, offerStatusPatch = Some(Expired)))
      )
      .anyNumberOfTimes()

  }

  trait DeciderActivated {
    (decider
      .apply(_: GDRequest)(_: RequestContext))
      .expects(*, *)
      .returningT(
        Response(
          Activate(activateDate, offerBilling, features = List.empty),
          None,
          None
        )
      )
      .anyNumberOfTimes()
  }

  "Goods service" should {

    "deactivate special if active turbo exists" in new DeciderDeactivated {
      val turbo = insertTurbo()
      prolongGoods(List(turbo)).success
      val special = insertSpecial()
      prolongGoods(List(special)).success
      checkInactive(special)
    }

    "activate special if only inactive turbo exists" in new DeciderActivated {
      val turbo = insertTurbo()
      deactivateTurbo(turbo)
      val special = insertSpecial()
      prolongGoods(List(special)).success
      checkActivated(special)
    }

    "activate special if only not activated yet turbo exists" in new DeciderActivated {

      insertTurbo()
      val special = insertSpecial()
      prolongGoods(List(special)).success
      checkActivated(special)
    }

    "deactivate special on turbo activation" in new DeciderActivated {
      val special = insertSpecial()
      prolongGoods(List(special)).success
      val turbo = insertTurbo()
      prolongGoods(List(turbo)).success
      checkDeactivated(special)
    }

    "deactivate special on turbo and special activation in one batch" in new DeciderDeactivated {
      val special = insertSpecial()
      val turbo = insertTurbo()
      prolongGoods(List(turbo, special)).success
      checkInactive(special)
    }

    "deactivate special on special and turbo activation in one batch" in new DeciderDeactivated {
      val special = insertSpecial()
      val turbo = insertTurbo()
      prolongGoods(List(special, turbo)).success
      checkInactive(special)
    }
  }

  // two tests: one for cars, another one for commercial
  protected def category: Category
  protected def adsRequestType: AdsRequestType

  // в этом тесте моки всегда работают так, чтобы услуга просто могла быть
  // активирована успешно; и можно было сосредоточиться на проверке
  // взаимодействия разных услуг
  private val offerService = mock[OfferService]
  private val badgeDao = mock[BadgeDao]
  private val promocoder = mock[PromocoderClient]
  private val productScheduleDao = mock[ProductScheduleDao]
  private val holdSource = mock[HoldSource]
  private val promocoderSource = mock[PromocoderSource]
  private val adsRequestDao = mock[AdsRequestDao]
  private val priceEstimateService = mock[PriceEstimateService]
  private val priceExtractor = mock[PriceExtractor]
  private val campaignCreationService = mock[CampaignCreationService]

  private val carsGoodsDao = new TestGoodsDao(JdbcCarsGoodsQueries)

  private val categorizedGoodsDao = new TestGoodsDao(JdbcCategorizedGoodsQueries)

  val campaignSource = stub[CampaignService]

  // can't use goodsDaoProvider, because need to infer TestGoodsDao type
  val goodsDao = category match {
    case Category.CARS => carsGoodsDao
    case Category.MOTO | Category.TRUCKS => categorizedGoodsDao
    case other => fail(s"unexpected category $other")
  }

  val goodsDaoProvider = new GoodsDaoProvider {
    def chooseDao(category: Category): GoodsDao = goodsDao
    def getAll(): List[GoodsDao] = List(goodsDao)
  }

  private val client =
    DetailedClient(
      clientId = 1,
      agencyId = Some(2),
      balanceClientId = 2,
      balanceAgencyId = Some(3),
      categorizedClientId = None,
      companyId = Some(4),
      regionId = RegionId(5),
      cityId = CityId(6),
      accountId = 7,
      isActive = true,
      firstModerated = true,
      singlePayment = Set(adsRequestType)
    )

  private val offer = {
    val b =
      Offer
        .newBuilder()
        // from sales.sql to join sales_services with sales in cars test
        .setId("1003276986-2b81")
        .setCategory(category)
    b.getPriceInfoBuilder.setCurrency("RUR")
    b.build()
  }

  private val adsRequest = AdsRequestDao.Record(client.clientId, adsRequestType)

  private val today = DateTimeInterval.currentDay

  private def testJsonPriceFor(product: ProductId) =
    MoishaTestDataGen.testJsonPriceFor(
      ProductId.alias(product),
      100,
      IsoDateTimeFormatter.print(today.from),
      IsoDateTimeFormatter.print(today.to),
      duration = 1
    )

  private def testPriceForIncludeRegions(product: ProductId) =
    List(
      MoishaResponse(
        List(
          MoishaPoint(
            "test-price-policy",
            MoishaInterval(DateTime.now(), DateTime.now()),
            MoishaProduct(product.toString, Nil, 100L)
          )
        ),
        request = MoishaRequestId(priceRequestId = None)
      )
    )

  private def makeGoodPrice(product: ProductId) =
    new PriceEstimateService.PriceResponse(testJsonPriceFor(product), now())

  private val turboPrice = makeGoodPrice(Turbo)

  private val specialPrice = makeGoodPrice(Special)

  private def makeCampaignHeader(product: ProductId) =
    CampaignHeader
      .newBuilder()
      .setVersion(1)
      .setId("2")
      .setOwner(CustomerHeader.newBuilder().setVersion(1))
      .setOrder(
        Order
          .newBuilder()
          .setVersion(1)
          .setId(2)
          .setOwner(CustomerId.newBuilder().setVersion(1).setClientId(2))
          .setText("3")
          .setCommitAmount(4)
          .setApproximateAmount(5)
      )
      .setProduct(
        Product
          .newBuilder()
          .setVersion(1)
          .addGoods(
            Good
              .newBuilder()
              .setVersion(1)
              .setCustom(
                Custom
                  .newBuilder()
                  .setId(ProductId.alias(product))
                  .setCost(Cost.newBuilder().setVersion(1))
              )
          )
      )
      .setSettings(
        CampaignSettings.newBuilder().setVersion(1).setIsEnabled(true)
      )
      .build()

  (offerService.updateSetDate _)
    .expects(*, *, *)
    .returningT(())
    .anyNumberOfTimes()
  (productScheduleDao.insertIfAbsent _)
    .expects(*)
    .returningT(())
    .anyNumberOfTimes()
  (holdSource.hold _)
    .expects(*, *, *, *, *, *)
    .returningT(Ok)
    .anyNumberOfTimes()
  (promocoderSource.getFeaturesForUser _)
    .expects(*)
    .returningZ(Nil)
    .anyNumberOfTimes()
  (adsRequestDao.get _)
    .expects(*, *)
    .returningT(Some(adsRequest))
    .anyNumberOfTimes()
  (priceEstimateService
    .estimate(_: PriceRequest))
    .expects(argThat { request: PriceEstimateService.PriceRequest =>
      request.product == Turbo
    })
    .returningZ(turboPrice)
    .anyNumberOfTimes()
  (priceEstimateService
    .estimate(_: PriceRequest))
    .expects(argThat { request: PriceEstimateService.PriceRequest =>
      request.product == Special
    })
    .returningZ(specialPrice)
    .anyNumberOfTimes()

  (priceEstimateService.extractor _)
    .expects(*)
    .returning(priceExtractor)
    .anyNumberOfTimes()
  (priceExtractor.price _)
    .expects(*, *)
    .returningZ(100)
    .anyNumberOfTimes()
  (priceEstimateService
    .estimateBatch(_: NonEmptyList[PriceRequest]))
    .expects(*)
    .returningZ(testPriceForIncludeRegions(Turbo))
    .anyNumberOfTimes()
  (priceEstimateService
    .estimateBatch(_: NonEmptyList[PriceRequest]))
    .expects(*)
    .returningZ(testPriceForIncludeRegions(Special))
    .anyNumberOfTimes()
  (campaignCreationService.getOrCreate _)
    .expects {
      argThat { campaign: CampaignSource =>
        campaign.product == Turbo
      }
    }
    .returningT(makeCampaignHeader(Turbo))
    .anyNumberOfTimes()
  (campaignCreationService.getOrCreate _)
    .expects {
      argThat { campaign: CampaignSource =>
        campaign.product == Special
      }
    }
    .returningT(makeCampaignHeader(Special))
    .anyNumberOfTimes()

  class TestGoodsDao(goodsDaoQueries: JdbcGoodsQueries)
      extends JdbcGoodsDao(goodsDaoQueries, database) {

    def getById(id: Long): GoodsDao.Record =
      get(Since(0)).success.value
        .find(_.primaryKeyId == id)
        .getOrElse(fail(s"Good with id = $id not found"))

    def clean(): Unit =
      database.withSession { implicit session =>
        StaticQuery.updateNA("DELETE FROM `sales_services_categories`").execute
      }
  }

  val goodsActivateService =
    new GoodsActivateServiceImpl(
      offerService,
      badgeDao,
      promocoder,
      productScheduleDao,
      goodsDaoProvider
    )

  val goodsDeactivateService =
    new GoodsDeactivateServiceImpl(
      offerService,
      goodsDao,
      badgeDao
    )

  private def makeGoodsSource(product: ProductId) =
    GoodsDao.Source(
      1003276986L,
      product,
      Active,
      activateDate = now(),
      details = Some(
        GoodsDao.SourceDetails(
          offerHash = Some("2b81"),
          category.flat,
          USED,
          client.clientId
        )
      )
    )

  val goodsService = new GoodsServiceImpl(
    goodsActivateService,
    goodsDeactivateService,
    goodsDao,
    decider
  ) with InstrumentedGoodsService {
    override def serviceName: String = "test-service"
    override def ops: OperationalSupport = TestOperationalSupport
  }

  private def prolongGoods(goods: List[GoodsDao.Record]): Try[Unit] =
    goodsService.prolong(offer, goods)

  private def insertGood(product: ProductId) =
    goodsDao.insert(makeGoodsSource(product)).success.value

  private def insertTurbo() = insertGood(Turbo)

  private def insertSpecial() = insertGood(Special)

  private def deactivateTurbo(turbo: GoodsDao.Record): Unit =
    goodsDao
      .update(
        WithGoodsId(turbo.primaryKeyId),
        GoodsDao.Patch(status = Some(Inactive))
      )
      .success
      .value

  private def checkActivated(old: GoodsDao.Record) = {
    val updated = goodsDao.getById(old.primaryKeyId)
    updated.status shouldBe Active
    updated.offerBilling should not be None
  }

  private def checkInactive(old: GoodsDao.Record) = {
    val updated = goodsDao.getById(old.primaryKeyId)
    updated.status shouldBe Inactive
    updated.offerBilling shouldBe None
  }

  private def checkDeactivated(old: GoodsDao.Record) = {
    val updated = goodsDao.getById(old.primaryKeyId)
    updated.status shouldBe Inactive
    updated.offerBilling should not be None
  }
}
