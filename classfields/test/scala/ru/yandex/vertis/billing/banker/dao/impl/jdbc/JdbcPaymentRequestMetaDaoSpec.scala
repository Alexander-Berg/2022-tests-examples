package ru.yandex.vertis.billing.banker.dao.impl.jdbc

import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.util.{CleanableDao, CleanableJdbcPaymentRequestMetaDao}
import ru.yandex.vertis.billing.banker.dao.{PaymentRequestMetaDao, PaymentRequestMetaDaoSpec}

/**
  * Runnable specs on [[JdbcPaymentRequestMetaDao]]
  *
  * @author alex-kovalenko
  */
class JdbcPaymentRequestMetaDaoSpec extends PaymentRequestMetaDaoSpec with JdbcSpecTemplate {

  def dao: PaymentRequestMetaDao with CleanableDao =
    new JdbcPaymentRequestMetaDao(database) with CleanableJdbcPaymentRequestMetaDao
}
