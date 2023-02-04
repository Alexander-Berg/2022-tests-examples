package ru.yandex.vertis.billing.banker.dao.impl.jdbc

import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.AccountTransactionDaoRequestsSpec
import ru.yandex.vertis.billing.banker.dao.util.CleanableJdbcAccountTransactionDao
import ru.yandex.vertis.billing.banker.model.Account

/**
  * Runnable specs on [[JdbcAccountTransactionDao]] transaction requests execution
  *
  * @author alex-kovalenko
  */
class JdbcAccountTransactionDaoRequestsSpec extends AccountTransactionDaoRequestsSpec with JdbcSpecTemplate {

  val accounts = new JdbcAccountDao(database)

  val account = accounts.upsert(Account("acc1", "u1")).futureValue.id

  val transactions =
    new GlobalJdbcAccountTransactionDao(database) with CleanableJdbcAccountTransactionDao

}
