package ru.auto.salesman.dao.jdbc

import ru.auto.salesman.dao.VinHistoryDaoSpec
import ru.auto.salesman.dao.impl.jdbc.JdbcVinHistoryDao
import ru.auto.salesman.test.template.SalesmanJdbcSpecTemplate

class JdbcVinHistoryDaoSpec extends VinHistoryDaoSpec with SalesmanJdbcSpecTemplate {
  def vinHistoryDao = new JdbcVinHistoryDao(database)
}
