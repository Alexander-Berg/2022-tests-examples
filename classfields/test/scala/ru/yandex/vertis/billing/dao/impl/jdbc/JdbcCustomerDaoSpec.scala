package ru.yandex.vertis.billing.dao.impl.jdbc

import ru.yandex.vertis.billing.dao.{CustomerDao, CustomerDaoSpec}

class JdbcCustomerDaoSpec extends CustomerDaoSpec with JdbcSpecTemplate {

  protected val customerDao: CustomerDao = new JdbcCustomerDao(billingDatabase)

}
