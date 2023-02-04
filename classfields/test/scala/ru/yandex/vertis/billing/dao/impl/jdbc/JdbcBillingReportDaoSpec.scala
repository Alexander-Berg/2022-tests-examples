package ru.yandex.vertis.billing.dao.impl.jdbc

import ru.yandex.vertis.billing.dao.impl.jdbc.order.{JdbcOrderDao, JdbcOrderDaoSpecComponent}
import ru.yandex.vertis.billing.dao.BillingReportDaoSpec

/**
  * Spec on [[JdbcBillingReportDao]]
  *
  * @author ruslansd
  */
class JdbcBillingReportDaoSpec extends BillingReportDaoSpec with JdbcOrderDaoSpecComponent {

  protected val billingReportDao = new JdbcBillingReportDao(billingDatabase)

}
