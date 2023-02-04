package ru.yandex.vertis.billing.dao.impl.jdbc

import ru.yandex.vertis.billing.dao.{MonthlyDiscountDao, MonthlyDiscountDaoSpec}

/**
  * @author ruslansd
  */
class JdbcMonthlyDiscountDaoSpec extends MonthlyDiscountDaoSpec with JdbcSpecTemplate {
  override protected def dao: MonthlyDiscountDao = new JdbcMonthlyDiscountDao(billingDatabase)
}
