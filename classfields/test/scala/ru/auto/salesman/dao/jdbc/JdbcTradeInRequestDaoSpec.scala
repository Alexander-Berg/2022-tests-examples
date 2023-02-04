package ru.auto.salesman.dao.jdbc

import ru.auto.salesman.dao.TradeInRequestDaoSpec
import ru.auto.salesman.dao.impl.jdbc.JdbcTradeInRequestDao
import ru.auto.salesman.test.template.SalesmanJdbcSpecTemplate

class JdbcTradeInRequestDaoSpec
    extends TradeInRequestDaoSpec
    with SalesmanJdbcSpecTemplate {
  def tradeInDao = new JdbcTradeInRequestDao(database)
}
