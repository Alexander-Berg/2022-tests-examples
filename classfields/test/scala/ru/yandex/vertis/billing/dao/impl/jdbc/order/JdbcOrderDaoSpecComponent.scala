package ru.yandex.vertis.billing.dao.impl.jdbc.order

import org.scalatest.Suite
import ru.yandex.vertis.billing.dao.impl.jdbc.{JdbcCustomerDao, JdbcSpecTemplate}
import ru.yandex.vertis.billing.dao.{CustomerDao, OrderDao}

/**
  * Trait for testing [[OrderDao]]
  *
  * @author dimas
  */
trait JdbcOrderDaoSpecComponent extends JdbcSpecTemplate {
  this: Suite =>

  protected lazy val customerDao: CustomerDao = new JdbcCustomerDao(billingDatabase)

  protected lazy val orderDao: OrderDao = new JdbcOrderDao(billingDualDatabase)

}
