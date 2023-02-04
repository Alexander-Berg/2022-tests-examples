package ru.yandex.vertis.billing.dao.impl.jdbc

import ru.yandex.vertis.billing.dao.BindingDaoSpec

/**
  * Runnable spec on [[JdbcBindingDao]]
  *
  * @author dimas
  */
class JdbcBindingDaoSpec extends BindingDaoSpec with JdbcSpecTemplate {

  protected val bindingDao = new JdbcBindingDao(billingDatabase)

}
