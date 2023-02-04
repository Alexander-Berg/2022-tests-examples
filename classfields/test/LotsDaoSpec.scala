package auto.c2b.lotus.storage.test

import auto.c2b.common.model.BuyOutAlg
import auto.c2b.common.model.LotTypes.{Price, UserId}
import auto.c2b.common.model.paging.PagingRequest
import auto.c2b.common.postgresql.PgOrder
import auto.c2b.common.prices.PriceRange
import auto.c2b.lotus.model._
import auto.c2b.lotus.model.errors.LotusError
import auto.c2b.lotus.model.testkit.Generators
import auto.c2b.lotus.storage.LotsDao.LotsDao
import auto.c2b.lotus.storage.postgresql.{LotColumns, PgBidsDao, PgLotsDao}
import auto.c2b.lotus.storage.{BidsDao, LotsDao}
import cats.implicits._
import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import doobie.Transactor
import eu.timepit.refined.api.Refined
import zio.random.Random
import zio.test.Assertion._
import zio.test.TestAspect.{sequential, shrinks}
import zio.test._
import zio.test.environment.TestEnvironment
import zio.{Has, Task, ZIO}

import java.time.Instant
import java.time.temporal.ChronoUnit

object LotsDaoSpec extends DefaultRunnableSpec {

  def truncTimes(l: Lot) = {
    def truncTime(t: Instant) = t.truncatedTo(ChronoUnit.MILLIS)

    l.copy(
      createdAt = truncTime(l.createdAt),
      startAt = truncTime(l.startAt),
      finishAt = l.finishAt.map(truncTime)
    )
  }

  override def spec: ZSpec[TestEnvironment, Any] = {
    (suite("LotsDao")(
      testM("создаёт лот") {
        checkNM(1)(Generators.Lot.paramForNewLotAny) {
          case (
                applicationId,
                offer,
                proAutoReport,
                startAt,
                startingPrice,
                appraisalId,
                createdAt
              ) =>
            for {
              dao <- ZIO.service[LotsDao.Service]
              lotOrError <- dao
                .create(
                  applicationId,
                  Some(offer),
                  createdAt,
                  proAutoReport,
                  startAt,
                  startingPrice,
                  appraisalId,
                  None,
                  None,
                  BuyOutAlg.Auction,
                  Some(PriceRange(2398, 295857))
                )
                .transactIO
              lot <- ZIO.fromEither(lotOrError)
              saved <- dao.get(LotGetter(id = Some(lot.id))).transactIO
            } yield {
              assert(saved)(isSome) &&
              assertTrue(saved.get.status == LotStatus.New) &&
              assertTrue(saved.get.applicationId == applicationId) &&
              assertTrue(saved.get.createdAt == lot.createdAt.truncatedTo(ChronoUnit.MICROS)) &&
              assertTrue(saved.get.offer == Some(offer)) &&
              assertTrue(saved.get.proAutoReport == proAutoReport) &&
              assertTrue(saved.get.startAt.getEpochSecond == startAt.getEpochSecond) &&
              assertTrue(saved.get.startingPrice == startingPrice) &&
              assertTrue(saved.get.prolongedTimes == 0) &&
              assertTrue(saved.get.finishAt.isEmpty) &&
              assertTrue(saved.get.currentPrice.isEmpty) &&
              assertTrue(saved.get.finalPrice.isEmpty) &&
              assertTrue(saved.get.clientId.isEmpty) &&
              assertTrue(saved.get.userId.isEmpty) &&
              assertTrue(saved.get.bidsCount == 0) &&
              assertTrue(saved.get.topPropositions.isEmpty)
              assertTrue(saved.get.pricePredictionRange.get == PriceRange(2398, 295857))
            }
        }
      },
      testM("возвращает лот c select for update") {
        checkNM(1)(Gen.listOfN(1)(Generators.Lot.lotAny)) { lots =>
          val lot = lots.head
          for {
            dao <- ZIO.service[LotsDao.Service]
            createdLot <- createLot(lot, dao)
            saved <- dao.getWithLock(createdLot.id).transactIO
          } yield assert(saved)(isSome(equalTo(createdLot)))
        }
      },
      testM("возвращает лот") {
        checkNM(1)(Gen.listOfN(2)(Generators.Lot.lotAny)) { lots =>
          val lot = lots(1)
          for {
            dao <- ZIO.service[LotsDao.Service]
            createdLot <- createLot(lot, dao)
            saved <- dao.get(LotGetter(id = Some(createdLot.id))).transactIO
          } yield assert(saved)(isSome(equalTo(createdLot)))
        }
      },
      testM("удаляет лот") {
        checkNM(1)(Gen.listOfN(3)(Generators.Lot.lotAny)) { lots =>
          val lot = lots(2)
          for {
            dao <- ZIO.service[LotsDao.Service]
            createdLot <- createLot(lot, dao)
            saved <- dao.get(LotGetter(id = Some(createdLot.id))).transactIO
            _ <- dao.delete(createdLot.id).transactIO
            afterDelete <- dao.get(LotGetter(id = Some(createdLot.id))).transactIO
          } yield assert(saved)(isSome) && assert(afterDelete)(isNone)
        }
      },
      testM("возвращает список лотов") {
        checkNM(1)(Gen.listOfN(14)(Generators.Lot.lotAny), Generators.Lot.userIdGen.derive) { (lotsSample, userId) =>
          val lots = lotsSample.drop(4)
          for {
            dao <- ZIO.service[LotsDao.Service]
            createdLots <- ZIO.foreach(lots)(createLot(_, dao))
            withWinner = createdLots.map(_.copy(userId = Some(userId)))
            updated <- ZIO.foreach(withWinner)(dao.update(_).transactIO.absolve[Any, Lot])
            got <- dao
              .list(
                LotFilter(userId = Some(userId), isWon = true),
                PagingRequest(10, 1),
                LotOrder(userId = Some(PgOrder.DESC))
              )
              .transactIO
          } yield {
            val descLots = updated.sortBy(_.userId.get).reverse
            assertTrue(got.lots == descLots) &&
            assertTrue(got.page.total == descLots.size)
          }
        }
      },
      testM("возвращает список активных лотов") {
        checkNM(1)(
          Gen.listOfN(35)(Generators.Lot.lotAny),
          Generators.Lot.clientIdGen.derive,
          Generators.Lot.userIdGen.derive,
          Generators.Lot.userIdGen.derive
        ) { (lotsSample, clientId, userId, anotherUserId) =>
          def mkPrice: Long => Price = Refined.unsafeApply
          val lots = lotsSample.drop(14)
          for {
            random <- ZIO.service[Random.Service]
            lotsDao <- ZIO.service[LotsDao.Service]
            bidsDao <- ZIO.service[BidsDao.Service]
            createdLots <- ZIO.foreach(lots)(createLot(_, lotsDao))
            activeLots = createdLots.drop(10).map(_.copy(status = LotStatus.Auction))
            finishedLots = createdLots.take(10).map(_.copy(status = LotStatus.Finished))
            _ <- ZIO.foreach_(activeLots ++ finishedLots)(lotsDao.update(_).transactIO.absolve[Any, Lot])
            bids <- ZIO.foreach(activeLots.drop(5) ++ finishedLots) { lot =>
              val price = random.nextLongBetween(0, 10000000).map(mkPrice)
              price >>= { price =>
                bidsDao.create(lot.id, clientId, userId, price, false).transactIO.absolve[Any, Bid] <*
                  bidsDao
                    .create(lot.id, clientId, anotherUserId, mkPrice(price.value + 1), false)
                    .transactIO
                    .absolve[Any, Bid]
              }
            }
            anotherBids <- ZIO.foreach(activeLots.take(5)) { lot =>
              val price = random.nextLongBetween(0, 10000000).map(mkPrice)
              price >>= { bidsDao.create(lot.id, clientId, anotherUserId, _, false).transactIO.absolve[Any, Bid] }
            }
            got <- lotsDao
              .list(
                LotFilter(statuses = Set(LotStatus.Auction), userId = Some(userId), myActivity = true),
                PagingRequest(5, 1)
              )
              .transactIO
            anotherGot <- lotsDao
              .list(
                LotFilter(statuses = Set(LotStatus.Auction), userId = Some(anotherUserId), myActivity = true),
                PagingRequest(5, 1)
              )
              .transactIO
          } yield {
            val accepted =
              activeLots.mapFilter(lot => bids.find(_.lotId == lot.id).as(lot)).sortBy(_.id.value).reverse.take(5)
            val anotherAccepted =
              activeLots
                .mapFilter(lot => anotherBids.find(_.lotId == lot.id).orElse(bids.find(_.lotId == lot.id)).as(lot))
                .sortBy(_.id.value)
                .reverse
                .take(5)

            assertTrue(
              got.lots == accepted &&
                got.page.page == 1 &&
                got.page.pageSize == 5 &&
                got.page.total == 6 &&
                anotherGot.lots == anotherAccepted &&
                anotherGot.page.page == 1 &&
                anotherGot.page.pageSize == 5 &&
                anotherGot.page.total == 11
            )
          }
        }
      },
      testM("update") {
        checkNM(1)(Generators.Lot.lotAny, Generators.Lot.lotAny) { (first, second) =>
          for {
            dao <- ZIO.service[LotsDao.Service]
            _ <- cleanLots()
            createLotId <- createLot(first, dao).map(_.id)
            _ <- dao
              .update(second.copy(id = createLotId))
              .transactIO
              .mapBoth(e => LotusError.UnknownDbError(e.cause), _.getOrElse(???))
            fromDb <- dao.get(LotGetter(Some(createLotId))).transactIO.mapError(e => LotusError.UnknownDbError(e.cause))
            expected = truncTimes(second.copy(id = createLotId, createdAt = first.createdAt))
          } yield assertTrue(truncTimes(fromDb.get) == expected)
        }
      }
    ) @@ sequential @@ shrinks(0))
      .provideCustomLayerShared {
        (TestPostgresql.managedTransactor >+> PgLotsDao.live) ++
          (TestPostgresql.managedTransactor >+> PgBidsDao.live)
      }
  }

  private def cleanLots() = {
    import doobie.implicits.toSqlInterpolator
    sql"""truncate ${LotColumns.tableName}""".update.run.transactIO.mapError(_.cause)
  }

  private def createLot(
      lot: Lot,
      dao: LotsDao.Service): ZIO[Has[Transactor[Task]] with LotsDao, LotusError.DbError, Lot] = {
    for {
      newLot <- dao
        .create(
          lot.applicationId,
          lot.offer,
          lot.createdAt,
          lot.proAutoReport,
          lot.startAt,
          lot.startingPrice,
          lot.appraisalId,
          lot.carInfo,
          lot.inspectionId,
          BuyOutAlg.Auction,
          None
        )
        .transactIO
        .mapError(e => LotusError.UnknownDbError(e.cause))
        .absolve[LotusError.DbError, Lot]
      updatedLot <- dao
        .update(lot.copy(id = newLot.id))
        .transactIO
        .mapError(e => LotusError.UnknownDbError(e.cause))
        .absolve[LotusError.DbError, Lot]
    } yield updatedLot
  }
}
