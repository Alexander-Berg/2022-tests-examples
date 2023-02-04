package ru.yandex.vertis.billing.dao.impl.doobie

import ru.yandex.vertis.billing.dao.{AutoruBalanceDao, AutoruBalanceDaoSpec}
import ru.yandex.vertis.billing.dao.impl.jdbc.{JdbcContainerSpec, JdbcSpecTemplate}

class DoobieAutoruBalanceDaoSpec extends AutoruBalanceDaoSpec with JdbcSpecTemplate {

  override def autoruBalanceDao: AutoruBalanceDao = {
    val transactor = JdbcContainerSpec.asTransactor(namedAutoruBalanceDatabase.name)
    new DoobieAutoruBalanceDao(transactor)
  }
}
