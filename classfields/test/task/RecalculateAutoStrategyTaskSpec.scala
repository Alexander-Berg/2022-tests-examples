package vsmoney.auction_auto_strategy.scheduler.test.task

import common.models.finance.Money.Kopecks
import vsmoney.auction_auto_strategy.clients.AuctionServiceClient
import vsmoney.auction_auto_strategy.model.{auction, _}
import vsmoney.auction_auto_strategy.model.auction.{
  AuctionContext,
  AuctionContextId,
  AuctionKey,
  AuctionState,
  Bid,
  CriteriaContext,
  Criterion,
  CriterionKey,
  CriterionValue,
  UserBid
}
import vsmoney.auction_auto_strategy.model.common.{ProductId, Project, UserId}
import vsmoney.auction_auto_strategy.scheduler.service.AutoStrategyCalculatorService.CalculateException
import vsmoney.auction_auto_strategy.scheduler.service.{ActiveUserService, AutoStrategyCalculatorService}
import vsmoney.auction_auto_strategy.scheduler.task.RecalculateAutoStrategyTask
import vsmoney.auction_auto_strategy.scheduler.testkit.{ActiveUserServiceMock, AutoStrategyCalculatorServiceMock}
import vsmoney.auction_auto_strategy.services.testkit.{
  AuctionContextServiceMock,
  AuctionServiceMock,
  AutoStrategyServiceMock
}
import vsmoney.auction_auto_strategy.services.{AuctionContextService, AutoStrategyService}
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation._
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.{ZIO, ZLayer}

object RecalculateAutoStrategyTaskSpec extends DefaultRunnableSpec {

  private val taskLive = {
    ZLayer.fromServices[
      AuctionContextService,
      AutoStrategyService,
      AuctionServiceClient,
      ActiveUserService,
      AutoStrategyCalculatorService,
      RecalculateAutoStrategyTask
    ](
      new RecalculateAutoStrategyTask(_, _, _, _, _)
    )
  }

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("RecalculateAutoStrategyTask")(
      testM("recalculate auto strategies and send data to auction") {
        val mocks =
          AuctionContextServiceMock.GetAll(value(List(mockContext))) ++
            AutoStrategyServiceMock.AllForContext(
              equalTo(mockContext.auctionKey),
              value(List(testAutoStrategy))
            ) ++
            AuctionServiceMock.GetState(equalTo(mockContext.auctionKey), value(testAuctionState)) ++
            ActiveUserServiceMock.ForAuction(
              equalTo(mockContext.auctionKey),
              value((testAutoStrategy.userId :: auctionBids.map(_.userId)).toSet)
            ) ++
            AutoStrategyCalculatorServiceMock.Calculate(
              equalTo((mockContext, testAuctionState, List(testAutoStrategy))),
              value(List(AuctionAction.NoAction))
            )

        val test = ZIO
          .service[RecalculateAutoStrategyTask]
          .flatMap(task => task.program)

        assertM(test.run)(succeeds(anything))
          .provideCustomLayer {
            mocks >>> taskLive
          }
      },
      testM("not recalculate auto strategies if contextService did not return any records") {
        val mocks =
          AuctionContextServiceMock.GetAll(value(List()))
        val test = ZIO
          .service[RecalculateAutoStrategyTask]
          .flatMap(task => task.program)

        assertM(test.run)(succeeds(anything))
          .provideCustomLayer {
            AutoStrategyServiceMock.empty ++
              AuctionServiceMock.empty ++
              ActiveUserServiceMock.empty ++
              AutoStrategyCalculatorServiceMock.empty ++ mocks >>> taskLive
          }
      },
      testM("not recalculate auto strategies if AutoStrategyService did not return any records") {

        val mocks =
          AuctionContextServiceMock.GetAll(value(List(mockContext))) ++
            AutoStrategyServiceMock.AllForContext(
              equalTo(mockContext.auctionKey),
              value(List())
            )
        val test = ZIO
          .service[RecalculateAutoStrategyTask]
          .flatMap(task => task.program)

        assertM(test.run)(succeeds(anything))
          .provideCustomLayer {
            AuctionServiceMock.empty ++
              ActiveUserServiceMock.empty ++
              AutoStrategyCalculatorServiceMock.empty ++ mocks >>> taskLive
          }

      },
      testM("should filtered auction bids if user didn't have active offers") {
        val auctionStateWithUserWithoutOffers = testAuctionState.copy(
          bids = Seq(
            UserBid(userId = UserId("user10"), bid = Bid(Kopecks(300))),
            auction.UserBid(userId = UserId("user11"), bid = Bid(Kopecks(300)))
          ) ++ testAuctionState.bids
        )
        val mocks =
          AuctionContextServiceMock.GetAll(value(List(mockContext))) ++
            AutoStrategyServiceMock.AllForContext(
              equalTo(mockContext.auctionKey),
              value(List(testAutoStrategy))
            ) ++
            AuctionServiceMock.GetState(equalTo(mockContext.auctionKey), value(auctionStateWithUserWithoutOffers)) ++
            ActiveUserServiceMock.ForAuction(
              equalTo(mockContext.auctionKey),
              value((testAutoStrategy.userId :: auctionBids.map(_.userId)).toSet)
            ) ++
            AutoStrategyCalculatorServiceMock.Calculate(
              equalTo((mockContext, testAuctionState, List(testAutoStrategy))),
              value(List(AuctionAction.NoAction))
            )

        val test = ZIO
          .service[RecalculateAutoStrategyTask]
          .flatMap(task => task.program)

        assertM(test.run)(succeeds(anything))
          .provideCustomLayer {
            mocks >>> taskLive
          }
      },
      testM("should filtered auto strategies if users have auto strategies no have active offers") {
        val testAutoStrategyWithoutActiveOffers = AutoStrategy(
          id = 12,
          userId = UserId("user7"),
          auctionContextId = AuctionContextId(999L),
          settings = null
        )

        val mocks =
          AuctionContextServiceMock.GetAll(value(List(mockContext))) ++
            AutoStrategyServiceMock.AllForContext(
              equalTo(mockContext.auctionKey),
              value(List(testAutoStrategyWithoutActiveOffers))
            ) ++
            AuctionServiceMock.GetState(equalTo(mockContext.auctionKey), value(testAuctionState)) ++
            ActiveUserServiceMock.ForAuction(
              equalTo(mockContext.auctionKey),
              value((auctionBids.map(_.userId)).toSet)
            ) ++
            AutoStrategyCalculatorServiceMock.Calculate(
              equalTo((mockContext, testAuctionState, List())),
              value(List())
            )
        val test = ZIO
          .service[RecalculateAutoStrategyTask]
          .flatMap(task => task.program)

        assertM(test.run)(succeeds(anything))
          .provideCustomLayer {
            mocks >>> taskLive
          }
      },
      testM("should forward exception from auction service on auction get state") {
        val mocks =
          AuctionContextServiceMock.GetAll(failure(new Exception("test")))

        val test = ZIO
          .service[RecalculateAutoStrategyTask]
          .flatMap(task => task.program)

        assertM(test.run)(fails(isSubtype[Exception](anything)))
          .provideCustomLayer {
            AutoStrategyServiceMock.empty ++
              AuctionServiceMock.empty ++
              ActiveUserServiceMock.empty ++
              AutoStrategyCalculatorServiceMock.empty ++ mocks >>> taskLive
          }
      },
      testM("should forward exception from read auto strategies from auction context") {
        val mocks =
          AuctionContextServiceMock.GetAll(value(List(mockContext))) ++
            AutoStrategyServiceMock.AllForContext(
              equalTo(mockContext.auctionKey),
              failure(new ClassCastException("test"))
            )

        val test = ZIO
          .service[RecalculateAutoStrategyTask]
          .flatMap(task => task.program)

        assertM(test.run)(fails(isSubtype[ClassCastException](anything)))
          .provideCustomLayer {
            AuctionServiceMock.empty ++
              ActiveUserServiceMock.empty ++
              AutoStrategyCalculatorServiceMock.empty ++ mocks >>> taskLive
          }
      },
      testM("should forward Exception from calculate service") {
        val testAutoStrategyWithoutActiveOffers = AutoStrategy(
          id = 12,
          userId = UserId("user7"),
          auctionContextId = AuctionContextId(999L),
          settings = null
        )

        val mocks =
          AuctionContextServiceMock.GetAll(value(List(mockContext))) ++
            AutoStrategyServiceMock.AllForContext(
              equalTo(mockContext.auctionKey),
              value(List(testAutoStrategyWithoutActiveOffers))
            ) ++
            AuctionServiceMock.GetState(equalTo(mockContext.auctionKey), value(testAuctionState)) ++
            ActiveUserServiceMock.ForAuction(
              equalTo(mockContext.auctionKey),
              value((auctionBids.map(_.userId)).toSet)
            ) ++
            AutoStrategyCalculatorServiceMock.Calculate(
              equalTo((mockContext, testAuctionState, List())),
              failure(CalculateException("test"))
            )
        val test = ZIO
          .service[RecalculateAutoStrategyTask]
          .flatMap(task => task.program)

        assertM(test.run)(fails(isSubtype[CalculateException](anything)))
          .provideCustomLayer {
            mocks >>> taskLive
          }
      }
    )

  }

  private val testCriteriaContext = CriteriaContext(criteria =
    Seq(
      Criterion(key = CriterionKey("region_id"), value = CriterionValue("42")),
      Criterion(key = CriterionKey("mark"), value = CriterionValue("bmw")),
      Criterion(key = CriterionKey("model"), value = CriterionValue("x5"))
    )
  )

  private val testAuctionKey = AuctionKey(
    project = Project.Autoru,
    product = ProductId("call"),
    context = testCriteriaContext
  )

  private val mockContext: AuctionContext = AuctionContext(
    id = AuctionContextId(11L),
    auctionKey = testAuctionKey
  )

  private val auctionBids = List(
    auction.UserBid(userId = UserId("user1"), bid = Bid(Kopecks(100))),
    auction.UserBid(userId = UserId("user2"), bid = Bid(Kopecks(200))),
    auction.UserBid(userId = UserId("user3"), bid = Bid(Kopecks(300)))
  )

  private val testAuctionState = AuctionState(
    auctionKey = testAuctionKey,
    bids = auctionBids,
    stepCost = Kopecks(100),
    minBid = Bid(Kopecks(1700))
  )

  private val testAutoStrategy = AutoStrategy(
    id = 11,
    userId = UserId("user4"),
    auctionContextId = AuctionContextId(999L),
    settings = null
  )

}
