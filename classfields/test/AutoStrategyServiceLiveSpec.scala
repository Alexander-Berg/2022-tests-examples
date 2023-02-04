package vsmoney.auction_auto_strategy.services.test

import common.models.finance.Money.Kopecks
import vsmoney.auction_auto_strategy.model.AutoStrategySettings.MaximumPositionForBid
import vsmoney.auction_auto_strategy.model._
import vsmoney.auction_auto_strategy.model.auction.{
  AuctionContext,
  AuctionContextId,
  AuctionKey,
  AuctionState,
  Bid,
  CriteriaContext,
  Criterion,
  CriterionKey,
  CriterionValue
}
import vsmoney.auction_auto_strategy.model.common.{ProductId, Project, UserId}
import vsmoney.auction_auto_strategy.services.AutoStrategyService
import vsmoney.auction_auto_strategy.services.exception.AutoStrategyException
import vsmoney.auction_auto_strategy.services.impl.AutoStrategyServiceLive
import vsmoney.auction_auto_strategy.services.testkit.{
  AuctionContextServiceMock,
  AuctionServiceMock,
  AutoStrategyDAOMock
}
import zio.ZIO
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation._
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

object AutoStrategyServiceLiveSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("AutoStrategyServiceLive")(
      suite("put")(
        testM(
          "should  get or create context by auction key and call it in autoStrategyDAO"
        ) {
          val maxBid = Bid(Kopecks(200))
          val auctionServiceMock = AuctionServiceMock.GetState(equalTo(testAuctionKey), value(testAuctionState))
          val autoStrategySettings = MaximumPositionForBid(maxBid)
          val contextServiceMock = AuctionContextServiceMock.GetOrCreate(equalTo(testAuctionKey), value(testContext))
          val autoStrategyDAOMock = AutoStrategyDAOMock.Put(
            equalTo(
              (testUser, testContext.id, autoStrategySettings, maxBid)
            ),
            unit
          )

          val result = ZIO
            .service[AutoStrategyService]
            .flatMap(
              _.put(
                userId = testUser,
                auctionKey = testAuctionKey,
                settings = MaximumPositionForBid(maxBid),
                maxBid = maxBid
              )
            )
          assertM(result.run)(succeeds(isUnit))
            .provideCustomLayer(
              contextServiceMock ++ auctionServiceMock ++ autoStrategyDAOMock >>> AutoStrategyServiceLive.live
            )
        },
        testM("should generate exception if maxBid did not equal stepCost") {
          val maxBid = Bid(Kopecks(195))
          val auctionServiceMock = AuctionServiceMock.GetState(equalTo(testAuctionKey), value(testAuctionState))
          val contextServiceMock = AuctionContextServiceMock.GetOrCreate(equalTo(testAuctionKey), value(testContext))

          val result = ZIO
            .service[AutoStrategyService]
            .flatMap(
              _.put(
                userId = testUser,
                auctionKey = testAuctionKey,
                settings = MaximumPositionForBid(maxBid),
                maxBid = maxBid
              )
            )
          assertM(result.run)(fails(isSubtype[AutoStrategyException.MaxBidNotMultipleCost](anything)))
            .provideCustomLayer(
              AutoStrategyDAOMock.empty ++ contextServiceMock ++ auctionServiceMock >>> AutoStrategyServiceLive.live
            )
        }
      ),
      suite("remove")(
        testM("should get context by auction key and remove user settings from dao") {
          val getContextMock = AuctionContextServiceMock.Get(equalTo(testAuctionKey), value(Some(testContext)))

          val removeAutoStrategyFromDAOMock = AutoStrategyDAOMock.Delete(equalTo((testUser, testContext.id)), unit)
          val result = ZIO
            .service[AutoStrategyService]
            .flatMap(
              _.remove(
                userId = testUser,
                auctionKey = testAuctionKey
              )
            )
          assertM(result.run)(succeeds(isUnit))
            .provideCustomLayer(
              AuctionServiceMock.empty ++ getContextMock ++ removeAutoStrategyFromDAOMock >>> AutoStrategyServiceLive.live
            )
        },
        testM("should get context and not remove from dao if context not found") {
          val getContextMock = AuctionContextServiceMock.Get(equalTo(testAuctionKey), value(None))
          val deleteAutoStrategyFromDaoMock = AutoStrategyDAOMock.empty

          val removeTest = ZIO
            .service[AutoStrategyService]
            .flatMap(
              _.remove(
                userId = testUser,
                auctionKey = testAuctionKey
              )
            )
          assertM(removeTest.run)(succeeds(isUnit))
            .provideCustomLayer(
              deleteAutoStrategyFromDaoMock ++ AuctionServiceMock.empty ++ getContextMock >>> AutoStrategyServiceLive.live
            )

        }
      ),
      suite("allForUser")(
        testM("should return all contexts for user") {
          val autoStrategyDaoMockCall = AutoStrategyDAOMock.AllByUser(equalTo(testUser), value(List(testAutoStrategy)))
          val testAllForUser = ZIO
            .service[AutoStrategyService]
            .flatMap(
              _.allForUser(
                userId = testUser
              )
            )
          assertM(testAllForUser.run)(succeeds(equalTo(List(testAutoStrategy))))
            .provideCustomLayer(
              AuctionContextServiceMock.empty ++ AuctionServiceMock.empty ++ autoStrategyDaoMockCall >>> AutoStrategyServiceLive.live
            )
        },
        testM("should forwarding error from AutoStrategyDAO") {
          val autoStrategyDaoMockCall = AutoStrategyDAOMock.AllByUser(equalTo(testUser), failure(new Exception("test")))
          val testAllForUser = ZIO
            .service[AutoStrategyService]
            .flatMap(
              _.allForUser(
                userId = testUser
              )
            )
          assertM(testAllForUser.run)(fails(isSubtype[Exception](anything)))
            .provideCustomLayer(
              AuctionContextServiceMock.empty ++ AuctionServiceMock.empty ++ autoStrategyDaoMockCall >>> AutoStrategyServiceLive.live
            )
        }
      ),
      suite("allForContext")(
        testM("should return all autoStrategies for auctionkey") {

          val contextServiceMock = AuctionContextServiceMock.Get(equalTo(testAuctionKey), value(Some(testContext)))
          val autoStrategyDAOMock =
            AutoStrategyDAOMock.AllByContext(equalTo(testContext.id), value(List(testAutoStrategy)))
          val testAllForAuctionKey = ZIO
            .service[AutoStrategyService]
            .flatMap(
              _.allForContext(
                auctionKey = testAuctionKey
              )
            )

          assertM(testAllForAuctionKey.run)(succeeds(equalTo(List(testAutoStrategy))))
            .provideCustomLayer(
              AuctionServiceMock.empty ++ contextServiceMock ++ autoStrategyDAOMock >>> AutoStrategyServiceLive.live
            )
        },
        testM("should return fail if context not found by auction key") {
          val contextServiceMock = AuctionContextServiceMock.Get(equalTo(testAuctionKey), value(None))

          val testAllForAuctionKey = ZIO
            .service[AutoStrategyService]
            .flatMap(
              _.allForContext(
                auctionKey = testAuctionKey
              )
            )
          assertM(testAllForAuctionKey.run)(fails(isSubtype[AutoStrategyException.ContextNotFound](anything)))
            .provideCustomLayer(
              AutoStrategyDAOMock.empty ++ AuctionServiceMock.empty ++ contextServiceMock >>> AutoStrategyServiceLive.live
            )
        }
      )
    )
  }

  private val testUser = UserId("user:123")

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

  private val testContext = AuctionContext(
    id = AuctionContextId(1999),
    auctionKey = testAuctionKey
  )

  private val testAutoStrategy = AutoStrategy(
    id = 1,
    userId = testUser,
    auctionContextId = testContext.id,
    settings = MaximumPositionForBid(Bid(Kopecks(100)))
  )

  private val testAuctionState = AuctionState(
    auctionKey = testAuctionKey,
    bids = Nil,
    stepCost = Kopecks(100),
    minBid = Bid(Kopecks(1700))
  )
}
