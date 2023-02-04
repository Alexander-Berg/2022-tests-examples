package ru.yandex.vertis.billing.banker.dao.impl.jdbc

import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.TrustPlusBonusDaoSpec
import ru.yandex.vertis.billing.banker.dao.util.{
  CleanableJdbcAccountDao,
  CleanableJdbcAccountTransactionDao,
  CleanableJdbcPaymentSystemDao,
  CleanableJdbcTrustPlusBonusDao
}
import ru.yandex.vertis.billing.banker.model.PaymentSystemIds

class JdbcTrustPlusBonusDaoSpec extends TrustPlusBonusDaoSpec with JdbcSpecTemplate {

  override protected def trustPlusBonusDao: CleanableJdbcTrustPlusBonusDao =
    new JdbcTrustPlusBonusDao(database) with CleanableJdbcTrustPlusBonusDao

  override protected val accountDao =
    new JdbcAccountDao(database) with CleanableJdbcAccountDao

  override protected val accountTransactionDao =
    new GlobalJdbcAccountTransactionDao(database)(ec) with CleanableJdbcAccountTransactionDao

  override protected val paymentSystemDao =
    new JdbcPaymentSystemDao(database, PaymentSystemIds.Trust) with CleanableJdbcPaymentSystemDao
}
