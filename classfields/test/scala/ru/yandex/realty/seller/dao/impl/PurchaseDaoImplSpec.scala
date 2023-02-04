package ru.yandex.realty.seller.dao.impl

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.application.ng.db.MasterSlaveJdbcDatabase
import ru.yandex.realty.seller.dao.PurchaseDaoSpec
import ru.yandex.realty.seller.dao.jdbc.{PurchaseDbActionsImpl, PurchasedProductDbActionsImpl}
import ru.yandex.realty.seller.db.mysql.SellerJdbcSpecBase

@RunWith(classOf[JUnitRunner])
class PurchaseDaoImplSpec extends PurchaseDaoSpec with SellerJdbcSpecBase {

  private val productDb = new PurchasedProductDbActionsImpl()
  private val purchaseDb = new PurchaseDbActionsImpl()

  override val dao = new PurchaseDaoImpl(purchaseDb, productDb, MasterSlaveJdbcDatabase(database, database))

}
