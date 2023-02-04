package vsmoney.auction.converters.test

import auto.indexing.auctions.{
  AuctionBidConfiguration,
  AuctionBidContainer,
  AuctionBidContext,
  AuctionBidIndex,
  AuctionBidProductIndex,
  CriteriaValue
}
import common.models.finance.Money.Kopecks
import vsmoney.auction.model.{
  BasePrice,
  Bid,
  CriteriaContext,
  Criterion,
  CriterionKey,
  CriterionValue,
  ProductId,
  UserId
}
import vsmoney.auction.model.`export`.{AuctionInfo, ProductAuctions, UserAuctions}
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

object SearcherProtoConvertersSpec extends DefaultRunnableSpec {
  import vsmoney.auction.converters.SearcherProtoConverters._

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("SearcherProtoConverters")(
      testM("auctions2Proto should return empty on empty input") {
        val auctions: Seq[ProductAuctions] = List.empty
        val proto: AuctionBidProductIndex = AuctionBidProductIndex(Map.empty)
        assertM(auctionsToProtoConverter.convert(auctions))(equalTo(proto))
      },
      testM("auctions2Proto should convert data properly") {
        assertM(auctionsToProtoConverter.convert(testAuctions))(equalTo(testSearcherAuctions))
      },
      testM("should convert criterion internal criteria model to searcher CriteriaValue") {
        val request = Criterion(key = CriterionKey("key1"), value = CriterionValue("v"))
        val response = CriteriaValue("key1", "v")
        assertM(criterionToCriteriaValue.convert(request))(equalTo(response))
      },
      testM("should convert CriteriaContext to AuctionBidContext") {
        val request = CriteriaContext(Seq(Criterion(key = CriterionKey("key1"), value = CriterionValue("v"))))
        val response = AuctionBidContext(Seq(CriteriaValue("key1", "v")))
        assertM(contextToProtoConverter.convert(request))(equalTo(response))
      },
      testM("should convert auctionInfo to ProtoConverter") {
        val bid = Bid(Kopecks(1345))
        val basePrice = BasePrice(Kopecks(6543))
        val request = AuctionInfo(
          context = CriteriaContext(Seq(Criterion(key = CriterionKey("key1"), value = CriterionValue("v")))),
          auctionObject = Some(Criterion(key = CriterionKey("objectKey"), value = CriterionValue("objectValue"))),
          bid = bid,
          basePrice = BasePrice(Kopecks(6543))
        )
        val response = AuctionBidConfiguration(
          context = Some(
            AuctionBidContext(
              criteriaValues = Seq(
                CriteriaValue("key1", "v"),
                CriteriaValue("objectKey", "objectValue")
              )
            )
          ),
          bid = bid.amount.value,
          basePrice = basePrice.amount.value
        )
        assertM(auctionInfoToProtoConverter.convert(request))(equalTo(response))
      }
    )
  }

  private val product = "call"
  private val user1 = "user:1"
  private val user2 = "user:2"
  private val bids1 = List(10L, 20L, 30L)
  private val bids2 = bids1.map(_ + 1L)
  private val basePrice1 = List(1L, 2L, 3L)
  private val basePrice2 = basePrice1.map(_ + 1L)
  private val criterion1key = "key"
  private val criterion1value = "value"

  private def createUserAuction(user: String, bids: Seq[Long], prices: Seq[Long]) =
    UserAuctions(
      UserId(user),
      bids.zip(prices).map { case (bid, price) =>
        AuctionInfo(
          CriteriaContext(List(Criterion(CriterionKey(criterion1key), CriterionValue(criterion1value)))),
          auctionObject = None,
          Bid(Kopecks(bid)),
          BasePrice(Kopecks(price))
        )
      },
      active = true
    )

  private val testAuctions = List(
    ProductAuctions(
      ProductId(product),
      List(
        createUserAuction(user1, bids1, basePrice1),
        createUserAuction(user2, bids2, basePrice2)
      )
    )
  )

  private def createSearcherAuctionBidContainer(bids: Seq[Long], prices: Seq[Long]) = {
    val confs = bids.zip(prices).map { case (bid, price) =>
      AuctionBidConfiguration(Some(AuctionBidContext(List(CriteriaValue(criterion1key, criterion1value)))), bid, price)
    }
    AuctionBidContainer(confs, true)
  }

  private val testSearcherAuctions = AuctionBidProductIndex(
    Map(
      product -> AuctionBidIndex(
        Map(
          user1 -> createSearcherAuctionBidContainer(bids1, basePrice1),
          user2 -> createSearcherAuctionBidContainer(bids2, basePrice2)
        )
      )
    )
  )

}
