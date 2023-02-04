package ru.yandex.realty.seller.dao.impl

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.ops.DaoMetrics
import ru.yandex.realty.seller.dao.jdbc.TariffDbActionsImpl
import ru.yandex.realty.seller.dao.{TariffDao, TariffDaoSpec}
import ru.yandex.realty.seller.db.mysql.SellerJdbcSpecBase

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class TariffDaoImplSpec extends TariffDaoSpec with SellerJdbcSpecBase {
  override def tariffDao: TariffDao =
    new TariffDaoImpl(
      new TariffDbActionsImpl(),
      masterSlaveDb2,
      DaoMetrics.stub()
    )
}
