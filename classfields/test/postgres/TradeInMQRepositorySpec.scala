package auto.dealers.trade_in_notifier.storage.postgres.test

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

import common.geobase.model.RegionIds.RegionId
import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestPostgresql
import doobie.Transactor
import doobie.implicits._
import doobie.postgres.implicits._
import auto.dealers.trade_in_notifier.model.SimplifiedOffer
import auto.dealers.trade_in_notifier.model.SimplifiedOffer.PriceRange
import auto.dealers.trade_in_notifier.storage.TradeInMQRepository.NoSuchOfferError
import auto.dealers.trade_in_notifier.storage.postgres.{PgOfferLogRepository, PgTradeInMQRepository}
import auto.dealers.trade_in_notifier.storage.{OfferLogRepository, TradeInMQRepository}
import zio._
import zio.interop.catz._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect._

object TradeInMQRepositorySpec extends DefaultRunnableSpec {

  private val simpleCarInfo = SimplifiedOffer.SimplifiedCarInfo(
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

  private val simpleOffer = SimplifiedOffer(
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

  private val now = OffsetDateTime
    .now()
    .truncatedTo(ChronoUnit.DAYS)
    .plusHours(1)

  private def enqueueNewTradeInRecord = testM("enqueue new trade-in record") {
    val record = (uuid: UUID) => TradeInMQRepository.TradeInRecord(1, "email", uuid)

    for {
      id <- OfferLogRepository.append(simpleOffer, now)
      _ <- TradeInMQRepository.enqueue(record(id))
      exists <- isTradeInExist(id)
    } yield assert(exists)(isTrue)
  }

  private def testSize = testM("queue size test") {
    val record = (uuid: UUID) => TradeInMQRepository.TradeInRecord(1, "email", uuid)

    for {
      id1 <- OfferLogRepository.append(simpleOffer, now)
      id2 <- OfferLogRepository.append(simpleOffer, now.plusDays(1))
      _ <- TradeInMQRepository.enqueue(Seq(record(id1), record(id2)))
      size <- TradeInMQRepository.size
    } yield assertTrue(size == 2L)
  }

  private def enqueueNewTradeInRecordBatch = testM("enqueue new trade-in record") {
    val record = (uuid: UUID) => TradeInMQRepository.TradeInRecord(1, "email", uuid)

    for {
      id <- OfferLogRepository.append(simpleOffer, now)
      _ <- TradeInMQRepository.enqueue(Seq(record(id)))
      exists <- isTradeInExist(id)
    } yield assert(exists)(isTrue)
  }

  private def addNewTradeInWithoutOffer =
    testM("should fail if try to add trade-in record without reference offer") {
      val record = TradeInMQRepository.TradeInRecord(1, "email", UUID.randomUUID())

      assertM(TradeInMQRepository.enqueue(record).run)(fails(isSubtype[NoSuchOfferError](anything)))
    }

  private def addNewTradeInWithoutOfferBatch =
    testM("should fail if try to add trade-in record without reference offer") {
      val record = TradeInMQRepository.TradeInRecord(1, "email", UUID.randomUUID())

      assertM(TradeInMQRepository.enqueue(Seq(record)).run)(fails(isSubtype[NoSuchOfferError](anything)))
    }

  private val drainOffers = testM("drain enqueued records and remove it after successful processing") {
    val record = (uuid: UUID) => TradeInMQRepository.TradeInRecord(1, "email", uuid)

    for {
      ref <- Ref.make(false)
      id <- OfferLogRepository.append(simpleOffer, now)
      _ <- TradeInMQRepository.enqueue(record(id))
      _ <- TradeInMQRepository.drainN(1, _ => ref.set(true))
      result <- ref.get
      exists <- isTradeInExist(id)
    } yield assert(result)(isTrue) && assert(exists)(isFalse)
  }

  private val failedDrain = testM("failed drain should not delete trade-in record") {
    val record = (uuid: UUID) => TradeInMQRepository.TradeInRecord(1, "email", uuid)

    for {
      errorRef <- Ref.make(false)
      id <- OfferLogRepository.append(simpleOffer, now)
      _ <- TradeInMQRepository.enqueue(record(id))
      _ <- TradeInMQRepository.drainN(
        1,
        _ => Task.fail(new Exception("some error")),
        (_: Throwable) => errorRef.set(true)
      )
      result <- errorRef.get
      exists <- isTradeInExist(id)
    } yield assert(result)(isTrue) && assert(exists)(isTrue)
  }

  def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    val suit = suite("TradeInMQRepositorySpec")(
      enqueueNewTradeInRecord,
      enqueueNewTradeInRecordBatch,
      testSize,
      addNewTradeInWithoutOffer,
      addNewTradeInWithoutOfferBatch,
      drainOffers,
      failedDrain
    ) @@ beforeAll(dbInit) @@ after(dbClean) @@ sequential

    suit.provideCustomLayerShared(
      TestPostgresql.managedTransactor >+> (PgOfferLogRepository.live ++ PgTradeInMQRepository.live)
    )
  }

  private val dbInit: URIO[Has[doobie.Transactor[Task]], Unit] = ZIO
    .service[Transactor[Task]]
    .flatMap(InitSchema("/schema.sql", _))
    .orDie

  private val dbClean = ZIO
    .service[Transactor[Task]]
    .flatMap { xa =>
      sql"DELETE FROM trade_in_notification_buffer".update.run.transact(xa) *>
        sql"DELETE FROM trade_in_offer_log".update.run.transact(xa)
    }

  private def isTradeInExist(uuid: UUID): URIO[Has[Transactor[Task]], Boolean] =
    ZIO
      .service[Transactor[Task]]
      .flatMap { xa =>
        sql"select exists (select 1 from trade_in_notification_buffer where trade_in_id = $uuid)"
          .query[Boolean]
          .unique
          .transact(xa)
          .orDie
      }
}
