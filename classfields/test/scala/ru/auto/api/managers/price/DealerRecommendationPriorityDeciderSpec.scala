package ru.auto.api.managers.price

import ru.auto.api.BaseSpec
import ru.auto.api.model.AutoruProduct
import ru.auto.api.model.AutoruProduct._

class DealerRecommendationPriorityDeciderSpec extends BaseSpec {
  "developer" should {
    "not forget add recommendation_priority for new product if needed" in {

      val otherProducts = Set(
        Placement,
        Highlighting,
        TopList,
        PackageExpress,
        CertificationPlanned,
        CertificationMobile,
        SaleAdd,
        PackageCart,
        StoTop,
        PackageVip,
        OffersHistoryReports,
        VinHistory,
        ConciergePrepay,
        Call,
        TradeInRequestCarsNew,
        TradeInRequestCarsUsed,
        MatchApplicationCarsNew,
        Booking,
        ShowInStories
      )
      AutoruProduct.values
        .groupBy(product => DealerRecommendationPriorityDecider.getRecommendationPriority(product))
        .view
        .filterKeys(_ == 0)
        .values
        .flatten
        .toSet shouldBe otherProducts
    }
  }
}
