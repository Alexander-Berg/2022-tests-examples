package auto.dealers.trade_in_notifier.logic

import auto.common.manager.region.testkit.RegionManagerMock
import common.geobase.model.RegionIds
import common.geobase.{Region, RegionTypes}
import common.zio.ops.tracing.RequestId
import ru.auto.api.common_model.GeoPoint
import ru.auto.api.api_offer_model.{
  Category,
  Documents,
  Location,
  Offer,
  OfferStatus,
  Phone,
  Seller,
  State,
  TradeInInfo,
  TradeInType
}
import ru.auto.api.cars_model.CarInfo
import auto.dealers.trade_in_notifier.logic.TradeIn.{
  MissingFieldError,
  OfferLogInternalError,
  SubscriptionMatcherInternalError
}
import auto.dealers.trade_in_notifier.model.SimplifiedOffer.MissingField
import auto.dealers.trade_in_notifier.storage.OfferLogRepository.OfferIsNotUniqueError
import auto.dealers.trade_in_notifier.storage.SubscriptionsMatcherRepository.NoSubscribers
import auto.dealers.trade_in_notifier.storage.TradeInRegionRepository
import auto.dealers.trade_in_notifier.storage.TradeInRegionRepository.TradeInRegionRepository
import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.mock.Expectation._
import zio.test.TestAspect.sequential
import auto.dealers.trade_in_notifier.storage.testkit._
import zio.clock.Clock
import common.zio.logging.Logging

import java.util.UUID

object TradeInSpec extends DefaultRunnableSpec {

  val region = Region(
    id = 1,
    parentId = 1,
    chiefRegionId = 1,
    `type` = RegionTypes.City,
    ruName = "мск",
    ruPreposition = "мск",
    ruNamePrepositional = "мск",
    ruNameGenitive = "мск",
    ruNameDative = "мск",
    ruNameAccusative = "мск",
    ruNameInstrumental = "мск",
    latitude = 0,
    longitude = 0,
    tzOffset = 3,
    population = 100
  )

  val seller = Seller(
    name = "Bob",
    phones = List(Phone("79111234567")),
    location = Some(Location(geobaseId = 1, coord = Some(GeoPoint(0, 0))))
  )

  val carInfo = CarInfo(
    mark = Some("BMW"),
    model = Some("X5"),
    bodyType = Some("ALLROAD_5_DOORS"),
    engineType = Some("DIESEL"),
    transmission = Some("AUTOMATIC"),
    drive = Some("ALL"),
    superGenId = Some(42L),
    configurationId = Some(43L),
    techParamId = Some(44L),
    complectationId = Some(45L)
  )

  val offer = Offer(
    id = "0123-4567",
    status = OfferStatus.ACTIVE,
    seller = Some(seller),
    category = Category.CARS,
    categoryInfo = Offer.CategoryInfo.CarInfo(carInfo),
    colorHex = "0x5a5a5a",
    documents = Some(Documents(year = 1998, ownersNumber = 1)),
    state = Some(State(mileage = 50000)),
    tradeInInfo = Some(
      TradeInInfo(
        tradeInType = TradeInType.FOR_NEW,
        tradeInPriceRange = Some(TradeInInfo.PriceRange(from = 300000, to = 330000, currency = "RUB"))
      )
    )
  )

  val requestId = RequestId.test(Some("test-id"))

  private val availableTrue = testM("check if trade-in available") {
    val offerLogMock = OfferLogRepositoryMock.Append(anything, value(UUID.randomUUID())).atMost(0)
    val regionRepositoryMock = RegionManagerMock.IsSubregion(anything, value(true))
    val subscriptionMatcherRepositoryMock =
      SubscriptionsMatcherRepositoryMock.SubscriptionsExist(equalTo(offer), value(true))
    val tradeInRegionRepositoryMock = createTradeInRegionRepositoryLayer(isExist = true)

    val layer =
      (tradeInRegionRepositoryMock ++ offerLogMock ++ regionRepositoryMock ++ subscriptionMatcherRepositoryMock) >>> TradeIn.live

    (for {
      result <- TradeIn.available(offer)
    } yield assert(result)(isTrue)).provideLayer(Clock.live ++ requestId ++ layer)
  }

  private val availableFalse = testM("if region is not available for trade-in then do not call subscriptionsExist") {
    val offerLogMock = OfferLogRepositoryMock.Append(anything, value(UUID.randomUUID())).atMost(0)
    val regionRepositoryMock = RegionManagerMock.IsSubregion(anything, value(true))
    val subscriptionMatcherRepositoryMock =
      SubscriptionsMatcherRepositoryMock.SubscriptionsExist(equalTo(offer), value(true)).atMost(0)
    val tradeInRegionRepositoryMock = createTradeInRegionRepositoryLayer(isExist = false)

    val layer =
      (tradeInRegionRepositoryMock ++ offerLogMock ++ regionRepositoryMock ++ subscriptionMatcherRepositoryMock) >>> TradeIn.live

    (for {
      result <- TradeIn.available(offer)
    } yield assert(result)(isFalse)).provideLayer(Clock.live ++ requestId ++ layer)
  }

  private val availableFailMissingField = testM("available should fail on missing field") {
    val offerLogMock = OfferLogRepositoryMock.Append(anything, value(UUID.randomUUID())).atMost(0)
    val regionRepositoryMock = RegionManagerMock.IsSubregion(anything, value(true)).atMost(0)
    val subscriptionMatcherRepositoryMock =
      SubscriptionsMatcherRepositoryMock.SubscriptionsExist(equalTo(offer), value(true)).atMost(0)
    val tradeInRegionRepositoryMock = createTradeInRegionRepositoryLayer(isExist = true)

    val layer =
      (tradeInRegionRepositoryMock ++ offerLogMock ++ regionRepositoryMock ++ subscriptionMatcherRepositoryMock) >>> TradeIn.live

    (for {
      result <- TradeIn.available(offer.copy(seller = None)).run
    } yield assert(result)(fails(equalTo(MissingFieldError(MissingField("seller"))))))
      .provideLayer(Clock.live ++ requestId ++ layer)
  }

  private val availableFailSubscriptionInternalError = testM("available should fail on subscription internal error") {
    val offerLogMock = OfferLogRepositoryMock.Append(anything, value(UUID.randomUUID())).atMost(0)
    val regionRepositoryMock = RegionManagerMock.IsSubregion(anything, value(true))
    val subscriptionMatcherRepositoryMock =
      SubscriptionsMatcherRepositoryMock.SubscriptionsExist(
        equalTo(offer),
        failure(NoSubscribers(offer))
      )
    val tradeInRegionRepositoryMock = createTradeInRegionRepositoryLayer(isExist = true)

    val layer =
      (tradeInRegionRepositoryMock ++ offerLogMock ++ regionRepositoryMock ++ subscriptionMatcherRepositoryMock) >>> TradeIn.live

    (for {
      result <- TradeIn.available(offer).run
    } yield assert(result)(fails(equalTo(SubscriptionMatcherInternalError(NoSubscribers(offer))))))
      .provideLayer(Clock.live ++ requestId ++ layer)
  }

  private val agreeSuccess = testM("agree should complete successfully") {
    val uuid = UUID.randomUUID()
    val offerLogMock = OfferLogRepositoryMock.Append(anything, value(uuid))
    val regionRepositoryMock = RegionManagerMock.IsSubregion(anything, value(true))
    val subscriptionMatcherRepositoryMock =
      SubscriptionsMatcherRepositoryMock.Push(equalTo((uuid, offer)), value(()))
    val tradeInRegionRepositoryMock = createTradeInRegionRepositoryLayer(isExist = true)

    val layer =
      (tradeInRegionRepositoryMock ++ offerLogMock ++ regionRepositoryMock ++ subscriptionMatcherRepositoryMock) >>> TradeIn.live

    (for {
      _ <- TradeIn.agree(offer)
    } yield assertCompletes).provideLayer(Clock.live ++ Logging.live ++ layer)
  }

  private val agreeFailMissingField = testM("agree should fail on missing field") {
    val uuid = UUID.randomUUID()
    val offerLogMock = OfferLogRepositoryMock.Append(anything, value(uuid)).atMost(0)
    val regionRepositoryMock = RegionManagerMock.IsSubregion(anything, value(true)).atMost(0)
    val subscriptionMatcherRepositoryMock =
      SubscriptionsMatcherRepositoryMock.Push(equalTo((uuid, offer)), value(())).atMost(0)
    val tradeInRegionRepositoryMock = createTradeInRegionRepositoryLayer(isExist = true)

    val layer =
      (tradeInRegionRepositoryMock ++ offerLogMock ++ regionRepositoryMock ++ subscriptionMatcherRepositoryMock) >>> TradeIn.live

    (for {
      result <- TradeIn.agree(offer.copy(seller = None)).run
    } yield assert(result)(fails(equalTo(MissingFieldError(MissingField("seller"))))))
      .provideLayer(Clock.live ++ Logging.live ++ layer)
  }

  private val agreeFailOfferLogInternalError = testM("agree should fail on offer log internal error") {
    val uuid = UUID.randomUUID()
    val offerLogMock = OfferLogRepositoryMock.Append(anything, failure(OfferIsNotUniqueError))
    val regionRepositoryMock = RegionManagerMock.IsSubregion(anything, value(true))
    val subscriptionMatcherRepositoryMock =
      SubscriptionsMatcherRepositoryMock.Push(equalTo((uuid, offer)), value(())).atMost(0)
    val tradeInRegionRepositoryMock = createTradeInRegionRepositoryLayer(isExist = true)

    val layer =
      (tradeInRegionRepositoryMock ++ offerLogMock ++ regionRepositoryMock ++ subscriptionMatcherRepositoryMock) >>> TradeIn.live

    (for {
      result <- TradeIn.agree(offer).run
    } yield assert(result)(fails(equalTo(OfferLogInternalError(OfferIsNotUniqueError)))))
      .provideLayer(Clock.live ++ Logging.live ++ layer)
  }

  private val agreeFailSubscriptionInternalError = testM("agree should fail on subscription internal error") {
    val uuid = UUID.randomUUID()
    val offerLogMock =
      OfferLogRepositoryMock.Append(anything, value(uuid)) ++ OfferLogRepositoryMock.Delete(equalTo(uuid), unit)
    val regionRepositoryMock = RegionManagerMock.IsSubregion(anything, value(true))
    val subscriptionMatcherRepositoryMock =
      SubscriptionsMatcherRepositoryMock.Push(equalTo((uuid, offer)), failure(NoSubscribers(offer)))
    val tradeInRegionRepositoryMock = createTradeInRegionRepositoryLayer(isExist = true)

    val layer =
      (tradeInRegionRepositoryMock ++ offerLogMock ++ regionRepositoryMock ++ subscriptionMatcherRepositoryMock) >>> TradeIn.live

    (for {
      result <- TradeIn.agree(offer).run
    } yield assert(result)(fails(equalTo(SubscriptionMatcherInternalError(NoSubscribers(offer))))))
      .provideLayer(Clock.live ++ Logging.live ++ layer)
  }

  private def createTradeInRegionRepositoryLayer(isExist: Boolean): ULayer[TradeInRegionRepository] =
    ZLayer.succeed(new TradeInRegionRepository.Service {

      override def exists(regionId: RegionIds.RegionId => UIO[Boolean]): UIO[Boolean] =
        regionId(RegionIds.RegionId(1)).as(isExist)

      override def get: UIO[List[RegionIds.RegionId]] = UIO.succeed(List.empty)
    })

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("TradeIn")(
      availableTrue,
      availableFalse,
      availableFailMissingField,
      availableFailSubscriptionInternalError,
      agreeSuccess,
      agreeFailMissingField,
      agreeFailOfferLogInternalError,
      agreeFailSubscriptionInternalError
    ) @@ sequential
}
