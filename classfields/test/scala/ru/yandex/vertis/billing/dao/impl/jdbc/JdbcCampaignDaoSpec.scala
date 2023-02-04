package ru.yandex.vertis.billing.dao.impl.jdbc

import ru.yandex.vertis.billing.dao._
import ru.yandex.vertis.billing.dao.impl.jdbc.order.JdbcOrderDao

class JdbcCampaignDaoSpec extends CampaignDaoSpec with JdbcSpecTemplate {

  protected val customerDao: CustomerDao = new JdbcCustomerDao(billingDatabase)

  protected val campaignDao: CampaignDao = new JdbcCampaignDao(billingDatabase)

  protected val orderDao: OrderDao = new JdbcOrderDao(billingDualDatabase)

}
