package ru.auto.api.managers.billing.trust

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.billing.trust.TrustGateConditions
import ru.auto.api.billing.trust.TrustGateConditions.CheckResult
import ru.auto.api.billing.v2.BillingModelV2.InitPaymentRequest.PurchaseCase
import ru.auto.api.managers.TestRequest
import ru.auto.api.model.AutoruProduct.OffersHistoryReports
import ru.auto.api.model.AutoruUser
import ru.yandex.vertis.Platform

class TrustGateConditionsSpec extends AnyWordSpecLike with Matchers with TestRequest {

  "TrustGateConditions.check" should {
    "pass" when {
      "enabled" in {
        TrustGateConditions(
          enabled = true,
          enabledUserIds = Set.empty,
          platforms = Set(Platform.PLATFORM_DESKTOP),
          minAndroidAppVersion = None,
          minIosAppVersion = None,
          purchaseCases = Set(PurchaseCase.AUTORU_PURCHASE),
          products = Set(OffersHistoryReports),
          firstPayment = true,
          probabilityPercent = 100
        ).check(
          user = AutoruUser(123),
          platform = Platform.PLATFORM_DESKTOP,
          purchaseCase = PurchaseCase.AUTORU_PURCHASE,
          products = Set(OffersHistoryReports)
        ) shouldBe CheckResult.Passed
      }
      "enabledUserIds" in {
        TrustGateConditions(
          enabled = false,
          enabledUserIds = Set("123"),
          platforms = Set(Platform.PLATFORM_DESKTOP),
          minAndroidAppVersion = None,
          minIosAppVersion = None,
          purchaseCases = Set(PurchaseCase.AUTORU_PURCHASE),
          products = Set(OffersHistoryReports),
          firstPayment = true,
          probabilityPercent = 100
        ).check(
          user = AutoruUser(123),
          platform = Platform.PLATFORM_DESKTOP,
          purchaseCase = PurchaseCase.AUTORU_PURCHASE,
          products = Set(OffersHistoryReports)
        ) shouldBe CheckResult.PassedUserId
      }
    }
    "reject" when {
      "enabled and conditions are not met" in {
        TrustGateConditions(
          enabled = true,
          enabledUserIds = Set.empty,
          platforms = Set(Platform.PLATFORM_DESKTOP),
          minAndroidAppVersion = None,
          minIosAppVersion = None,
          purchaseCases = Set(PurchaseCase.SUBSCRIBE_PURCHASE),
          products = Set(OffersHistoryReports),
          firstPayment = true,
          probabilityPercent = 100
        ).check(
          user = AutoruUser(123),
          platform = Platform.PLATFORM_DESKTOP,
          purchaseCase = PurchaseCase.AUTORU_PURCHASE,
          products = Set(OffersHistoryReports)
        ) shouldBe CheckResult.Rejected
      }
      "enabledUserIds and conditions are not met" in {
        TrustGateConditions(
          enabled = false,
          enabledUserIds = Set("123"),
          platforms = Set(Platform.PLATFORM_ANDROID, Platform.PLATFORM_IOS),
          minAndroidAppVersion = None,
          minIosAppVersion = None,
          purchaseCases = Set(PurchaseCase.AUTORU_PURCHASE),
          products = Set(OffersHistoryReports),
          firstPayment = true,
          probabilityPercent = 100
        ).check(
          user = AutoruUser(123),
          platform = Platform.PLATFORM_DESKTOP,
          purchaseCase = PurchaseCase.AUTORU_PURCHASE,
          products = Set(OffersHistoryReports)
        ) shouldBe CheckResult.Rejected
      }
    }
  }

}
