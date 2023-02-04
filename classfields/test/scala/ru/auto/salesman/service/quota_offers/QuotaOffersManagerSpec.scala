package ru.auto.salesman.service.quota_offers

import ru.auto.salesman.model.OfferStatuses
import ru.auto.salesman.service.quota_offers.QuotaOffersManager.QuotedOffers
import ru.auto.salesman.test.BaseSpec

class QuotaOffersManagerSpec extends BaseSpec {
  import ru.auto.salesman.service.quota_offers.QuotaOffersTestData._

  "QuotedOffers split offers by quota" should {
    "split by offer status and quota size" in {
      val offersInQuota =
        genOffers(testQuotaSize).map(_.copy(status = OfferStatuses.Show))
      val offersBehindQuota = genOffers(testQuotaSize).map(
        _.copy(status = OfferStatuses.WaitingActivation)
      )
      val offers = offersBehindQuota ++ offersInQuota
      val quotedOffers =
        QuotedOffers(offers, Some(testQuota.toQuota), testClient)
      quotedOffers.offersInQuota should contain theSameElementsAs offersInQuota
      quotedOffers.offersBehindQuota should contain theSameElementsAs offersBehindQuota
    }

    "all in quota if quota is bigger then offers size" in {
      val offers = genOffers(testQuotaSize)
      val quotedOffers =
        QuotedOffers(offers, Some(testQuota.toQuota), testClient)
      quotedOffers.offersInQuota should contain theSameElementsAs offers
      quotedOffers.offersBehindQuota.size shouldBe 0
    }

    "all behind quota if quota is 0" in {
      val offers = genOffers(1)
      val quotedOffers =
        QuotedOffers(offers, quota = None, testClient)
      quotedOffers.offersInQuota.size shouldBe 0
      quotedOffers.offersBehindQuota should contain theSameElementsAs offers
    }
  }

}
