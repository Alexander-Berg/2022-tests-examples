package ru.auto.salesman.model

import ru.auto.salesman.model.UniqueProductType._
import ru.auto.salesman.test.BaseSpec

class UniqueProductTypeSpec extends BaseSpec {
  "productIdForPriceService" should {
    "right converts ApplicationCreditAccess" in {
      ApplicationCreditAccess.externalProductId.right.value shouldBe ProductId.CreditApplication
    }

    "right converts ApplicationCreditSingle" in {
      ApplicationCreditSingle.externalProductId.right.value shouldBe ProductId.SingleCreditApplication
    }

    "right converts GibddHistoryReport" in {
      GibddHistoryReport.externalProductId.right.value shouldBe ProductId.GibddHistoryReport
    }

    "right converts CmExpertHistoryReport" in {
      CmExpertHistoryReport.externalProductId.right.value shouldBe ProductId.CmExpertHistoryReport
    }

    "right converts FullHistoryReport to VinHistory" in {
      FullHistoryReport.externalProductId.right.value shouldBe ProductId.VinHistory
    }
  }
}
