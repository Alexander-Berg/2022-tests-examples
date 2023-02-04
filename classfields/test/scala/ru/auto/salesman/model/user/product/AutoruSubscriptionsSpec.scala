package ru.auto.salesman.model.user.product

import ru.auto.salesman.model.DeprecatedDomains
import ru.auto.salesman.model.user.product.ProductProvider.AutoruSubscriptions
import ru.auto.salesman.test.BaseSpec

class AutoruSubscriptionsSpec extends BaseSpec {

  "AutoruSubscriptions" should {
    "return empty set of 'all'" in {
      AutoruSubscriptions.all shouldBe empty
    }
    "parse subscriptions from names" in {
      val counter = 10
      AutoruSubscriptions.Companion.values.foreach { companion =>
        val countable =
          AutoruSubscriptions.withCountable(companion.name, counter)
        val parsed = AutoruSubscriptions.withName(
          companion.name + "-" + counter,
          alsoByAlias = false
        )
        countable shouldBe parsed
        parsed.counter shouldBe counter
        parsed.name shouldBe companion.name
        parsed.domain shouldBe DeprecatedDomains.AutoRu
      }
    }
  }

}
