package ru.auto.salesman.dao.jdbc

import ru.auto.salesman.dao.BalanceOrderDaoSpec
import ru.auto.salesman.dao.impl.jdbc.JdbcBalanceOrderDao
import ru.auto.salesman.test.template.BalanceJdbcSpecTemplate

class JdbcBalanceOrderDaoSpec extends BalanceOrderDaoSpec with BalanceJdbcSpecTemplate {

  val balanceOrderDao = new JdbcBalanceOrderDao(database)
}
