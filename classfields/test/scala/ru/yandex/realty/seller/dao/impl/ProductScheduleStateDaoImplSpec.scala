package ru.yandex.realty.seller.dao.impl

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.application.ng.db.MasterSlaveJdbcDatabase
import ru.yandex.realty.seller.dao.jdbc.ProductScheduleStateDbActionsImpl
import ru.yandex.realty.seller.dao.{ProductScheduleStateDao, ProductScheduleStateDaoSpec, ProductScheduleStateDbActions}
import ru.yandex.realty.seller.db.mysql.SellerJdbcSpecBase

@RunWith(classOf[JUnitRunner])
class ProductScheduleStateDaoImplSpec extends ProductScheduleStateDaoSpec with SellerJdbcSpecBase {
  override def actions: ProductScheduleStateDbActions = new ProductScheduleStateDbActionsImpl()

  override def dao: ProductScheduleStateDao =
    new ProductScheduleStateDaoImpl(
      actions,
      MasterSlaveJdbcDatabase(database, database)
    )
}
