package vsmoney.auction_auto_strategy.scheduler.test.service

import common.models.finance.Money.Kopecks
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
import vsmoney.auction_auto_strategy.scheduler.service.AutoStrategyCalculatorService
import vsmoney.auction_auto_strategy.scheduler.service.impl.AutoStrategyCalculatorServiceLive
import zio.ZIO
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

object AutoStrategyCalculatorServiceLiveSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {

    suite("AutoStrategyCalculatorServiceLive")(
      testM("should make bid = min bid  if only one member and maxBid > price + stepCost") {
        val stepCost = Kopecks(300)
        val minBid = Bid(Kopecks(1700))

        val autoStrategy = makeAutoStrategy("user1", 5000)
        val autoStrategies = List(autoStrategy)

        val actions = List(
          makeNewBid("user1", 1700, mockAuctionContext, autoStrategy.id.toString)
        )

        val auctionState =
          AuctionState(
            auctionKey = testAuctionKey,
            bids = Nil,
            stepCost = stepCost,
            minBid = minBid
          )

        runTest(
          auctionState = auctionState,
          autoStrategies = autoStrategies,
          resultActions = actions
        )
      },
      testM("should not make bid if minBid in auction more maxBid in auto strategy") {
        val stepCost = Kopecks(100)
        val minBid = Bid(Kopecks(1700))
        val autoStrategies = List(
          makeAutoStrategy("user-6", 1600)
        )
        val auctionState =
          auction.AuctionState(
            auctionKey = testAuctionKey,
            bids = Nil,
            stepCost = stepCost,
            minBid = minBid
          )

        runTest(
          auctionState = auctionState,
          autoStrategies = autoStrategies,
          resultActions = List(AuctionAction.NoAction)
        )
      },
      testM("should not generate actions if there are no auto strategies") {
        val stepCost = Kopecks(100)
        val minBid = Bid(Kopecks(1700))
        val userBids = List(
          makeUserBid("user1", 44400),
          makeUserBid("user1", 42400),
          makeUserBid("user1", 41400)
        )
        val auctionState =
          auction.AuctionState(
            auctionKey = testAuctionKey,
            bids = userBids,
            stepCost = stepCost,
            minBid = minBid
          )
        runTest(
          auctionState = auctionState,
          autoStrategies = Nil,
          resultActions = Nil
        )
      },
      testM("should generate change bid if max bid in setting more max bid in auction") {
        val stepCost = Kopecks(100)
        val minBid = Bid(Kopecks(1700))
        val userBids = List(
          makeUserBid("user1", 5000),
          makeUserBid("user4", 3000)
        )
        val autoStrategy = makeAutoStrategy("user4", 9000)
        val autoStrategies = List(autoStrategy)
        val auctionState =
          auction.AuctionState(
            auctionKey = testAuctionKey,
            bids = userBids,
            stepCost = stepCost,
            minBid = minBid
          )
        runTest(
          auctionState = auctionState,
          autoStrategies = autoStrategies,
          resultActions = List(
            makeChangeBid("user4", 3000, 5100, mockAuctionContext, autoStrategy.id.toString)
          )
        )
      },
      testM("should set equal position if equal maxBid ") {
        val stepCost = Kopecks(100)
        val minBid = Bid(Kopecks(1700))
        val userBids = List(
          makeUserBid("user1", 5000),
          makeUserBid("user4", 3000)
        )
        val autoStrategy = makeAutoStrategy("user4", 9000)
        val autoStrategy2 = makeAutoStrategy("user5", 9000)
        val autoStrategies = List(autoStrategy, autoStrategy2)
        val auctionState =
          auction.AuctionState(
            auctionKey = testAuctionKey,
            bids = userBids,
            stepCost = stepCost,
            minBid = minBid
          )
        runTest(
          auctionState = auctionState,
          autoStrategies = autoStrategies,
          resultActions = List(
            makeChangeBid("user4", 3000, 5100, mockAuctionContext, autoStrategy.id.toString),
            makeNewBid("user5", 5100, mockAuctionContext, autoStrategy2.id.toString)
          )
        )
      },
      testM("should generate maximum position within settings") {
        val stepCost = Kopecks(100)
        val minBid = Bid(Kopecks(1700))
        val userBids = List(
          makeUserBid("user1", 5000),
          makeUserBid("user4", 3000)
        )

        val autoStrategyUser3 = makeAutoStrategy("user3", 7000)
        val autoStrategyUser4 = makeAutoStrategy("user4", 9000)
        val autoStrategies = List(
          autoStrategyUser3,
          autoStrategyUser4
        )
        val auctionState =
          auction.AuctionState(
            auctionKey = testAuctionKey,
            bids = userBids,
            stepCost = stepCost,
            minBid = minBid
          )
        runTest(
          auctionState = auctionState,
          autoStrategies = autoStrategies,
          resultActions = List(
            makeNewBid("user3", 5100, mockAuctionContext, autoStrategyUser3.id.toString),
            makeChangeBid("user4", 3000, 7100, mockAuctionContext, autoStrategyUser4.id.toString)
          )
        )
      },
      testM("should not generate action if max bid < minBid + stepCost and haven't old bid") {
        val stepCost = Kopecks(100)
        val minBid = Bid(Kopecks(1700))
        val auctionBid = List()
        val autoStrategies = List(
          makeAutoStrategy("user-6", 50)
        )
        val auctionState =
          auction.AuctionState(
            auctionKey = testAuctionKey,
            bids = auctionBid,
            stepCost = stepCost,
            minBid = minBid
          )
        runTest(
          auctionState = auctionState,
          autoStrategies = autoStrategies,
          resultActions = List(AuctionAction.NoAction)
        )
      },
      testM("should leve from auction if max bid < minBid + stepCost") {
        val stepCost = Kopecks(100)
        val minBid = Bid(Kopecks(1700))
        val auctionBid = List(makeUserBid("user-6", 30))
        val autoStrategy = makeAutoStrategy("user-6", 50)
        val autoStrategies = List(autoStrategy)

        val actions = List(
          AuctionAction.StopAuction(
            userId = UserId("user-6"),
            oldBid = Bid(Kopecks(30)),
            context = mockAuctionContext,
            autoStrategyId = autoStrategy.id.toString
          )
        )
        val auctionState =
          auction.AuctionState(
            auctionKey = testAuctionKey,
            bids = auctionBid,
            stepCost = stepCost,
            minBid = minBid
          )
        runTest(
          auctionState = auctionState,
          autoStrategies = autoStrategies,
          resultActions = actions
        )
      },
      testM("should set equal bid if auto strategies have equals maxbid") {
        val stepCost = Kopecks(100)
        val minBid = Bid(Kopecks(1700))
        val autoStrategies = List(
          makeAutoStrategy("user-6", 3050),
          makeAutoStrategy("user-7", 3050)
        )

        val auctionState =
          auction.AuctionState(
            auctionKey = testAuctionKey,
            bids = Nil,
            stepCost = stepCost,
            minBid = minBid
          )
        runTest(
          auctionState = auctionState,
          autoStrategies = autoStrategies,
          resultActions = List(
            makeNewBid("user-6", 1700, mockAuctionContext, autoStrategies.head.id.toString),
            makeNewBid("user-7", 1700, mockAuctionContext, autoStrategies.last.id.toString)
          )
        )
      }
    )
  }

  private def makeNewBid(
      userId: String,
      bid: Long,
      context: AuctionContext,
      autoStrategyId: String): AuctionAction.ChangeBid = {
    AuctionAction.ChangeBid(
      userId = UserId(userId),
      oldBid = None,
      newBid = Bid(Kopecks(bid)),
      context = context,
      autoStrategyId = autoStrategyId
    )
  }

  private def makeChangeBid(
      userId: String,
      oldBid: Long,
      newBid: Long,
      context: AuctionContext,
      autoStrategyId: String): AuctionAction.ChangeBid = {
    AuctionAction.ChangeBid(
      userId = UserId(userId),
      oldBid = Some(Bid(Kopecks(oldBid))),
      newBid = Bid(Kopecks(newBid)),
      context = context,
      autoStrategyId = autoStrategyId
    )
  }

  private def makeAutoStrategy(userId: String, maxBidKopecks: Long): AutoStrategy = {
    val maxBid = Bid(Kopecks(maxBidKopecks))
    AutoStrategy(
      id = 1L,
      userId = UserId(userId),
      auctionContextId = mockAuctionContext.id,
      settings = AutoStrategySettings.MaximumPositionForBid(maxBid)
    )
  }

  private def makeUserBid(userId: String, bid: Long): UserBid = {
    auction.UserBid(userId = UserId(userId), bid = Bid(Kopecks(bid)))
  }

  private def runTest(
      auctionState: AuctionState,
      autoStrategies: List[AutoStrategy],
      resultActions: List[AuctionAction]): ZIO[zio.ZEnv, Nothing, zio.test.TestResult] = {

    val test = ZIO
      .service[AutoStrategyCalculatorService]
      .flatMap(
        _.calculate(
          context = mockAuctionContext,
          auctionState = auctionState,
          autoStrategies = autoStrategies
        )
      )
    assertM(test.run)(succeeds(equalTo(resultActions)))
      .provideCustomLayer(AutoStrategyCalculatorServiceLive.live)
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

  private val mockAuctionContext: AuctionContext = AuctionContext(
    id = AuctionContextId(11L),
    auctionKey = testAuctionKey
  )

}
