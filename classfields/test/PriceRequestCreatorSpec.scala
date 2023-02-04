package vsmoney.auction.clients.test

import common.models.finance.Money.Kopecks
import vsmoney.auction.clients.PriceRequestCreator
import vsmoney.auction.model.howmuch.{
  ChangePriceBatchRequest,
  ChangePriceRequest,
  ChangePriceRequestEntity,
  PriceRequest,
  PriceRequestEntry
}
import vsmoney.auction.model.request.{LeaveAuctionEntityRequest, PlaceBidBEntityRequest}
import vsmoney.auction.model.{
  AuctionChangeSource,
  AuctionKey,
  Bid,
  CriteriaContext,
  Criterion,
  CriterionKey,
  CriterionValue,
  MatrixId,
  ProductId,
  Project,
  UserAuction,
  UserId
}
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

import java.time.Instant

object PriceRequestCreatorSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("PriceRequestCreator")(
      test("getAuctionBid should create request with auction product and user in context") {
        val date = Instant.now()

        val expectedContext = CriteriaContext(
          List(
            Criterion(CriterionKey(CriterionKey.userId), CriterionValue(user1)),
            Criterion(CriterionKey(criterion1key), CriterionValue(criterion1value))
          )
        )
        val expectedRequest = PriceRequest(
          project,
          date,
          List(
            PriceRequestEntry(
              PriceRequestCreator.defaultEntryId,
              MatrixId.AuctionMatrixId(product),
              expectedContext
            )
          )
        )

        val res = PriceRequestCreator.forAuctionBid(testAuction, date, PriceRequestCreator.defaultEntryId)
        assert(res)(equalTo(expectedRequest))
      },
      test("getBasePrice should create request for product") {
        val date = Instant.now()

        val expectedRequest = PriceRequest(
          project,
          date,
          List(
            PriceRequestEntry(
              PriceRequestCreator.defaultEntryId,
              MatrixId.ProductMatrixId(product),
              testContext
            )
          )
        )

        val res = PriceRequestCreator.forBasePrice(testAuction.key, date, PriceRequestCreator.defaultEntryId)
        assert(res)(equalTo(expectedRequest))
      },
      test("changeAuctionBid should create request with auction product and user in context") {
        val prevPrice = Kopecks(1000)
        val newPrice = Kopecks(1000)

        val expectedContext = CriteriaContext(
          List(
            Criterion(CriterionKey(CriterionKey.userId), CriterionValue(user1)),
            Criterion(CriterionKey(criterion1key), CriterionValue(criterion1value))
          )
        )
        val expectedRequest = ChangePriceRequest(
          project,
          matrix = MatrixId.AuctionMatrixId(product),
          expectedContext,
          Some(prevPrice),
          Some(newPrice),
          Some(testSource),
          auctionObject = None
        )

        val res =
          PriceRequestCreator.forChangeAuctionBid(testAuction, Some(Bid(prevPrice)), Bid(newPrice), Some(testSource))
        assert(res)(equalTo(expectedRequest))
      },
      test("should create ChangePriceBatchRequest from StopUserAuctionBatch") {
        val testBid = Bid(Kopecks(324))
        val leaveEntityRequest = List(
          LeaveAuctionEntityRequest(
            userAuction = testAuction,
            prevBid = testBid
          )
        )
        val expectedRequest = ChangePriceBatchRequest(
          project,
          List[ChangePriceRequestEntity](
            ChangePriceRequestEntity(
              MatrixId.AuctionMatrixId(product),
              testAuction.withUserAndObjectContext.key.context,
              Some(testBid.amount),
              None,
              testAuction.key.auctionObject
            )
          ),
          Some(testSource)
        )
        val res = PriceRequestCreator.forStopUserAuctionBatch(project, leaveEntityRequest, Some(testSource))
        assert(res)(equalTo(expectedRequest))
      },
      test("should create StopUserAuctionBatch from PlaceBidBEntityRequest") {
        val testPrevBid = Bid(Kopecks(533))
        val testBid = Bid(Kopecks(324))
        val placeEntityRequest = List(
          PlaceBidBEntityRequest(
            userAuction = testAuction,
            prevBid = Some(testPrevBid),
            bid = testBid
          )
        )
        val expectedRequest = ChangePriceBatchRequest(
          project,
          List[ChangePriceRequestEntity](
            ChangePriceRequestEntity(
              MatrixId.AuctionMatrixId(product),
              testAuction.withUserAndObjectContext.key.context,
              Some(testPrevBid.amount),
              Some(testBid.toKopecks),
              testAuction.key.auctionObject
            )
          ),
          Some(testSource)
        )
        val res = PriceRequestCreator.forChangeAuctionBidBatch(project, placeEntityRequest, Some(testSource))
        assert(res)(equalTo(expectedRequest))
      }
    )
  }

  private val project = Project.Autoru
  private val product = ProductId("call")
  private val user1 = "user:1"
  private val criterion1key = "key"
  private val criterion1value = "value"

  private val testContext = CriteriaContext(
    List(Criterion(CriterionKey(criterion1key), CriterionValue(criterion1value)))
  )

  private val testAuction = UserAuction(
    AuctionKey(
      project,
      product,
      testContext,
      auctionObject = None
    ),
    UserId(user1)
  )

  private val testSource = AuctionChangeSource.UserAction(UserId(user1))
}
