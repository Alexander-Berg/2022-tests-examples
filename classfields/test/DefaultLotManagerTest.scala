package auto.c2b.lotus.logic.test

import auto.c2b.common.model.{BuyOutAlg, FlowConfig}
import auto.c2b.common.model.LotTypes.{ClientId, LotId, Price, UserId}
import auto.c2b.common.model.paging.PagingRequest
import auto.c2b.common.prices.PriceRange
import auto.c2b.lotus.logic.AuctionTimeManager.AuctionTimeManager
import auto.c2b.lotus.logic.LotManager.LotManager
import auto.c2b.lotus.logic.LotStore.LotStore
import auto.c2b.lotus.logic.NotificationsManager.NotificationsManager
import auto.c2b.lotus.logic.testkit.{BrokerSenderManagerMock, NotificationsManagerMock}
import auto.c2b.lotus.logic.{DefaultAuctionTimeManager, LotManager, LotStore}
import auto.c2b.lotus.model.errors.LotusError
import auto.c2b.lotus.model.errors.LotusError.RequestMalformed
import auto.c2b.lotus.model.{BidFilter, ClientInfo, Lot, LotEvent, LotFilter, LotGetter, LotStatus, StartSchedule}
import auto.c2b.lotus.storage.LotsDao
import auto.c2b.lotus.storage.LotsDao.LotsDao
import auto.c2b.lotus.storage.postgresql.{PgBidsDao, PgLotsDao}
import auto.c2b.lotus.storage.BidsDao
import auto.c2b.lotus.storage.BidsDao.BidsDao
import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import common.zio.logging.Logging
import doobie.Transactor
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV
import ru.auto.api.api_offer_model.Offer
import zio.clock.Clock
import zio.magic._
import zio.test.TestAspect.sequential
import zio.test._
import zio.test.environment.{TestClock, TestEnvironment}
import zio.{clock, Has, IO, Queue, Task, ZIO, ZLayer}

import java.time._
import scala.concurrent.duration._

object DefaultLotManagerTest extends DefaultRunnableSpec {
  private val timeZone = ZoneId.of("Europe/Moscow")

  override def spec: ZSpec[TestEnvironment, Any] = {
    (suite("DefaultLotManager")(
      testM("Start hour later if matches schedule") {
        for {
          _ <- TestClock.setDateTime(OffsetDateTime.of(2007, 1, 15, 14, 14, 14, 14, ZoneOffset.ofHours(3)))
          now <- clock.instant
          created <- createLot(1)
        } yield assertTrue {
          now.plusSeconds(60 * 60) == created.startAt
        }
      },
      testM("Select nearest available date and time") {
        for {
          _ <- TestClock.setDateTime(OffsetDateTime.of(2007, 4, 30, 22, 22, 22, 22, ZoneOffset.ofHours(3)))
          created <- createLot(2)
          startAt = created.startAt.atZone(timeZone)
        } yield assertTrue {
          startAt.getMonthValue == 5 &&
          startAt.getDayOfMonth == 2 &&
          startAt.getHour == 10 &&
          startAt.getMinute == 0
        }
      },
      testM("Makes successful bid") {
        for {
          lotDao <- ZIO.service[LotsDao.Service]
          _ <- TestClock.setDateTime(OffsetDateTime.of(2007, 4, 30, 22, 22, 22, 22, ZoneOffset.ofHours(3)))
          created <- createLot(13)
          startAt = created.startAt
          finishAt = Instant.ofEpochSecond(startAt.getEpochSecond + 3600)
          currentPrice <- createPrice(100)
          updatedLot <- updateLot(
            created.copy(finishAt = Some(finishAt), status = LotStatus.Auction, currentPrice = Some(currentPrice)),
            lotDao
          )
          newBid <- createPrice(150)
          addedBid <- LotManager.addBid(
            updatedLot.id,
            newBid,
            createClientInfo
          )
          lot <- getLotById(updatedLot.id, lotDao)
        } yield assertTrue {
          addedBid &&
          lot.currentPrice.exists(_.value == 150)
        }
      },
      testM("Makes unsuccessful bid because current price equals new bid") {
        for {
          lotDao <- ZIO.service[LotsDao.Service]
          bidDao <- ZIO.service[BidsDao.Service]

          _ <- TestClock.setDateTime(OffsetDateTime.of(2007, 4, 30, 22, 22, 22, 22, ZoneOffset.ofHours(3)))
          created <- createLot(133)
          startAt = created.startAt
          finishAt = Instant.ofEpochSecond(startAt.getEpochSecond + 3600)
          currentPrice <- createPrice(100)
          updatedLot <- updateLot(
            created.copy(finishAt = Some(finishAt), status = LotStatus.Auction, currentPrice = Some(currentPrice)),
            lotDao
          )
          newBid <- createPrice(150)
          addedBidFirst <- LotManager.addBid(
            updatedLot.id,
            newBid,
            createClientInfo
          )
          addedBidFSecond <- LotManager.addBid(
            updatedLot.id,
            newBid,
            createClientInfo
          )
          lot <- getLotById(updatedLot.id, lotDao)
          bids <- getBidByLotId(updatedLot.id, bidDao)
        } yield assertTrue {
          addedBidFirst &&
          !addedBidFSecond &&
          lot.currentPrice.exists(_.value == 150) &&
          bids.size == 1
        }
      },
      testM("Checking prolong time for auction") {
        for {
          lotDao <- ZIO.service[LotsDao.Service]
          auctionConfig <- ZIO.service[LotManager.AuctionConfig]
          _ <- TestClock.setDateTime(OffsetDateTime.of(2022, 2, 7, 13, 0, 0, 0, ZoneOffset.ofHours(3)))
          created <- createLot(1000)
          startAt = created.startAt
          finishAt = Instant.ofEpochSecond(startAt.getEpochSecond + 60)
          currentPrice <- createPrice(100)
          updatedLot <- updateLot(
            created.copy(finishAt = Some(finishAt), status = LotStatus.Auction, currentPrice = Some(currentPrice)),
            lotDao
          )
          newBid <- createPrice(150)
          _ <- TestClock.setDateTime(OffsetDateTime.of(2022, 2, 7, 14, 0, 30, 0, ZoneOffset.ofHours(3)))
          addedBid <- LotManager.addBid(
            updatedLot.id,
            newBid,
            createClientInfo
          )
          lot <- getLotById(updatedLot.id, lotDao)
        } yield assertTrue {
          addedBid && lot.currentPrice.exists(_.value == 150) && lot.finishAt.exists(newFinishAt =>
            newFinishAt.getEpochSecond == (finishAt.getEpochSecond + auctionConfig.prolongationTime.toSeconds)
          ) && lot.prolongedTimes == 1
        }
      },
      testM("Successful addition of two bids") {
        for {
          lotDao <- ZIO.service[LotsDao.Service]
          bidDao <- ZIO.service[BidsDao.Service]
          _ <- TestClock.setDateTime(OffsetDateTime.of(2007, 4, 30, 22, 22, 22, 22, ZoneOffset.ofHours(3)))
          created <- createLot(1001)
          startAt = created.startAt
          finishAt = Instant.ofEpochSecond(startAt.getEpochSecond + 3600)
          currentPrice <- createPrice(100)
          updatedLot <- updateLot(
            created.copy(finishAt = Some(finishAt), status = LotStatus.Auction, currentPrice = Some(currentPrice)),
            lotDao
          )
          firstBid <- createPrice(150)
          addedBidFirst <- LotManager.addBid(
            updatedLot.id,
            firstBid,
            createClientInfo
          )
          secondBid <- createPrice(160)
          addedBidFSecond <- LotManager.addBid(
            updatedLot.id,
            secondBid,
            createClientInfo
          )
          lot <- getLotById(updatedLot.id, lotDao)
          bids <- getBidByLotId(lot.id, bidDao)
        } yield assertTrue {
          addedBidFirst &&
          addedBidFSecond &&
          lot.currentPrice.exists(_.value == 160) &&
          bids.size == 2 && lot.bidsCount == 2
        }
      },
      testM("Successful get information about Lot (bidsCount, bids)") {
        for {
          lotDao <- ZIO.service[LotsDao.Service]
          bidDao <- ZIO.service[BidsDao.Service]
          _ <- TestClock.setDateTime(OffsetDateTime.of(2007, 4, 30, 22, 22, 22, 22, ZoneOffset.ofHours(3)))
          created <- createLot(1002)
          createdSecondLot <- createLot(666)
          startAt = created.startAt
          finishAt = Instant.ofEpochSecond(startAt.getEpochSecond + 3600)
          currentPrice <- createPrice(100)
          updatedLot <- updateLot(
            created.copy(finishAt = Some(finishAt), status = LotStatus.Auction, currentPrice = Some(currentPrice)),
            lotDao
          )
          updateSecondLot <- updateLot(
            createdSecondLot
              .copy(finishAt = Some(finishAt), status = LotStatus.Auction, currentPrice = Some(currentPrice)),
            lotDao
          )

          firstBid <- createPrice(150)
          addedBidFirst <- LotManager.addBid(
            updatedLot.id,
            firstBid,
            createClientInfo
          )
          addedBidFirstForSecondLot <- LotManager.addBid(
            updateSecondLot.id,
            firstBid,
            createClientInfo
          )

          secondBid <- createPrice(160)
          addedBidSecond <- LotManager.addBid(
            updatedLot.id,
            secondBid,
            createClientInfo
          )
          addedBidSecondForSecondLot <- LotManager.addBid(
            updateSecondLot.id,
            secondBid,
            createClientInfo
          )
          thirdBid <- createPrice(170)
          addedBidThird <- LotManager.addBid(
            updatedLot.id,
            thirdBid,
            createClientInfo
          )
          addedBidThirdForSecondLot <- LotManager.addBid(
            updateSecondLot.id,
            thirdBid,
            createClientInfo
          )
          userId = createClientInfo.userId
          lotInfo <- LotManager.listLotsWithBidsCount(LotFilter(Set(LotStatus.Auction)), PagingRequest(10, 1), userId)
          lotFirst = lotInfo._2.find(_.lot.id == created.id)
          lotSecond = lotInfo._2.find(_.lot.id == updateSecondLot.id)
        } yield assertTrue {
          addedBidFirst && addedBidSecond && addedBidThird &&
          addedBidFirstForSecondLot && addedBidSecondForSecondLot && addedBidThirdForSecondLot && lotFirst.nonEmpty && lotSecond.nonEmpty &&
          lotFirst.get.bids.size == 2 && lotFirst.get.lot.bidsCount == 3 && lotSecond.get.bids.size == 2 && lotSecond.get.lot.bidsCount == 3
        }
      }
    ) @@ sequential).provideCustomLayerShared {
      ZLayer
        .wireSome[
          TestEnvironment,
          LotManager with AuctionTimeManager with TestClock with Clock with NotificationsManager with LotStore with BidsDao with LotsDao with Has[
            Transactor[Task]
          ] with Has[LotManager.AuctionConfig] with Logging.Logging with Has[Queue[LotEvent]] with Has[FlowConfig]
        ](
          ZLayer.succeed(
            DefaultAuctionTimeManager.Config(
              StartSchedule(
                time = StartSchedule.StartTime(
                  from = LocalTime.of(10, 0),
                  to = LocalTime.of(18, 0)
                ),
                workingDaysOfWeek = Set(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY),
                holidays = Set(MonthDay.of(5, 1)),
                auctionTimeZone = timeZone,
                workingWeekends = Set(MonthDay.of(3, 5)),
                confirmDays = 3
              )
            )
          ),
          ZLayer.succeed(FlowConfig(false)),
          ZLayer.succeed(
            LotManager.AuctionConfig(
              prolongationCondition = 90.seconds,
              prolongationTime = 300.seconds
            )
          ),
          TestPostgresql.managedTransactor,
          PgLotsDao.live,
          LotManager.live,
          NotificationsManagerMock.live,
          BrokerSenderManagerMock.live,
          LotStore.live,
          PgBidsDao.live,
          Logging.live,
          DefaultAuctionTimeManager.live,
          Queue.sliding[LotEvent](11).toLayer
        )
    }
  }

  private def createPrice(value: Long): IO[RequestMalformed, Price] = ZIO
    .fromEither(refineV[NonNegative](value))
    .mapError(RequestMalformed("price", value, _))

  private def createLot(applicationId: Long) = {
    LotManager.createLot(
      applicationId,
      Some(Offer.defaultInstance),
      "http://somereport.com",
      668L,
      Some(342342L),
      None,
      Some(1111),
      BuyOutAlg.Auction,
      Some(PriceRange(2398, 295857))
    )
  }

  private def createClientInfo =
    ClientInfo(userId = UserId("111"), id = ClientId("111"))

  private def updateLot(lot: Lot, dao: LotsDao.Service): ZIO[Has[Transactor[Task]], LotusError, Lot] =
    for {
      lotOrError <- dao
        .update(lot)
        .transactIO
        .mapError(e => LotusError.UnknownDbError(e.cause))
      lot <- ZIO.fromEither(lotOrError)
    } yield lot

  private def getLotById(lotId: LotId, dao: LotsDao.Service) = {
    for {
      lotOrError <- dao
        .get(LotGetter(id = Some(lotId)))
        .transactIO
        .mapError(e => LotusError.UnknownDbError(e.cause))
      lot <- ZIO.fromOption(lotOrError)
    } yield lot
  }

  private def getBidByLotId(lotId: LotId, dao: BidsDao.Service) = {
    for {
      bids <- dao
        .listBidsByLotIds(Set(lotId), None)
        .transactIO
        .mapError(e => LotusError.UnknownDbError(e.cause))
    } yield bids
  }
}
