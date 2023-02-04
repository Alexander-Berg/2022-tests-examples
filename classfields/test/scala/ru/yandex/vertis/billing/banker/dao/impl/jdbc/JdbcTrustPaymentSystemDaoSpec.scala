package ru.yandex.vertis.billing.banker.dao.impl.jdbc

import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.PaymentSystemDaoSpec
import ru.yandex.vertis.billing.banker.model.PaymentSystemIds

class JdbcTrustPaymentSystemDaoSpec extends PaymentSystemDaoSpec with JdbcSpecTemplate {

  override val accounts = new JdbcAccountDao(database)

  override val payments = new JdbcPaymentSystemDao(database, PaymentSystemIds.Trust)
}
