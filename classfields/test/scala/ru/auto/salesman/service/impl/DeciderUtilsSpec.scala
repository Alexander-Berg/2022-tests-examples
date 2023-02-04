package ru.auto.salesman.service.impl

import org.joda.time.DateTime
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.Section.NEW
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.salesman.client.GuardianClient.HoldStates
import ru.auto.salesman.environment._
import ru.auto.salesman.model.ProductId.Placement
import ru.auto.salesman.model._
import ru.auto.salesman.service.GoodsDecider
import ru.auto.salesman.service.GoodsDecider.Action.{Deactivate, NoAction}
import ru.auto.salesman.service.GoodsDecider.DeactivateReason._
import ru.auto.salesman.service.GoodsDecider.NoActionReason._
import ru.auto.salesman.service.GoodsDecider._
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.dao.gens._
import ru.auto.salesman.test.model.gens.OfferModelGenerators._
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.util.GeoUtils
import ru.yandex.vertis.billing.Model.CampaignHeader
import ru.yandex.vertis.billing.Model.InactiveReason.NO_ENOUGH_FUNDS
import ru.yandex.vertis.generators.ProducerProvider.asProducer
import zio.test.environment.{TestClock, TestEnvironment}

import java.time.OffsetDateTime
import scala.concurrent.duration._
import scala.util.Either.RightProjection
import scala.util.{Failure, Left, Success}

class DeciderUtilsSpec extends BaseSpec {

  import DeciderUtils._
  import DeciderUtilsSpec._

  "checkClient" should {

    "return Deactivate if client is inactive" in {
      val client = testClient.copy(isActive = false)
      checkClient(client) should matchPattern(deactivatePf(InactiveClient))
    }

    "return Deactivate if client hasn't passed moderation" in {
      val client = testClient.copy(firstModerated = false)
      checkClient(client) should matchPattern(deactivatePf(InactiveClient))
    }

    "return the same client in other case" in {
      checkClient(testClient).value should be(())
    }
  }

  "checkHold" should {
    "return HoldStates.Ok if hold is Ok" in {
      checkHold(HoldStates.Ok).value shouldBe HoldStates.Ok
    }

    "return Deactivate if not enough funds to hold" in {
      checkHold(HoldStates.NotEnoughFunds) should matchPattern(
        deactivatePf(NotEnoughFunds)
      )
    }
  }

  "getCurrentActivateDate" should {
    val AlmostDayMs = (24.hours - 5.seconds).toMillis.toInt
    val MoreThenDayMs = (24.hours + 5.seconds).toMillis.toInt
    val ExactlyDayMs = 24.hours.toMillis.toInt
    val shiftMillis = (2.hour + 45.minutes).toMillis
    val today = now().withTimeAtStartOfDay()
    val activateDates = Iterable(
      today.withTimeAtStartOfDay().plus(shiftMillis),
      today.plusDays(1).plus(shiftMillis)
    ).map(FirstActivateDate.apply)

    "return today with shift as activateDate" in {
      activateDates.foreach { ad =>
        val timestamp = getCurrentActivateDate(ad, None)
        timestamp.getMillisOfDay shouldBe shiftMillis
        timestamp.withTimeAtStartOfDay() shouldBe today
      }
    }

    "return yesterday if active date was less then 24h ago" in {
      val ad = FirstActivateDate(now().minusMillis(AlmostDayMs))
      val timestamp = getCurrentActivateDate(ad, None)
      timestamp.getMillisOfDay shouldEqual ad.getMillisOfDay +- 1000
      timestamp.withTimeAtStartOfDay() shouldBe today
        .minusDays(1)
        .withTimeAtStartOfDay()
    }

    "return today if active date was more then 24h ago" in {
      val ad = FirstActivateDate(now().minusMillis(MoreThenDayMs))
      val timestamp = getCurrentActivateDate(ad, None)
      timestamp.getMillisOfDay shouldEqual ad.getMillisOfDay +- 1000
      timestamp.withTimeAtStartOfDay() shouldBe today
        .withTimeAtStartOfDay()
    }

    "return yesterday if active date was less then 24h * N ago" in {
      for (N <- 1 to 3) {
        val ad =
          FirstActivateDate(now().minusMillis(AlmostDayMs + N * ExactlyDayMs))
        val timestamp = getCurrentActivateDate(ad, None)
        timestamp.getMillisOfDay shouldEqual ad.getMillisOfDay +- 1000
        timestamp.withTimeAtStartOfDay() shouldBe today
          .minusDays(1)
          .withTimeAtStartOfDay()
      }
    }

    "return today if active date was more then 24h * N ago" in {
      for (N <- 1 to 3) {
        val ad =
          FirstActivateDate(now().minusMillis(MoreThenDayMs + N * ExactlyDayMs))
        val timestamp = getCurrentActivateDate(ad, None)
        timestamp.getMillisOfDay shouldEqual ad.getMillisOfDay +- 1000
        timestamp.withTimeAtStartOfDay() shouldBe today.withTimeAtStartOfDay()
      }
    }

    "return today if active date was exactly 24h * N ago" in {
      for (N <- 0 to 3) {
        val ad =
          FirstActivateDate(now().minusMillis(N * ExactlyDayMs))
        val timestamp = getCurrentActivateDate(ad, None)
        timestamp.getMillisOfDay shouldEqual ad.getMillisOfDay +- 1000
        timestamp.withTimeAtStartOfDay() shouldBe today.withTimeAtStartOfDay()
      }
    }

    "return next activate day based by known deadline" in {
      val ad = FirstActivateDate(now().minusDays(10))
      val deadline = now.minusDays(9)
      val timestamp = getCurrentActivateDate(ad, Some(deadline))
      timestamp.asDateTime shouldBe deadline
    }
  }

  "onStop " should {
    "return next on success" in {
      Success(1).orStop(ClientResolutionError).value shouldBe 1
    }

    "return NoAction with reason of failure" in {
      val message = "message"
      Failure(new Exception(message))
        .orStop(ClientResolutionError) should matchPattern {
        case RightProjection(Left(NoAction(ClientResolutionError(e))))
            if e.getMessage == message =>
      }
    }
  }

  "getBillingTimestampZ" should {

    def getBillingTimestamp(activateDate: String, now: String) = {
      val parsedNow = OffsetDateTime.parse(now)
      val parsedActivateDate = ActivateDate(DateTime.parse(activateDate))
      (TestClock.setDateTime(parsedNow) *>
        getBillingTimestampZ(parsedActivateDate))
        .provideLayer(zio.ZEnv.live >>> TestEnvironment.live)
        .success
        .value
        .asDateTime
        .toString("yyyy-MM-dd'T'HH:mmZZ")
    }

    "return given activate date if there is no big lag" in {
      val recentActivateDate = "2019-12-15T13:05+03:00"
      val now = "2019-12-15T13:28+03:00"
      getBillingTimestamp(recentActivateDate, now) shouldBe recentActivateDate
    }

    "return now if there is big lag" in {
      val oldActivateDate = "2019-11-30T18:21+03:00"
      val now = "2019-12-15T13:28+03:00"
      getBillingTimestamp(oldActivateDate, now) shouldBe now
    }
  }
}

object DeciderUtilsSpec {

  val testOfferId: OfferId = 1
  val testOfferHash = "fff"
  val testOfferCategory = OfferCategories.Cars
  val testOfferSection = Section.USED

  val testOffer = offerGen(
    autoruOfferIdGen(testOfferId),
    Category.CARS,
    offerSectionGen = testOfferSection
  ).next

  val testCarsNewOffer =
    offerGen(
      autoruOfferIdGen(testOfferId),
      Category.CARS,
      offerSectionGen = Section.NEW
    ).next

  val testCarsUsedOffer =
    offerGen(
      autoruOfferIdGen(testOfferId),
      Category.CARS,
      offerSectionGen = Section.USED
    ).next

  val testCallsOffer: ApiOfferModel.Offer =
    testOffer.toBuilder.setSection(NEW).build()

  val testClientId: ClientId = 2
  val testProduct = GoodsNameGen.next
  val testServiceId = 1

  val testRequest = GoodsDecider.Request(
    testClientId,
    testOffer,
    ProductContext(testProduct),
    FirstActivateDate(now()),
    None
  )

  val testCallsRequest: Request =
    testRequest.copy(
      offer = testCallsOffer,
      context = ProductContext(ProductId.Premium)
    )

  val testSingleWithCallsRequest: Request =
    testRequest.copy(
      context = ProductContext(ProductId.Premium)
    )

  val testCallsPlacementOffer: ApiOfferModel.Offer = {
    val b = testCallsOffer.toBuilder
    b.getAdditionalInfoBuilder.setCreationDate(
      DateTime.parse("2019-05-16").getMillis
    )
    b.build()
  }

  val testCallsPlacementRequest: Request =
    testCallsRequest.copy(
      offer = testCallsPlacementOffer,
      context = ProductContext(Placement)
    )

  val testSingleWithCallsPlacementRequest: Request =
    testRequest.copy(
      context = ProductContext(Placement)
    )

  val testClient = ClientDetailsGen.next.copy(
    clientId = testClientId,
    singlePayment = Set(AdsRequestTypes.CarsUsed),
    isActive = true,
    firstModerated = true
  )

  val testCallsClient = testClient.copy(paidCallsAvailable = true)

  val testCallsPlacementClient =
    testCallsClient.copy(regionId = GeoUtils.RegMoscow)

  val testNotCallsPlacementClient =
    testCallsClient.copy(regionId = GeoUtils.RegSverdlovsk)

  val regionWithCallCarsNew = RegionId(3228)

  val testSingleWithCallsPlacementClient =
    testClient.copy(regionId = regionWithCallCarsNew)

  val testBalanceClientId: BalanceClientId = testClient.balanceClientId
  val testBalanceAgencyId: Option[BalanceClientId] = testClient.balanceAgencyId
  val testAccount: AccountId = testClient.accountId
  val testProductKey: Option[String] = testClient.productKey

  val testCampaignSource = CampaignSource(
    AutoruUserId,
    testBalanceClientId,
    testBalanceAgencyId,
    testAccount,
    testProduct,
    testProductKey
  )
  val testException = new Exception

  val testCampaignHeader = CampaignHeader.newBuilder().buildPartial()

  val activeCallCampaign: CampaignHeader =
    campaignHeaderGen(inactiveReasonGen = None).next

  val activeCallCarsUsedCampaign: CampaignHeader =
    campaignHeaderGen(inactiveReasonGen = None).next

  val inactiveCallCampaign: CampaignHeader =
    campaignHeaderGen(inactiveReasonGen = Some(NO_ENOUGH_FUNDS)).next

  val loyaltyFeature =
    FeatureInstance(
      id = "qwertyui",
      origin = FeatureOrigin("origin"),
      "loyalty",
      user = "123",
      count = FeatureCount(1000L, FeatureUnits.Money),
      createTs = now(),
      deadline = now.plusDays(2),
      FeaturePayload(FeatureUnits.Money)
        .copy(featureType = FeatureTypes.Loyalty)
    )

  val loyaltyVasFeature =
    FeatureInstance(
      id = "qwertyui1",
      origin = FeatureOrigin("origin"),
      "loyalty_vas",
      user = "123",
      count = FeatureCount(1000L, FeatureUnits.Money),
      createTs = now(),
      deadline = now.plusDays(2),
      FeaturePayload(FeatureUnits.Money)
        .copy(featureType = FeatureTypes.Loyalty)
    )

  val loyaltyPlacementFeature =
    FeatureInstance(
      id = "qwertyui2",
      origin = FeatureOrigin("origin"),
      "loyalty_placement",
      user = "123",
      count = FeatureCount(1000L, FeatureUnits.Money),
      createTs = now(),
      deadline = now.plusDays(2),
      FeaturePayload(FeatureUnits.Money)
        .copy(featureType = FeatureTypes.Loyalty)
    )

  val loyaltyDiscountFeature =
    FeatureInstance(
      id = "discFeature1",
      origin = FeatureOrigin("origin"),
      "loyalty_placement",
      user = "123",
      count = FeatureCount(1000L, FeatureUnits.Items),
      createTs = now(),
      deadline = now().plusDays(2),
      FeaturePayload(
        FeatureUnits.Items,
        FeatureTypes.Loyalty,
        constraint = None,
        Some(FeatureDiscount(FeatureDiscountTypes.Percent, 50))
      )
    )

  def feature(product: ProductId): FeatureInstance = {
    val featureId = s"$product:promo_salesman-test:96eb92e69602f216"
    val featureTag = ProductId.alias(product)
    val featurePayload = FeaturePayload(FeatureUnits.Money)
    val featureCount = FeatureCount(1000L, FeatureUnits.Money)
    val user = "123"
    FeatureInstance(
      featureId,
      FeatureOrigin("origin"),
      featureTag,
      user,
      featureCount,
      now(),
      now.plusDays(2),
      featurePayload
    )
  }

  def features(product: ProductId): List[FeatureInstance] =
    features(feature(product))

  def features(feature: FeatureInstance): List[FeatureInstance] =
    List(
      feature,
      loyaltyFeature,
      loyaltyVasFeature,
      loyaltyPlacementFeature
    )

  def noActionPf(reason: NoActionReason): PartialFunction[Any, _] = {
    case RightProjection(Left(NoAction(`reason`))) =>
  }

  def deactivatePf(reason: DeactivateReason): PartialFunction[Any, _] = {
    case RightProjection(
          Left(
            Deactivate(
              `reason`,
              Some(OfferStatuses.Expired),
              /* deactivateOtherGoods = */ true
            )
          )
        ) =>
  }

  def nonDestructiveDeactivatePf(
      reason: DeactivateReason
  ): PartialFunction[Any, _] = {
    case RightProjection(
          Left(Deactivate(`reason`, None, /* deactivateOtherGoods = */ false))
        ) =>
  }
}
