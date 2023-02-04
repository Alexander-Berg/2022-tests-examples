package ru.yandex.vertis.billing.dao.impl.jdbc

import ru.yandex.vertis.billing.dao.{KeyValueDao, KeyValueDaoSpec}

/**
  * Runnable spec on [[ru.yandex.vertis.billing.dao.KeyValueDaoSpec]]
  */
class JdbcKeyValueDaoSpec extends KeyValueDaoSpec with JdbcSpecTemplate {

  val keyValueDao: KeyValueDao = new JdbcKeyValueDao(billingDatabase)

}
