package auto.dealers.trade_in_notifier.storage.postgres.test

import auto.dealers.trade_in_notifier.model.DealerId
import auto.dealers.trade_in_notifier.storage.OfferLogRepository
import auto.dealers.trade_in_notifier.storage.OfferLogRepository._
import auto.dealers.trade_in_notifier.storage.postgres.PgOfferLogRepository
import auto.dealers.trade_in_notifier.model.SimplifiedOffer
import auto.dealers.trade_in_notifier.model.SimplifiedOffer.{PriceRange, SimplifiedCarInfo}
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
import java.time.temporal.ChronoUnit
import java.time.OffsetDateTime
import java.util.UUID

object OfferLogRepositorySpec extends DefaultRunnableSpec {

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

  val appendAndGetTest =
    testM("Insert offer") {
      for {
        id <- OfferLogRepository.append(simpleOffer, now)
        result <- OfferLogRepository.get(id)
      } yield assert(result)(isSome(equalTo((simpleOffer, now))))
    }

  val appendDifferentOffersTest =
    testM("Insert two offers") {
      val secondOffer = simpleOffer.copy(
        name = "Mary",
        phones = List("79321234567"),
        offerId = None
      )
      for {
        id1 <- OfferLogRepository.append(simpleOffer, now)
        id2 <- OfferLogRepository.append(secondOffer, now)
        (offer1, _) <- OfferLogRepository.get(id1).map(_.get)
        (offer2, _) <- OfferLogRepository.get(id2).map(_.get)
      } yield assert((offer1, offer2))(equalTo((simpleOffer, secondOffer)))
    }

  val appendNonUniqueTest =
    testM("Insert non-unique offer") {
      val result = for {
        _ <- OfferLogRepository.append(simpleOffer, now)
        _ <- OfferLogRepository.append(simpleOffer, now)
      } yield ()

      assertM(result.run)(fails(equalTo(OfferIsNotUniqueError)))
    }

  val appendAfterAWhileTest =
    testM("Insert similar offer after a couple of hours") {
      for {
        id1 <- OfferLogRepository.append(simpleOffer, now)
        id2 <- OfferLogRepository.append(simpleOffer, now.plusHours(2))
        (offer1, _) <- OfferLogRepository.get(id1).map(_.get)
        (offer2, _) <- OfferLogRepository.get(id2).map(_.get)
      } yield assert((offer1, offer2))(equalTo((simpleOffer, simpleOffer)))
    }

  val malformedJsonTest =
    testM("Get malformed offer json") {
      val result = for {
        xa <- ZIO.service[Transactor[Task]]
        id <-
          sql"""INSERT INTO trade_in_offer_log(offer, md5sum, timestamp) VALUES ('{"name":"Bob"}', '1234', $now) RETURNING id"""
            .query[UUID]
            .unique
            .transact(xa)
        res <- OfferLogRepository.get(id)
      } yield res

      assertM(result.run)(fails(isSubtype[OfferParsingError](anything)))
    }

  val deleteOfferTest =
    testM("Delete offer") {
      for {
        id <- OfferLogRepository.append(simpleOffer, now)
        _ <- OfferLogRepository.delete(id)
        res <- OfferLogRepository.get(id)
      } yield assert(res)(isNone)
    }

  def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    val suit = suite("OfferLogRepositorySpec")(
      appendAndGetTest,
      appendDifferentOffersTest,
      appendNonUniqueTest,
      appendAfterAWhileTest,
      malformedJsonTest,
      deleteOfferTest
    ) @@ beforeAll(dbInit) @@ after(dbClean) @@ sequential

    suit.provideCustomLayerShared(TestPostgresql.managedTransactor >+> PgOfferLogRepository.live)
  }

  private val dbInit: URIO[Has[doobie.Transactor[Task]], Unit] = ZIO
    .service[Transactor[Task]]
    .flatMap(InitSchema("/schema.sql", _))
    .orDie

  private val dbClean = ZIO
    .service[Transactor[Task]]
    .flatMap { xa =>
      sql"DELETE FROM trade_in_offer_log".update.run.transact(xa)
    }

}
