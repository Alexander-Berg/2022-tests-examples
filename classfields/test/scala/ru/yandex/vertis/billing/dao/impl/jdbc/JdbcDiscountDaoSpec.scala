package ru.yandex.vertis.billing.dao.impl.jdbc

import ru.yandex.vertis.billing.dao.{CustomerDao, DiscountDao, DiscountDaoSpec}

/**
  * Runnable spec on [[JdbcDiscountDao]]
  *
  * @author ruslansd
  */
class JdbcDiscountDaoSpec extends DiscountDaoSpec with JdbcSpecTemplate {

  protected val dao: DiscountDao = new JdbcDiscountDao(billingDatabase)

  protected val customerDao: CustomerDao = new JdbcCustomerDao(billingDatabase)

}
