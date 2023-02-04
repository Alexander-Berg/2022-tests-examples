package ru.yandex.vertis.billing.dao.impl.jdbc

import ru.yandex.vertis.billing.dao.{PaidOffersDao, PaidOffersDaoSpec}

/**
  * Runnable spec on [[JdbcPaidOffersDao]]
  */
class JdbcPaidOffersDaoSpec extends PaidOffersDaoSpec with JdbcSpecTemplate {

  protected val paidOffersDao: PaidOffersDao =
    new JdbcPaidOffersDao(billingDualDatabase)

}
