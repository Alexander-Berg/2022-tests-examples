package vsmoney.auction.scheduler.test.converters

import common.models.finance.Money.Kopecks
import vsmoney.auction.model.`export`.{AuctionInfo, ProductAuctions, UserAuctions}
import vsmoney.auction.model.{
  AuctionKey,
  BasePrice,
  Bid,
  CriteriaContext,
  Criterion,
  CriterionKey,
  CriterionValue,
  ProductId,
  Project,
  UserAuction,
  UserId
}
import vsmoney.auction.model.howmuch.{PriceResponse, PriceResponseEntry}
import vsmoney.auction.scheduler.convertes.ProductAuctionsBuilder
import vsmoney.auction.scheduler.model.{UserAuctionWithBid, UserAuctionWithBidWithBasePrice}
import zio.test.{DefaultRunnableSpec, ZSpec}
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

object ProductAuctionBuilderSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("ProductAuctionBuilder")(
      test("should return ProductAuctions") {
        val productId = ProductId("call")

        val request: Seq[UserAuctionWithBidWithBasePrice] = Seq(
          UserAuctionWithBidWithBasePrice(
            userAuction = UserAuction(
              key = AuctionKey(
                project = Project.Autoru,
                product = productId,
                context = CriteriaContext(
                  criteria = Seq(
                    Criterion(CriterionKey("mark"), CriterionValue("bmw")),
                    Criterion(CriterionKey("model"), CriterionValue("x5"))
                  )
                ),
                auctionObject = None
              ),
              user = UserId("45")
            ),
            bid = Bid(Kopecks(4444)),
            basePrice = BasePrice(Kopecks(100))
          )
        )

        val expectedProductAuctions = ProductAuctions(
          product = productId,
          auctions = Seq(
            UserAuctions(
              userId = UserId("45"),
              auctions = Seq(
                AuctionInfo(
                  context = CriteriaContext(
                    criteria = Seq(
                      Criterion(CriterionKey("user_id"), CriterionValue("45")),
                      Criterion(CriterionKey("mark"), CriterionValue("bmw")),
                      Criterion(CriterionKey("model"), CriterionValue("x5"))
                    )
                  ),
                  auctionObject = None,
                  bid = Bid(Kopecks(4444)),
                  basePrice = BasePrice(Kopecks(100))
                )
              ),
              active = true
            )
          )
        )
        val result = ProductAuctionsBuilder.build(
          userAuctionWithPriceWithBasePrice = request,
          productId = productId,
          blockedUsers = Set.empty
        )

        assert(result)(equalTo(expectedProductAuctions))
      }
    )
  }
}
