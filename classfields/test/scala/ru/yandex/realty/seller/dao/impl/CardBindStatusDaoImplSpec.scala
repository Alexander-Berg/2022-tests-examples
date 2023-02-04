package ru.yandex.realty.seller.dao.impl

import ru.yandex.realty.application.ng.db.MasterSlaveJdbcDatabase
import ru.yandex.realty.seller.dao.jdbc.CardBindStatusDbActionsImpl
import ru.yandex.realty.seller.dao.{CardBindStatusDao, CardBindStatusDaoSpec}
import ru.yandex.realty.seller.db.mysql.SellerJdbcSpecBase
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CardBindStatusDaoImplSpec extends CardBindStatusDaoSpec with SellerJdbcSpecBase {

  private val statusesDb = new CardBindStatusDbActionsImpl()

  override def dao: CardBindStatusDao =
    new CardBindStatusDaoImpl(statusesDb, MasterSlaveJdbcDatabase(database, database))
}
