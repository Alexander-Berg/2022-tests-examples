package ru.yandex.vertis.billing.banker.model

import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.billing.banker.model.PaymentRequest.{Context, Options, Source, Targets}
import ru.yandex.vertis.billing.banker.model.gens.PaymentRequestReceiptGen

/**
  * Specs on [[PaymentRequest.Source]]
  */
class PaymentRequestSourceSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 1000, workers = 20)

  val id: AccountId = "acc1"

  "PaymentRequest.Source" should {
    "fail on empty account" in {
      intercept[IllegalArgumentException] {
        Source("", 1L, Payload.Empty, Options(), None, Context(Targets.Wallet), None, None)
      }
    }
    "fail on non-positive amount" in {
      val amountGen = Gen.oneOf(Gen.const(0L), Gen.negNum[Long])
      forAll(amountGen) { amount =>
        intercept[IllegalArgumentException] {
          Source(id, amount, Payload.Empty, Options(), None, Context(Targets.Wallet), None, None)
        }
      }
    }
    "pass empty receipt" in {
      Source(id, 100L, Payload.Empty, Options(), None, Context(Targets.Wallet), None, None)
    }
    "fail when totalCost does not equal to amount" in {
      val amountGen = Gen.posNum[Long]
      forAll(amountGen, PaymentRequestReceiptGen) { (amount, receipt) =>
        whenever(amount != receipt.totalCost) {
          intercept[IllegalArgumentException] {
            Source(id, amount, Payload.Empty, Options(), Some(receipt), Context(Targets.Wallet), None, None)
          }
        }
      }
    }
  }
}
