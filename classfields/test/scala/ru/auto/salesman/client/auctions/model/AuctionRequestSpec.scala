package ru.auto.salesman.client.auctions.model

import com.google.protobuf.timestamp.Timestamp
import org.joda.time.DateTime
import ru.auto.salesman.model.criteria.Criterion
import ru.auto.salesman.model.{AutoruDealer, ProductId}
import ru.auto.salesman.test.BaseSpec
import vsmoney.auction.auction_bids.BidByDateTimeRequest
import vsmoney.auction.common_model.{CriteriaValue, Project}
import vsmoney.auction.{common_model => proto}
import ru.auto.salesman.environment.RichDateTime
import ru.auto.salesman.model.criteria.CriteriaContext.CallCarsNewCriteriaContext

class AuctionRequestSpec extends BaseSpec {
  "convert toProto with right context ordering" in {

    val ctx = CallCarsNewCriteriaContext(
      List(
        Criterion(Criterion.modelKey, "X5"),
        Criterion(Criterion.regionKey, "1"),
        Criterion(Criterion.markKey, "BMW")
      )
    )
    val product = ProductId.Call
    val user = AutoruDealer(1)
    val request = AuctionRequest(user, product, ctx)
    val date = DateTime.now()

    val expectedContext = proto.AuctionContext(
      proto.AuctionContext.Context.CriteriaContext(
        proto.AuctionContext.CriteriaContext(
          List(
            CriteriaValue("region_id", "1"),
            CriteriaValue("mark", "BMW"),
            CriteriaValue("model", "X5")
          )
        )
      )
    )
    val expected = BidByDateTimeRequest(
      project = Project.AUTORU,
      product = ProductId.Call.toString,
      userId = user.toString,
      context = Some(expectedContext),
      datetime = Some(Timestamp.fromJavaProto(date.asTimestamp))
    )

    val result = AuctionRequest.toProto[ProductId.Call.type](request, date)

    result shouldBe expected
  }
}
