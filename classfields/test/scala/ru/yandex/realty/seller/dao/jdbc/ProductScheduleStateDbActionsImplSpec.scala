package ru.yandex.realty.seller.dao.jdbc

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.seller.dao.{ProductScheduleStateDbActions, ProductScheduleStateDbActionsSpec}
import ru.yandex.realty.seller.db.mysql.SellerJdbcSpecBase

@RunWith(classOf[JUnitRunner])
class ProductScheduleStateDbActionsImplSpec extends ProductScheduleStateDbActionsSpec with SellerJdbcSpecBase {
  override def actions: ProductScheduleStateDbActions =
    new ProductScheduleStateDbActionsImpl()
}
