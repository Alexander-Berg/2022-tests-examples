package ru.yandex.vertis.billing.banker.dao.impl.jdbc

import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.AccountDaoSpec

/**
  * JDBC impl for [[AccountDaoSpec]]
  *
  * @author alesavin
  */
class JdbcAccountDaoSpec extends AccountDaoSpec with JdbcSpecTemplate {

  override val accounts =
    new JdbcAccountDao(database)
}
