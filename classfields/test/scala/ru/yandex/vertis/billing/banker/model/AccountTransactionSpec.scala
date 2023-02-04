package ru.yandex.vertis.billing.banker.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.billing.banker.model.AccountTransactions.{Incoming, Withdraw}
import ru.yandex.vertis.billing.banker.model.gens.{accountTransactionGen, Producer}

/**
  * Specs on [[AccountTransaction]]
  * We have a lot of places where we rely on model invariants,
  * so uncontrolled changing may be dangerous and it is a good idea to fasten them in tests.
  */
class AccountTransactionSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {

  private def nonIncomeTransaction(source: AccountTransaction): Unit = {
    "accept only zero income" in {
      forAll { (i: Funds) =>
        whenever(i != 0) {
          intercept[IllegalArgumentException] {
            source.copy(income = i)
          }
        }
      }
    }
  }

  private def nonOverdraftTransaction(source: AccountTransaction): Unit = {
    "accept only zero overdraft" in {
      forAll { (o: Funds) =>
        whenever(o != 0) {
          intercept[IllegalArgumentException] {
            source.copy(overdraft = o)
          }
        }

      }
    }
  }

  "AccountTransaction.Incoming" should {
    val tr = accountTransactionGen(Incoming).next
      .copy(income = 0, withdraw = 0)

    behave.like(nonOverdraftTransaction(tr))

    "accept only zero withdraw with negative incoming" in {
      tr.copy(income = -1, withdraw = 0)
      forAll { (i: Funds, w: Funds) =>
        whenever(i <= 0 && w != 0) {
          intercept[IllegalArgumentException] {
            tr.copy(income = i, withdraw = w)
          }
        }
      }
    }

    "accept withdraw = zero or income for non-negative income" in {
      tr.copy(income = 1, withdraw = 0)
      tr.copy(income = 1, withdraw = 1)
      forAll { (w: Funds, i: Funds) =>
        whenever(i >= 0 && w != 0 && w != i) {
          intercept[IllegalArgumentException] {
            tr.copy(income = i, withdraw = w)
          }
        }
      }

    }
  }

  "AccountTransaction.Withdraw" should {
    val tr = accountTransactionGen(Withdraw).next
      .copy(income = 0, withdraw = 0, overdraft = 0)

    behave.like(nonIncomeTransaction(tr))

    "accept non-negative withdraw" in {
      tr.copy(withdraw = 1)
      forAll { (w: Funds) =>
        whenever(w < 0) {
          intercept[IllegalArgumentException] {
            tr.copy(withdraw = w)
          }
        }
      }
    }

    "accept non-negative overdraft" in {
      tr.copy(overdraft = 1)
      forAll { (o: Funds) =>
        whenever(o < 0) {
          intercept[IllegalArgumentException] {
            tr.copy(overdraft = o)
          }
        }
      }
    }
  }
}
