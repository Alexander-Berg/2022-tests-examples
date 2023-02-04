package ru.yandex.realty.seller.dao.jdbc

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.seller.dao.{CardBindStatusDbActions, CardBindStatusDbActionsSpec}
import ru.yandex.realty.seller.db.mysql.SellerJdbcSpecBase

@RunWith(classOf[JUnitRunner])
class CardBindStatusDbActionsImplSpec extends CardBindStatusDbActionsSpec with SellerJdbcSpecBase {
  override def statusesDb: CardBindStatusDbActions = new CardBindStatusDbActionsImpl()
}
