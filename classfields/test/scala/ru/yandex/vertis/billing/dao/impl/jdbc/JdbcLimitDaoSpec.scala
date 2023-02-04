package ru.yandex.vertis.billing.dao.impl.jdbc

import ru.yandex.vertis.billing.dao.LimitDaoSpec
import ru.yandex.vertis.billing.dao.impl.jdbc.order.JdbcOrderDao

/**
  * Runnable spec on [[JdbcLimitDao]]
  */
class JdbcLimitDaoSpec extends LimitDaoSpec with JdbcSpecTemplate {

  protected lazy val limitDao = new JdbcLimitDao(billingDatabase)

  protected lazy val orderDao = new JdbcOrderDao(billingDualDatabase)

  protected lazy val campaignDao = new JdbcCampaignDao(billingDatabase)

  protected lazy val customerDao = new JdbcCustomerDao(billingDatabase)

}
