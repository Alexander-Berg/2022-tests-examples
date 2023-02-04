package auto.dealers.trade_in_notifier.logic

import auto.common.manager.catalog.model.{CatalogFilter, CatalogNames}
import auto.common.manager.catalog.testkit.CatalogRepositoryMock
import auto.common.manager.region.{RegionManager, RegionManagerLive}
import auto.common.manager.region.testkit.{RegionManagerEmpty, RegionManagerStub}
import auto.dealers.trade_in_notifier.model.SimplifiedOffer.PriceRange
import auto.dealers.trade_in_notifier.model._
import auto.dealers.trade_in_notifier.storage.TradeInMQRepository._
import auto.dealers.trade_in_notifier.storage._
import auto.dealers.trade_in_notifier.storage.managers.ColorPaletteRepositoryLive
import auto.dealers.trade_in_notifier.storage.postgres._
import auto.dealers.trade_in_notifier.storage.testkit._
import common.geobase.model.RegionIds._
import common.zio.logging.Logging
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import zio._
import zio.clock.Clock
import zio.interop.catz._
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._
import zio.test.mock.Expectation._

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

object TradeInNotifierSpec extends DefaultRunnableSpec {

  val uuid0 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
  val uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174432")

  val simpleCarInfo = SimplifiedOffer.SimplifiedCarInfo(
    mark = "BMW",
    model = "X5",
    body = "ALLROAD_5_DOORS",
    engine = "DIESEL",
    transmission = "AUTOMATIC",
    drive = "ALL",
    superGenId = Some(42L),
    configurationId = Some(43L),
    techParamId = Some(44L),
    complectationId = Some(45L)
  )

  val simpleOffer0 = SimplifiedOffer(
    name = "Bob",
    phones = List("79111234567"),
    location = Moscow,
    year = 1998,
    mileage = 50000,
    info = simpleCarInfo,
    color = "CACECB",
    ownersNumber = 1,
    tradeInType = "FOR_NEW",
    priceRange = PriceRange(BigDecimal(300000), BigDecimal(330000)),
    currency = "RUB",
    offerId = Some("0123-4567")
  )

  val simpleOffer1 = SimplifiedOffer(
    name = "Mary",
    phones = List("79111234568"),
    location = Moscow,
    year = 1998,
    mileage = 50000,
    info = simpleCarInfo,
    color = "CACECB",
    ownersNumber = 1,
    tradeInType = "FOR_NEW",
    priceRange = PriceRange(BigDecimal(300000), BigDecimal(340000)),
    currency = "RUB",
    offerId = Some("0123-3210")
  )

  val simpleMeta = SimplifiedOfferMeta(
    markHR = Some("BMW"),
    modelHR = Some("X5"),
    locationHR = Some("Москва"),
    colorHR = Some("серебристый"),
    transportInfo = Some("AMT 1.8 AMT (140 л.с.)")
  )

  val now = OffsetDateTime
    .now()
    .truncatedTo(ChronoUnit.DAYS)
    .plusHours(1)

  val catalogMock =
    CatalogRepositoryMock.GetHumanNames(
      equalTo(CatalogFilter("BMW", Some("X5"), Some(42L), Some(44L))),
      value(CatalogNames("BMW", Some("X5"), Some("VIII Рестайлинг"), Some("AMT 1.8 AMT (140 л.с.)")))
    )

  type PartialEnv = ColorPaletteRepository.ColorPaletteRepository
    with Has[RegionManager]
    with TradeInMQRepository.TradeInMQRepository
    with NotificationLogRepository.NotificationLogRepository
    with Logging.Logging
    with Clock

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    val suit = suite("TradeInNotifier")(
      singleMessageTest,
      multipleMessageTest,
      limitedMessageTest,
      incompleteMessageTest,
      unsentMessageTest
    ) @@ beforeAll(Schema.dbInit) @@ after(Schema.dbCleanup) @@ sequential

    suit.provideCustomLayerShared(
      Schema.live >+>
        PgTradeInMQRepository.live >+>
        PgOfferLogRepository.live >+>
        PgNotificationLogRepository.live >+>
        ColorPaletteRepositoryLive.live >+>
        RegionManagerStub.live >+>
        Clock.live >+>
        Logging.live
    )
  }

  val insertOffers = for {
    uuid0 <- OfferLogRepository.append(simpleOffer0, now)
    uuid1 <- OfferLogRepository.append(simpleOffer1, now)
  } yield (uuid0, uuid1)

  val insertMalformedOffers = {
    val malformedOffer1 = simpleOffer1.copy(location = RegionId(-42))
    for {
      uuid0 <- OfferLogRepository.append(simpleOffer0, now)
      uuid1 <- OfferLogRepository.append(malformedOffer1, now)
    } yield (uuid0, uuid1)
  }

  val selectLoggedNotifs: ZIO[Has[Transactor[Task]], Nothing, List[UUID]] =
    ZIO
      .accessM[Has[Transactor[Task]]](xa =>
        sql"SELECT trade_in_id FROM trade_in_notification_log"
          .query[UUID]
          .to[List]
          .transact(xa.get)
      )
      .orDie

  val singleMessageTest =
    testM("single notification")(insertOffers.flatMap { case (uuid, _) =>
      val emailMock =
        EmailNotificationRepositoryMock.SendTradeInNotification(
          equalTo((uuid, "bob@dot.com", simpleOffer0, simpleMeta)),
          unit
        )

      val mocks = (catalogMock ++ emailMock).toLayer
      val tradeInNotifier =
        mocks ++ ZLayer.requires[PartialEnv] >>> TradeInNotifier.live

      for {
        _ <- TradeInMQRepository.enqueue(Seq(TradeInRecord(0, "bob@dot.com", uuid)))
        res <- TradeInNotifier.notify(100).provideSomeLayer(tradeInNotifier)
        logged <- selectLoggedNotifs
      } yield assert(res)(isUnit) && assert(logged)(hasSameElements(Seq(uuid)))
    })

  val multipleMessageTest =
    testM("multiple notifications")(insertOffers.flatMap { case (uuid0, uuid1) =>
      val emailMock0 =
        EmailNotificationRepositoryMock.SendTradeInNotification(
          equalTo((uuid0, "bob@dot.com", simpleOffer0, simpleMeta)),
          unit
        )

      val emailMock1 =
        EmailNotificationRepositoryMock.SendTradeInNotification(
          equalTo((uuid1, "ann@dot.com", simpleOffer1, simpleMeta)),
          unit
        )

      val mocks = (catalogMock ++ emailMock0 ++ catalogMock ++ emailMock1).toLayer
      val tradeInNotifier =
        mocks ++ ZLayer.requires[PartialEnv] >>> TradeInNotifier.live

      for {
        _ <- TradeInMQRepository.enqueue(
          Seq(
            TradeInRecord(0, "bob@dot.com", uuid0),
            TradeInRecord(1, "ann@dot.com", uuid1)
          )
        )
        res <- TradeInNotifier.notify(100).provideSomeLayer(tradeInNotifier)
        logged <- selectLoggedNotifs
      } yield assert(res)(isUnit) && assert(logged)(hasSameElements(Seq(uuid0, uuid1)))
    })

  val limitedMessageTest =
    testM("limited notifications and sending order")(insertOffers.flatMap { case (uuid0, uuid1) =>
      val emailMock0 =
        EmailNotificationRepositoryMock.SendTradeInNotification(
          equalTo((uuid0, "bob@dot.com", simpleOffer0, simpleMeta)),
          unit
        )

      val mocks = (catalogMock ++ emailMock0).toLayer
      val tradeInNotifier =
        mocks ++ ZLayer.requires[PartialEnv] >>> TradeInNotifier.live

      for {
        _ <- TradeInMQRepository.enqueue(
          Seq(
            TradeInRecord(0, "bob@dot.com", uuid0),
            TradeInRecord(1, "ann@dot.com", uuid1)
          )
        )
        res <- TradeInNotifier.notify(1).provideSomeLayer(tradeInNotifier)
        logged <- selectLoggedNotifs
      } yield assert(res)(isUnit) && assert(logged)(hasSameElements(Seq(uuid0)))
    })

  val incompleteMessageTest =
    testM("malformed notifications are not sent")(insertMalformedOffers.flatMap { case (uuid0, uuid1) =>
      val emailMock =
        EmailNotificationRepositoryMock.SendTradeInNotification(
          equalTo((uuid0, "bob@dot.com", simpleOffer0, simpleMeta)),
          unit
        )

      val mocks = (catalogMock ++ emailMock).toLayer
      val tradeInNotifier =
        mocks ++ ZLayer.requires[PartialEnv] >>> TradeInNotifier.live

      for {
        _ <- TradeInMQRepository.enqueue(
          Seq(
            TradeInRecord(0, "bob@dot.com", uuid0),
            TradeInRecord(1, "ann@dot.com", uuid1)
          )
        )
        res <- TradeInNotifier.notify(100).provideSomeLayer(tradeInNotifier)
        logged <- selectLoggedNotifs
      } yield assert(res)(isUnit) && assert(logged)(hasSameElements(Seq(uuid0)))
    })

  val unsentMessageTest =
    testM("failed notifications are not written to notification log")(insertOffers.flatMap { case (uuid0, uuid1) =>
      val emailMock0 =
        EmailNotificationRepositoryMock.SendTradeInNotification(
          equalTo((uuid0, "bob@dot.com", simpleOffer0, simpleMeta)),
          failure(EmailNotificationRepository.BadEmailAddress("bob@doc.com", new Exception))
        )

      val emailMock1 =
        EmailNotificationRepositoryMock.SendTradeInNotification(
          equalTo((uuid1, "ann@dot.com", simpleOffer1, simpleMeta)),
          unit
        )

      val mocks = (catalogMock ++ emailMock0 ++ catalogMock ++ emailMock1).toLayer
      val tradeInNotifier =
        mocks ++ ZLayer.requires[PartialEnv] >>> TradeInNotifier.live

      for {
        _ <- TradeInMQRepository.enqueue(
          Seq(
            TradeInRecord(0, "bob@dot.com", uuid0),
            TradeInRecord(1, "ann@dot.com", uuid1)
          )
        )
        res <- TradeInNotifier.notify(100).provideSomeLayer(tradeInNotifier)
        logged <- selectLoggedNotifs
      } yield assert(res)(isUnit) && assert(logged)(hasSameElements(Seq(uuid1)))
    })

}
