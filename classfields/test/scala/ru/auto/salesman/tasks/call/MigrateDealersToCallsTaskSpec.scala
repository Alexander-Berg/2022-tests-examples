package ru.auto.salesman.tasks.call

import ru.auto.salesman.model.ProductId
import ru.auto.salesman.tasks.call.MigrateDealersToCallsTask.CarsNewProducts
import ru.auto.salesman.test.BaseSpec

class MigrateDealersToCallsTaskSpec extends BaseSpec {

  "MigrateDealersToCallsTask" should {
    "probably contain all 'new' products" in {
      ProductId.values.filter { product =>
        val p = product.toString
        p.contains("placement") && p.contains("cars") && p.contains("new")
      } should contain theSameElementsAs CarsNewProducts
    }
  }

}
