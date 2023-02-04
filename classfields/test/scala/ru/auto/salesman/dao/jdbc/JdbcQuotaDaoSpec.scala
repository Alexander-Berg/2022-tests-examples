package ru.auto.salesman.dao.jdbc

import ru.auto.salesman.dao.QuotaDaoSpec
import ru.auto.salesman.dao.impl.jdbc.JdbcQuotaDao
import ru.auto.salesman.test.template.SalesmanJdbcSpecTemplate

class JdbcQuotaDaoSpec extends QuotaDaoSpec with SalesmanJdbcSpecTemplate {
  protected val dao = new JdbcQuotaDao(database)

}
