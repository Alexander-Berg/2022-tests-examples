package ru.yandex.vertis.billing.banker.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.banker.model.AccountTransactions.{Incoming, Withdraw}
import ru.yandex.vertis.billing.banker.model.gens.{incomingRqGen, requestReceiptGen, withdrawRqGen, Producer}

/**
  * Runnable specs on [[AccountTransactionRequest]]
  * We have a lot of places where we rely on model invariants,
  * so uncontrolled changing may be dangerous and it is a good idea to fasten them in tests.
  */
class AccountTransactionRequestSpec extends AnyWordSpec with Matchers {

  "IncomingRequest" should {
    "not be an instance on ConsumeAccountTransactionRequest" in {
      incomingRqGen().next.isInstanceOf[ConsumeAccountTransactionRequest] shouldBe false
    }
    "require correct type" in {
      val incoming = incomingRqGen().next
      val id = HashAccountTransactionId("h", Incoming)
      AccountTransactions.values.filter(_ != Incoming).foreach { t =>
        intercept[IllegalArgumentException] {
          incoming.copy(id = id.copy(`type` = t))
        }
      }
    }
    "accept any amount" in {
      val incoming = incomingRqGen().next
      incoming.copy(amount = 1)
      incoming.copy(amount = 0)
      incoming.copy(amount = -1)
    }
  }

  "WithdrawRequest" should {
    "be an instance on ConsumeAccountTransactionRequest" in {
      withdrawRqGen().next.isInstanceOf[ConsumeAccountTransactionRequest] shouldBe true
    }
    "require correct type" in {
      val withdraw = withdrawRqGen().next
      val id = HashAccountTransactionId("h", Withdraw)
      AccountTransactions.values.filter(_ != Withdraw).foreach { t =>
        intercept[IllegalArgumentException] {
          withdraw.copy(id = id.copy(`type` = t))
        }
      }
    }
    "require non-negative amount" in {
      val withdraw = withdrawRqGen().next.copy(receiptData = None)
      withdraw.copy(amount = 10)
      withdraw.copy(amount = 0)
      intercept[IllegalArgumentException] {
        withdraw.copy(amount = -1)
      }
    }
    "require equal receipt and transaction amount" in {
      val withdraw = withdrawRqGen().next.copy(receiptData = None)
      val receipt = withdraw.receiptData.getOrElse(requestReceiptGen(withdraw.amount).next)
      val withdrawWithReceipt = withdraw.copy(receiptData = Some(receipt))
      intercept[IllegalArgumentException] {
        withdrawWithReceipt.copy(amount = 10)
      }
      intercept[IllegalArgumentException] {
        withdrawWithReceipt.copy(amount = 0)
      }
      intercept[IllegalArgumentException] {
        withdrawWithReceipt.copy(amount = -1)
      }
    }
  }
}
