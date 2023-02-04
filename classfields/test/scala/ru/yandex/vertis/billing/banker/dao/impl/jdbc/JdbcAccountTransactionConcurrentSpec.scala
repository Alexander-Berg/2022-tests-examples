package ru.yandex.vertis.billing.banker.dao.impl.jdbc

import ru.yandex.vertis.billing.banker.dao.AccountTransactionConcurrentSpec
import ru.yandex.vertis.billing.banker.dao.util.{CleanableJdbcAccountDao, CleanableJdbcAccountTransactionDao}

/**
  * @author ruslansd
  */
class JdbcAccountTransactionConcurrentSpec extends AccountTransactionConcurrentSpec {

  override protected lazy val accountDao =
    new JdbcAccountDao(database)(ec) with CleanableJdbcAccountDao

  override protected lazy val transactionDao =
    new GlobalJdbcAccountTransactionDao(database)(ec) with CleanableJdbcAccountTransactionDao
}
