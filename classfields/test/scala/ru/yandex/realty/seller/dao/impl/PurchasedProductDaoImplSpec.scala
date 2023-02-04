package ru.yandex.realty.seller.dao.impl

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.application.ng.db.MasterSlaveJdbcDatabase
import ru.yandex.realty.seller.dao.PurchasedProductDaoSpec
import ru.yandex.realty.seller.dao.jdbc.PurchasedProductDbActionsImpl
import ru.yandex.realty.seller.db.mysql.SellerJdbcSpecBase

@RunWith(classOf[JUnitRunner])
class PurchasedProductDaoImplSpec extends PurchasedProductDaoSpec with SellerJdbcSpecBase {

  private val productDb =
    new PurchasedProductDbActionsImpl()

  override val dao = new PurchasedProductDaoImpl(productDb, MasterSlaveJdbcDatabase(database, database))
}
