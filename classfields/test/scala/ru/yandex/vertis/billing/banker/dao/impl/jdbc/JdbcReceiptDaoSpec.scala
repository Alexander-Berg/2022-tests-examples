package ru.yandex.vertis.billing.banker.dao.impl.jdbc

import ru.yandex.vertis.billing.banker.dao.{AccountDao, ReceiptDao, ReceiptDaoSpec}

/**
  * @author ruslansd
  */
class JdbcReceiptDaoSpec extends ReceiptDaoSpec {
  override def dao: ReceiptDao = new JdbcReceiptDao(database)

  override def accountDao: AccountDao = new JdbcAccountDao(database)
}
