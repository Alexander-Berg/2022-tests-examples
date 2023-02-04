package ru.yandex.vertis.billing.banker.dao.impl.jdbc

import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.TrustExternalPurchaseDaoSpec
import ru.yandex.vertis.billing.banker.dao.util.{
  CleanableJdbcAccountDao,
  CleanableJdbcPaymentSystemDao,
  CleanableJdbcTrustExternalPurchaseDao,
  CleanablePaymentSystemDao
}
import ru.yandex.vertis.billing.banker.model.PaymentSystemIds.Trust

class JdbcTrustExternalPurchaseDaoSpec extends TrustExternalPurchaseDaoSpec with JdbcSpecTemplate {

  override protected def accountDao: CleanableJdbcAccountDao =
    new JdbcAccountDao(database) with CleanableJdbcAccountDao

  override protected def paymentSystemDao: CleanablePaymentSystemDao =
    new JdbcPaymentSystemDao(database, Trust) with CleanableJdbcPaymentSystemDao

  override protected def purchaseDao: CleanableJdbcTrustExternalPurchaseDao =
    new JdbcTrustExternalPurchaseDao(database) with CleanableJdbcTrustExternalPurchaseDao

}
