package auto.c2b.lotus.storage.test

import auto.c2b.common.model.LotTypes.{ClientId, LotId, Price, UserId}
import auto.c2b.common.postgresql.PgOrder
import auto.c2b.common.testkit.CommonGenerators
import auto.c2b.lotus.model.errors.LotusError
import auto.c2b.lotus.model.testkit.Generators
import auto.c2b.lotus.model.{Bid, BidOrder}
import auto.c2b.lotus.storage.BidsDao
import auto.c2b.lotus.storage.postgresql.BidColumns.tableName
import auto.c2b.lotus.storage.postgresql.PgBidsDao
import common.zio.doobie.DbError.DbException
import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import doobie.implicits.toSqlInterpolator
import doobie.syntax.stream._
import doobie.util.transactor.Transactor
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV
import zio.interop.catz._
import zio.stream.interop.fs2z._
import zio.test.Assertion._
import zio.test.TestAspect.{sequential, shrinks}
import zio.test._
import zio.test.environment.TestEnvironment
import zio.{Has, IO, Task, ZIO}

object BidsDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    (suite("BidsDao")(
      testM("создаёт ставку") {
        checkNM(1)(Generators.Bid.paramForNewBidAny) { case (lotId, userId, clientId, value) =>
          for {
            dao <- ZIO.service[BidsDao.Service]
            bidOrError <- dao.create(lotId, clientId, userId, value, false).transactIO
            bid <- ZIO.fromEither(bidOrError)
            fromDb <- dao.listBidsByLotIds(Set(lotId), None).transactIO.map(_.find(_.id == bid.id))
          } yield {
            assert(fromDb)(isSome) &&
            assertTrue(fromDb.get.lotId == lotId) &&
            assertTrue(fromDb.get.userId == userId) &&
            assertTrue(fromDb.get.value == value)
          }
        }
      },
      testM("возвращает список ставок") {
        checkNM(1)(
          Gen.listOfN(14)(Generators.Bid.bidAny),
          Generators.Bid.userIdGen.derive,
          CommonGenerators.refinedNonNegativeLongDeriveGen.derive
        ) { (bidsSample, userId, lotId) =>
          val bids = bidsSample.drop(4).map(_.copy(userId = userId, lotId = lotId))
          for {
            xa <- ZIO.service[Transactor[Task]]
            dao <- ZIO.service[BidsDao.Service]
            createdBids <- ZIO.foreach(bids)(b => createBid(b.copy(preOffer = false), dao))
            streamed <- dao.listAll(lotId).transact(xa).toZStream().runCollect
          } yield {
            val descBids = createdBids.sortBy(_.id.value).reverse
            assertTrue(streamed.size == descBids.size) &&
            assertTrue(streamed.toList.reverse == descBids)
          }
        }
      },
      testM("возвращает n ставок по id лота") {
        checkNM(1)(Gen.listOfN(100)(Generators.Bid.bidAny), Gen.listOfN(5)(Generators.Lot.lotAny.map(_.id))) {
          (bidsSample, lots) =>
            val allBids = bidsSample.drop(90)
            val firstHalf = allBids.take(5).map(_.copy(lotId = lots.head))
            val secondHalf = allBids.drop(5).map(_.copy(lotId = lots.last))
            for {
              dao <- ZIO.service[BidsDao.Service]
              createdBids <- ZIO.foreach(firstHalf ::: secondHalf)(createBid(_, dao))
              limited <- dao
                .listBidsByLotIds(lots.toSet, Some(2))
                .transactIO
              notLimited <- dao
                .listBidsByLotIds(lots.toSet, None)
                .transactIO
            } yield {
              assertTrue(createdBids.size == 10) &&
              assertTrue(limited.size == 4) &&
              assertTrue(createdBids.sortBy(_.id.value)(Ordering[Long].reverse) == notLimited) &&
              assertTrue(notLimited.size == 10)
            }
        }
      },
      testM("возвращает наибольшую последнюю ставку") {
        checkNM(1)(Generators.Bid.paramForNewBidAny) { case (lotId, firstUserId, firstClientId, _) =>
          for {
            dao <- ZIO.service[BidsDao.Service]

            firstPrice <- createNonNegativeNumber(100)
            secondPrice <- createNonNegativeNumber(90)
            _ <- dao.create(lotId, firstClientId, firstUserId, firstPrice, false).transactIO
            _ <- dao.create(lotId, firstClientId, firstUserId, secondPrice, false).transactIO

            thirdPrice <- createNonNegativeNumber(100)
            secondClientId = ClientId("1111345")
            secondUserId = UserId("1111345")
            _ <- dao.create(lotId, secondClientId, secondUserId, thirdPrice, false).transactIO

            saved <- dao.findWinnerBid(lotId).transactIO
          } yield {
            assert(saved)(isSome) &&
            assertTrue(saved.get.value.value == 100) &&
            assertTrue(saved.get.userId == secondUserId)
          }
        }
      },
      testM("возвращает наибольшую ставку сделанную ранее, если есть две одинаковые") {
        checkNM(1)(Generators.Bid.paramForNewBidAny) { case (lotId, firstUserId, firstClientId, _) =>
          for {
            dao <- ZIO.service[BidsDao.Service]

            firstPrice <- createNonNegativeNumber(100)
            secondPrice <- createNonNegativeNumber(90)
            _ <- dao.create(lotId, firstClientId, firstUserId, firstPrice, false).transactIO
            _ <- dao.create(lotId, firstClientId, firstUserId, secondPrice, false).transactIO

            thirdPrice <- createNonNegativeNumber(90)
            secondClientId = ClientId("1111345")
            secondUserId = UserId("1111345")
            _ <- dao.create(lotId, secondClientId, secondUserId, thirdPrice, false).transactIO

            saved <- dao.findWinnerBid(lotId).transactIO
          } yield {
            assert(saved)(isSome) &&
            assertTrue(saved.get.value.value == 90) &&
            assertTrue(saved.get.userId == firstUserId)
          }
        }
      },
      testM("ставок во втором круге нет") {
        checkNM(1)(Generators.Bid.paramForNewBidAny) { case (_, firstUserId, firstClientId, _) =>
          for {
            dao <- ZIO.service[BidsDao.Service]
            lotId <- createNonNegativeNumber(133333)
            firstPrice <- createNonNegativeNumber(100)
            secondPrice <- createNonNegativeNumber(90)
            _ <- dao.create(lotId, firstClientId, firstUserId, firstPrice, true).transactIO
            _ <- dao.create(lotId, firstClientId, firstUserId, secondPrice, true).transactIO
            saved <- dao.findWinnerBid(lotId).transactIO
          } yield {
            assert(saved)(isNone)
          }
        }
      },
      testM("listLastUserBids") {
        checkNM(1)(Generators.Bid.paramForNewBidAny, Generators.Bid.paramForNewBidAny) {
          case ((lotId1, userId1, clientId, _), (lotId2, userId2, _, _)) =>
            def check(
                ids: Set[LotId],
                userId: Option[UserId] = None,
                isPreOffer: Option[Boolean] = None,
                order: BidOrder = BidOrder()
              )(assertions: List[Bid] => TestResult): ZIO[Has[Transactor[Task]] with Has[BidsDao.Service], DbException, TestResult] = {
              ZIO
                .service[BidsDao.Service]
                .flatMap(dao => dao.listLastUserBids(ids, userId, isPreOffer, order).transactIO)
                .map(assertions)
            }

            def bidsToTuple(bids: List[Bid]): Seq[(LotId, UserId, Long, Boolean)] =
              bids.map(b => (b.lotId, b.userId, b.value.value, b.preOffer))

            for {
              _ <- cleanBids()
              price1 <- createNonNegativeNumber(18577)
              price2 <- createNonNegativeNumber(228572)
              price3 <- createNonNegativeNumber(3195873)
              price4 <- createNonNegativeNumber(483375)
              price5 <- createNonNegativeNumber(52592)
              price6 <- createNonNegativeNumber(6223)
              price7 <- createNonNegativeNumber(4917571)
              price8 <- createNonNegativeNumber(19919285)
              price9 <- createNonNegativeNumber(986582093322L)
              _ <- createBid(lotId1, clientId, userId1, price1, true)
              _ <- createBid(lotId1, clientId, userId1, price2, true)
              withNoFilters <- check(Set(lotId1)) { res =>
                assertTrue(res.size == 1) &&
                assertTrue(res.head.value == price2) &&
                assertTrue(res.head.userId == userId1)
              }
              _ <- createBid(lotId1, clientId, userId1, price3, false)
              withUserIdAndIsPreOffer <- check(Set(lotId1), userId = Some(userId1), isPreOffer = Some(false)) { res =>
                assertTrue(res.size == 1) &&
                assertTrue(res.head.value == price3) &&
                assertTrue(res.head.userId == userId1)
              }
              noResultForWrongUserId <- check(
                Set(lotId1),
                userId = Some(userId2),
                order = BidOrder(id = Some(PgOrder.ASC))
              )(lst => assertTrue(lst.isEmpty))
              // user2 for lot1
              _ <- createBid(lotId1, clientId, userId2, price4, true)
              _ <- createBid(lotId1, clientId, userId2, price5, false)
              _ <- createBid(lotId1, clientId, userId2, price6, false)

              // lot2
              _ <- createBid(lotId2, clientId, userId1, price7, true)
              _ <- createBid(lotId2, clientId, userId1, price8, false)
              _ <- createBid(lotId2, clientId, userId2, price9, true)

              forOneUser <- check(
                Set(lotId1, lotId2),
                userId = Some(userId2),
                order = BidOrder(id = Some(PgOrder.ASC))
              ) { res =>
                assertTrue(
                  bidsToTuple(res) == List(
                    (lotId1, userId2, price4.value, true),
                    (lotId1, userId2, price6.value, false),
                    (lotId2, userId2, price9.value, true)
                  )
                )
              }

              allInCorrectOrder <- check(Set(lotId1, lotId2), order = BidOrder(value = Some(PgOrder.DESC))) { res =>
                val expected = List(
                  (lotId1, userId1, price2.value, true),
                  (lotId1, userId1, price3.value, false),
                  (lotId1, userId2, price4.value, true),
                  (lotId1, userId2, price6.value, false),
                  (lotId2, userId1, price7.value, true),
                  (lotId2, userId1, price8.value, false),
                  (lotId2, userId2, price9.value, true)
                ).sortBy(-_._3)

                assertTrue(bidsToTuple(res) == expected)
              }
            } yield withNoFilters && withUserIdAndIsPreOffer && noResultForWrongUserId && allInCorrectOrder && forOneUser
        }
      }
    ) @@ sequential @@ shrinks(0))
      .provideCustomLayerShared {
        TestPostgresql.managedTransactor >+> PgBidsDao.live
      }
  }

  private def createBid(
      lotId: LotId,
      clientId: ClientId,
      userId: UserId,
      value: Price,
      preOffer: Boolean): ZIO[Has[Transactor[Task]] with Has[BidsDao.Service], LotusError.DbError, Bid] = {
    ZIO
      .service[BidsDao.Service]
      .flatMap(dao =>
        dao
          .create(lotId, clientId, userId, value, preOffer)
          .transactIO
          .mapError(e => LotusError.UnknownDbError(e.cause))
          .flatMap(ZIO.fromEither(_))
      )
  }

  private def createBid(bid: Bid, dao: BidsDao.Service): ZIO[Has[Transactor[Task]], LotusError.DbError, Bid] =
    createBid(bid.lotId, bid.clientId, bid.userId, bid.value, bid.preOffer)
      .provideSome(_.add(dao))

  private def cleanBids() = {
    sql"""truncate $tableName""".update.run.transactIO.mapError(_.cause)
  }

  private def createNonNegativeNumber(value: Long): IO[String, Refined[Long, NonNegative]] =
    ZIO.fromEither(refineV[NonNegative](value))
}
