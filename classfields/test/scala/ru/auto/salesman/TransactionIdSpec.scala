package ru.auto.salesman

import ru.auto.salesman.model.AutoruUser
import ru.auto.salesman.model.user.product.ProductSource.{AutoApply, AutoProlong}
import ru.auto.salesman.test.BaseSpec

class TransactionIdSpec extends BaseSpec {

  "TransactionId.apply" should {

    "generate autoapply transaction id" in {
      TransactionId(
        AutoruUser(4723984),
        AutoApply(2948)
      ) shouldBe "user:4723984-scheduled-2948"
    }

    "generate autoprolong transaction id" in {
      val result = TransactionId(
        AutoruUser(809432),
        AutoProlong("f8e276f358cdd992b21b2b1cd14d7fb1")
      )
      result shouldBe "user:809432-prolongation-f8e276f358cdd992b21b2b1cd14d7fb1"
    }
  }
}
