package ru.yandex.vertis.moisha.impl.autoru.v1

import ru.yandex.vertis.moisha.impl.autoru.SingleProductDailyPolicySpec
import ru.yandex.vertis.moisha.impl.autoru.model._

/**
  * Specs on AutoRu policy for [[Products.Certification]]
  * Rates are described in project https://planner.yandex-team.ru/projects/31761/
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
trait AutoRuCertificationSpec extends SingleProductDailyPolicySpec {

  "AutoRuPolicyV1 policy" should {
    "not support product Certification" in {
      policy.products.contains(Products.Certification.toString) should be(false)
    }

    "not support product MobileCertification" in {
      policy.products.contains(Products.MobileCertification.toString) should be(false)
    }

    "fail on certification request" in {
      checkPolicyFailure(
        correctInterval,
        Products.Certification,
        priceIn(0L, Long.MaxValue),
        inMoscow
      )
    }

    "fail on mobile-certification request" in {
      checkPolicyFailure(
        correctInterval,
        Products.MobileCertification,
        priceIn(0L, Long.MaxValue),
        inMoscow
      )
    }

  }
}
