package ru.yandex.vertis.moisha.impl.autoru.v2

import ru.yandex.vertis.moisha.impl.autoru.SingleProductDailyPolicySpec
import ru.yandex.vertis.moisha.impl.autoru.model._
import ru.yandex.vertis.moisha.model.FundsConversions._
import ru.yandex.vertis.moisha.model._

/**
  * Specs on AutoRu policy for [[Products.Certification]]
  * Rates are described in project https://planner.yandex-team.ru/projects/31761/
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
trait AutoRuCertificationSpec extends SingleProductDailyPolicySpec {

  import AutoRuCertificationSpec._

  "AutoRuPolicyV2 policy" should {
    "support product Certification" in {
      policy.products.contains(Products.Certification.toString) should be(true)
    }

    "support product MobileCertification" in {
      policy.products.contains(Products.MobileCertification.toString) should be(true)
    }

    "return correct certification price for Moscow" in {
      checkPolicy(correctInterval, certification(2990.rubles), priceIn(0L, Long.MaxValue), inMoscow)
    }

    "return correct mobile-certification price for Moscow" in {
      checkPolicy(correctInterval, mobileCertification(3490.rubles), priceIn(0L, Long.MaxValue), inMoscow)
    }

    "return correct certification price for Spb" in {
      checkPolicy(correctInterval, certification(2990.rubles), priceIn(0L, Long.MaxValue), inSPb)
    }

    "return correct mobile-certification price for Spb" in {
      checkPolicy(correctInterval, mobileCertification(3490.rubles), priceIn(0L, Long.MaxValue), inSPb)
    }

    "fail on certification request" in {
      checkPolicyFailure(
        correctInterval,
        Products.Certification,
        priceIn(0L, Long.MaxValue),
        inRegion(0)
      )
    }

    "fail on mobile-certification request" in {
      checkPolicyFailure(
        correctInterval,
        Products.MobileCertification,
        priceIn(0L, Long.MaxValue),
        inRegion(0)
      )
    }

  }
}

object AutoRuCertificationSpec {

  def certification(price: Funds): AutoRuProduct =
    AutoRuProduct(
      Products.Certification,
      Set(
        AutoRuGood(
          Goods.Custom,
          Costs.PerIndexing,
          price
        )
      ),
      duration = DefaultDuration
    )

  def mobileCertification(price: Funds): AutoRuProduct =
    AutoRuProduct(
      Products.MobileCertification,
      Set(
        AutoRuGood(
          Goods.Custom,
          Costs.PerIndexing,
          price
        )
      ),
      duration = DefaultDuration
    )

}
