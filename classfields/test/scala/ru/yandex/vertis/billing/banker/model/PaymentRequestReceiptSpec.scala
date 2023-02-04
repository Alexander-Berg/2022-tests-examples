package ru.yandex.vertis.billing.banker.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.banker.model.PaymentRequest.{ReceiptData, ReceiptGood}

/**
  * Spec on [[PaymentRequest.ReceiptData]]
  */
class PaymentRequestReceiptSpec extends AnyWordSpec with Matchers {

  "PaymentRequest.ReceiptData" should {
    "fail if get empty goods" in {
      intercept[IllegalArgumentException] {
        ReceiptData(Seq.empty[ReceiptGood], Some("email"))
      }
    }
    "fail if get empty email" in {
      intercept[IllegalArgumentException] {
        ReceiptData(Seq(ReceiptGood("g1", 1, 100L)), Some(""))
      }
    }

    "correctly calculate total cost" in {
      val good1 = ReceiptGood("g1", 1, 100L)
      ReceiptData(Seq(good1), Some("email")).totalCost shouldBe 100L

      val good2 = ReceiptGood("g2", 2, 100L)
      ReceiptData(Seq(good2), Some("email")).totalCost shouldBe 200L

      ReceiptData(Seq(good1, good2), Some("email")).totalCost shouldBe 300L
    }
  }

}
