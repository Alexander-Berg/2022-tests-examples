package ru.yandex.vertis.billing.dao.impl.jdbc

import ru.yandex.vertis.billing.dao.ClientDaoSpec

/**
  * Runnable spec on [[ClientDaoSpec]]
  */
class JdbcClientDaoSpec extends ClientDaoSpec with JdbcSpecTemplate {

  protected val clientDao: JdbcClientDao = new JdbcClientDao(billingDatabase)

  protected val customerDao: JdbcCustomerDao = new JdbcCustomerDao(billingDatabase)

}
