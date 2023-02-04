package auto.plus_me.services.test

import auto.plus_me.domain.{OfferOwner, OfferUpdatedEvent, WatchedOffer, YaPlusBonus}
import auto.plus_me.services._
import auto.plus_me.storage.mock.{MockLockDao, MockOfferWatchDao, MockYaPlusBonusDao}
import auto.plus_me.storage.{OfferWatchDao, YaPlusBonusDao}
import common.clients.plus.testkit.PlusClientMock
import common.geobase.testkit.TestGeobase
import common.models.finance.Money.Rubles
import common.zio.logging.Logging
import ru.auto.api.api_offer_model.OfferStatus
import zio._
import zio.duration._
import zio.magic._
import zio.test.TestAspect._
import zio.test._
import zio.test.environment.TestClock

import java.time.{Instant, Period}

object E2ESpec extends DefaultRunnableSpec {

  private val OfferId = "123"
  private val UserId = 2L
  private val YandexPuid = 999L

  private val StartDate = RulesService.StartDate
  private val After1Day = StartDate.plus(1.day).toInstant
  private val After1Month = StartDate.plus(32.day).toInstant

  case class SentPush(text: String, timestamp: Instant)

  case class StorageState(
      watchedOffers: Chunk[WatchedOffer],
      sentPushes: Chunk[SentPush],
      bonuses: Chunk[YaPlusBonus]) {
    def assertOneBonus: Assert = assertBonuses(After1Day)
    def assertNoBonuses: Assert = assertBonuses()

    def assertBonuses(when: Instant*): Assert = {
      assertTrue(
        bonuses.size == when.size,
        bonuses.forall(_.puid == YandexPuid),
        bonuses.forall(_.amount == Rubles(200)),
        bonuses.zip(when).forall { case (bonus, expectedInstant) =>
          bonus.statusUpdated == expectedInstant
        }
      )
    }

    def assertNoWatches: Assert = assertTrue(watchedOffers.isEmpty)
  }

  def emitOfferUpdated(
      id: String = OfferId,
      userId: Long = UserId,
      reseller: Boolean = false,
      regionId: Long = 51L, // Самара
      status: OfferStatus = OfferStatus.ACTIVE,
      created: Instant = StartDate.toInstant,
      callcenter: Boolean = false): RIO[Has[PlusMeService], Unit] = {
    PlusMeService.offerUpdated(
      OfferUpdatedEvent(id, OfferOwner.AutoruUser(userId, reseller), regionId, status, created, callcenter)
    )
  }

  def collectState = {
    for {
      bonuses <- YaPlusBonusDao.TestService.allBonuses
      watches <- OfferWatchDao.getWatchesOverDeadline(Instant.MAX, Int.MaxValue)
    } yield StorageState(
      watches,
      Chunk.empty,
      bonuses
    )
  }

  def timeFlowFor(period: Period) = {
    val eff = TestClock.adjust(1.day) *>
      PlusMeService.checkOfferWatches *>
      clock.instant

    for {
      start <- clock.instant
      end = start.plus(period)
      _ <- eff.repeatWhile(_.isBefore(end))
    } yield ()
  }

  def spec = {
    suite("PlusMe e2e spec")(
      testM("Выдаем бонус через сутки после размещения для частников") {
        for {
          _ <- UserService.TestService.setYandexPuid(UserId, Some(YandexPuid))
          _ <- emitOfferUpdated()
          _ <- timeFlowFor(Period.ofDays(10))
          state <- collectState
        } yield state.assertOneBonus && state.assertNoWatches
      },
      testM("Не даем бонус за снятое объявление") {
        for {
          _ <- UserService.TestService.setYandexPuid(UserId, Some(YandexPuid))
          _ <- emitOfferUpdated()
          _ <- emitOfferUpdated(status = OfferStatus.REMOVED)
          _ <- timeFlowFor(Period.ofDays(10))
          state <- collectState
        } yield state.assertNoBonuses && state.assertNoWatches
      },
      testM("Не даем бонус за объявление до начала акции") {
        for {
          _ <- UserService.TestService.setYandexPuid(UserId, Some(YandexPuid))
          _ <- emitOfferUpdated(created = StartDate.minusHours(1).toInstant)
          _ <- timeFlowFor(Period.ofDays(10))
          state <- collectState
        } yield state.assertNoBonuses && state.assertNoWatches
      },
      testM("Выдаем бонус только один раз") {
        for {
          _ <- UserService.TestService.setYandexPuid(UserId, Some(YandexPuid))
          _ <- emitOfferUpdated(id = "1")
          _ <- emitOfferUpdated(id = "2")
          _ <- timeFlowFor(Period.ofDays(10))
          state <- collectState
        } yield state.assertOneBonus && state.assertNoWatches
      },
      testM("Выдаем бонус повторно через месяц") {
        for {
          _ <- UserService.TestService.setYandexPuid(UserId, Some(YandexPuid))
          _ <- emitOfferUpdated(id = "1")
          _ <- timeFlowFor(Period.ofDays(31))
          _ <- emitOfferUpdated(id = "2", created = After1Month)
          _ <- timeFlowFor(Period.ofDays(1))
          state <- collectState
        } yield state.assertNoWatches && state.assertBonuses(After1Day, After1Month)
      },
      testM("Не выдаем бонус повторно через месяц за одно объявление") {
        for {
          _ <- UserService.TestService.setYandexPuid(UserId, Some(YandexPuid))
          _ <- emitOfferUpdated(id = "1")
          _ <- timeFlowFor(Period.ofDays(40))
          _ <- emitOfferUpdated(id = "1")
          _ <- timeFlowFor(Period.ofDays(1))
          state <- collectState
        } yield state.assertOneBonus && state.assertNoWatches
      },
      testM("Не даем бонус если не привязан yandex") {
        for {
          _ <- UserService.TestService.setYandexPuid(UserId, None)
          _ <- emitOfferUpdated(id = "1")
          _ <- timeFlowFor(Period.ofDays(1))
          state <- collectState
        } yield state.assertNoBonuses && state.assertNoWatches
      },
      testM("Не даем бонус в нецелевых регионах (например, Москва)") {
        for {
          _ <- UserService.TestService.setYandexPuid(UserId, Some(YandexPuid))
          _ <- emitOfferUpdated(regionId = 213L)
          _ <- timeFlowFor(Period.ofDays(1))
          state <- collectState
        } yield state.assertNoBonuses && state.assertNoWatches
      },
      testM("Не даем бонус объявлениям из колцентра") {
        for {
          _ <- UserService.TestService.setYandexPuid(UserId, Some(YandexPuid))
          _ <- emitOfferUpdated(callcenter = true)
          _ <- timeFlowFor(Period.ofDays(1))
          state <- collectState
        } yield state.assertNoBonuses && state.assertNoWatches
      }
    ) @@ before(TestClock.setDateTime(StartDate.toOffsetDateTime))
  }.injectCustom(
    RulesService.live,
    BonusService.live,
    UserService.test,
    LockService.live,
    PlusMeService.live,
    MockYaPlusBonusDao.live,
    MockOfferWatchDao.live,
    MockLockDao.live,
    Logging.live,
    TestGeobase.liveRef,
    PlusClientMock.live,
    ZLayer.succeed(BonusService.CampaignId("test"))
  )
}
