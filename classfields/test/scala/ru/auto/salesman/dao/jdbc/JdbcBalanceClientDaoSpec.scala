package ru.auto.salesman.dao.jdbc

import ru.auto.salesman.dao.BalanceClientDaoSpec
import ru.auto.salesman.dao.impl.jdbc.JdbcBalanceClientDao
import ru.auto.salesman.test.template.BalanceJdbcSpecTemplate

class JdbcBalanceClientDaoSpec extends BalanceClientDaoSpec with BalanceJdbcSpecTemplate {

  val balanceClientDao = new JdbcBalanceClientDao(database)
}
