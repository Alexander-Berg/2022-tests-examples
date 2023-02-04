package vsmoney.auction.converters.test

import common.models.finance.Money.Kopecks
import common.scalapb.ScalaProtobuf
import vsmoney.auction.auction_bids_delivery.AuctionBid
import vsmoney.auction.model.{
  AuctionChangeSource,
  AuctionKey,
  Bid,
  CriteriaContext,
  Criterion,
  CriterionKey,
  CriterionValue,
  ProductId,
  Project,
  UserAuction,
  UserAuctionBidEvent,
  UserId
}
import vsmoney.auction.common_model.{ChangeSource, Project => ProtoProject}
import vsmoney.auction.converters.BidEventProtoConverter
import zio.test.Assertion.equalTo
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

import java.time.OffsetDateTime

object BidEventProtoConverterSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("BidEventProtoConverter")(
      testM("should convert UserAuctionBidEvent to AuctionBid with user_id in context and bid") {
        val testValue = UserAuctionBidEvent(testAuction, Some(Bid(testBid)), testTime, Some(testSource))

        assertM(BidEventProtoConverter.auctionBidToBidEvent.convert(testValue))(equalTo(testEvent))
      },
      testM("should convert UserAuctionBidEvent to AuctionBid with user_id in context and bid = 0") {
        val testValue = UserAuctionBidEvent(testAuction, None, testTime, Some(testSource))
        val expectedEvent = testEvent.copy(bid = 0)

        assertM(BidEventProtoConverter.auctionBidToBidEvent.convert(testValue))(equalTo(expectedEvent))
      }
    )
  }
  private val product = ProductId("call")
  private val project = Project.Autoru
  private val user = UserId("user:1")
  private val testBid = Kopecks(2000)
  private val testTime = OffsetDateTime.parse("2021-07-09T19:34+03:00").toInstant

  private val testContext = CriteriaContext(
    List(Criterion(CriterionKey("key"), CriterionValue("value")))
  )

  private val testAuction = UserAuction(
    AuctionKey(
      project,
      product,
      testContext,
      auctionObject = Some(Criterion(CriterionKey("offer_id"), CriterionValue("232-4dd")))
    ),
    user
  )

  private val testSource = AuctionChangeSource.UserAction(user)

  private val testEvent = AuctionBid(
    Some(ScalaProtobuf.instantToTimestamp(testTime)),
    ProtoProject.AUTORU,
    product.id,
    user.id,
    "user_id=user:1&key=value",
    testBid.value,
    Some(ChangeSource(ChangeSource.Source.User(ChangeSource.User(user.id)))),
    "offer_id=232-4dd"
  )
}
