package ru.yandex.realty.seller.dao.jdbc

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.seller.dao.{PurchaseDbActions, PurchaseDbActionsSpec}
import ru.yandex.realty.seller.db.mysql.SellerJdbcSpecBase

@RunWith(classOf[JUnitRunner])
class PurchaseDbActionsImplSpec extends PurchaseDbActionsSpec with SellerJdbcSpecBase {

  override val purchaseDb: PurchaseDbActions =
    new PurchaseDbActionsImpl()

}
