package ru.yandex.realty.seller.dao

import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.seller.model.gen.SellerModelGenerators

trait PurchaseDaoSpec extends AsyncSpecBase with SellerModelGenerators {

  def dao: PurchaseDao

  "PurchaseDao" when {
    "update purchases" should {
      "handle empty" in {
        dao.updatePurchases(Iterable.empty).futureValue
      }
    }
  }
}
