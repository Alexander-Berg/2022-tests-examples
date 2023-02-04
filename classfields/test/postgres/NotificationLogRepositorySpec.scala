package auto.dealers.trade_in_notifier.storage.postgres.test

import auto.dealers.trade_in_notifier.model.{DealerId, SimplifiedOffer}
import auto.dealers.trade_in_notifier.model.SimplifiedOffer.PriceRange
import auto.dealers.trade_in_notifier.storage.OfferLogRepository
import auto.dealers.trade_in_notifier.storage.postgres.PgOfferLogRepository
import auto.dealers.trade_in_notifier.storage.NotificationLogRepository
import auto.dealers.trade_in_notifier.storage.NotificationLogRepository._
import auto.dealers.trade_in_notifier.storage.postgres.PgNotificationLogRepository
import common.geobase.model.RegionIds.RegionId
import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestPostgresql
import doobie._
import doobie.postgres.implicits._
import doobie.implicits._
import zio.test.Assertion._
import zio.test._
import zio.test.TestAspect.{after, beforeAll, sequential}
import zio.test.{DefaultRunnableSpec, ZSpec, _}
import zio.{Has, Task, URIO, ZIO}
import zio.interop.catz._
import java.time.OffsetDateTime
import java.util.UUID
import java.time.temporal.ChronoUnit

object NotificationLogRepositorySpec extends DefaultRunnableSpec {

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

  val simpleOffer = SimplifiedOffer(
    name = "Bob",
    phones = List("79111234567"),
    location = RegionId(0),
    year = 1998,
    mileage = 50000,
    info = simpleCarInfo,
    color = "0x5a5a5a",
    ownersNumber = 1,
    tradeInType = "FOR_NEW",
    priceRange = PriceRange(BigDecimal(300000), BigDecimal(330000)),
    currency = "RUB",
    offerId = Some("0123-4567")
  )

  val now = OffsetDateTime
    .now()
    .truncatedTo(ChronoUnit.DAYS)
    .plusHours(1)

  val simpleInsertTest =
    testM("Insert several notifs") {
      for {
        id <- OfferLogRepository.append(simpleOffer, now)
        _ <- NotificationLogRepository.append(id, 0, "bob@work.com", now)
        _ <- NotificationLogRepository.append(id, 1, "mary@work.com", now)
        res <- selectEntries
      } yield assert(res)(
        hasSameElements(
          List(
            (id, 0, "bob@work.com", now),
            (id, 1, "mary@work.com", now)
          )
        )
      )
    }

  val insertForAbsentIdTest =
    testM("Won't insert notif for non-existent trade-in offer") {
      val id = UUID.fromString("123e4567-e89b-12d3-a456-426655440000")
      val result = for {
        res <- NotificationLogRepository.append(id, 0, "bob@work.com", now)
      } yield res

      assertM(result.run)(fails(isSubtype[NoSuchOfferError](anything)))
    }

  def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    val suit = suite("NotificationLogRepositorySpec")(
      simpleInsertTest,
      insertForAbsentIdTest
    ) @@ beforeAll(dbInit) @@ after(dbClean) @@ sequential

    suit.provideCustomLayerShared(
      TestPostgresql.managedTransactor >+> PgOfferLogRepository.live >+> PgNotificationLogRepository.live
    )
  }

  val selectEntries: ZIO[Has[Transactor[Task]], Throwable, List[(UUID, Long, String, OffsetDateTime)]] =
    URIO.accessM[Has[Transactor[Task]]](xa =>
      sql"SELECT trade_in_id, dealer_id, email, timestamp FROM trade_in_notification_log"
        .query[(UUID, Long, String, OffsetDateTime)]
        .to[List]
        .transact(xa.get)
    )

  private val dbInit: URIO[Has[Transactor[Task]], Unit] = ZIO
    .service[Transactor[Task]]
    .flatMap(InitSchema("/schema.sql", _))
    .orDie

  private val dbClean = ZIO
    .service[Transactor[Task]]
    .flatMap { xa =>
      sql"DELETE FROM trade_in_notification_log".update.run.transact(xa) *>
        sql"DELETE FROM trade_in_offer_log".update.run.transact(xa)
    }

}
