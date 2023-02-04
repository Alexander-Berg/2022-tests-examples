package ru.yandex.vertis.billing.banker.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.billing.banker.model.gens.AccountTransactionIdGen

/**
  * Specs on [[AccountTransactionId]] SerDe
  *
  * @author alex-kovalenko
  */
class AccountTransactionIdSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 10000, workers = 20)

  "AccountTransactionId" should {
    "serialize and read itself" in {
      forAll(AccountTransactionIdGen) { id =>
        AccountTransactionId.parse(id.value, id.`type`) shouldBe id
      }
    }
  }
}
