package ru.yandex.realty.seller.dao.jdbc

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.seller.dao.{PurchasedProductDbActions, PurchasedProductDbActionsSpec}
import ru.yandex.realty.seller.db.mysql.SellerJdbcSpecBase

@RunWith(classOf[JUnitRunner])
class PurchasedProductDbActionsImplSpec extends PurchasedProductDbActionsSpec with SellerJdbcSpecBase {

  override val productDb: PurchasedProductDbActions =
    new PurchasedProductDbActionsImpl()

}
