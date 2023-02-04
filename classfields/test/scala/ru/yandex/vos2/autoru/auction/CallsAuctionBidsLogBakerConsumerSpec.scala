package ru.yandex.vos2.autoru.auction

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.Suite
import org.scalatest.prop.TableDrivenPropertyChecks
import vsmoney.auction.AuctionBidsDelivery.AuctionBid

class CallsAuctionBidsLogBakerConsumerSpec extends AnyWordSpec with Matchers with Suite with TableDrivenPropertyChecks {

  private val BidObjectExamples = Table(
    ("input", "expected"),
    ("offer_id=1106386737-955447c1", Some("1106386737-955447c1")),
    ("foo=bar&offer_id=123&baz=qux", Some("123")),
    ("foo=123", None)
  )

  "getOfferIdFromBid" should {
    "work as expected for known examples" in {
      forAll(BidObjectExamples) { (input, expected) =>
        val bid = AuctionBid.newBuilder().setObject(input).build()
        val result = CallsAuctionBidsLogBakerConsumer.getOfferIdFromBid(bid)
        result shouldEqual expected
      }
    }
  }
}
