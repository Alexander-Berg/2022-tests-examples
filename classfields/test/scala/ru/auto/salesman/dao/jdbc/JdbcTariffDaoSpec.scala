package ru.auto.salesman.dao.jdbc

import ru.auto.salesman.dao.impl.jdbc.JdbcTariffDao
import ru.auto.salesman.dao.{TariffDao, TariffDaoSpec}
import ru.auto.salesman.test.template.SalesmanJdbcSpecTemplate

class JdbcTariffDaoSpec extends TariffDaoSpec with SalesmanJdbcSpecTemplate {

  override protected val dao: TariffDao =
    new JdbcTariffDao(database)
}
