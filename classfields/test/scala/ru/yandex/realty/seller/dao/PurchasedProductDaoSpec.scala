package ru.yandex.realty.seller.dao

import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.logging.Logging
import ru.yandex.realty.seller.model.gen.SellerModelGenerators

/**
  * @author Vsevolod Levin
  */
trait PurchasedProductDaoSpec extends AsyncSpecBase with SellerModelGenerators with Logging {

  def dao: PurchasedProductDao

  "PurchasedProductDao" when {

    "update products" should {
      "handle empty" in {
        dao.updateProducts(Iterable.empty).futureValue
      }
    }

    "get products" should {
      "handle empty" in {
        dao.getProducts(Iterable.empty).futureValue
      }
    }
  }
}
