package ru.yandex.vertis.billing.banker.dao.impl.jdbc

import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.util.{CleanableDao, CleanableJdbcPaymentSystemDao}
import ru.yandex.vertis.billing.banker.dao.{DowntimeMethodDao, DowntimeMethodDaoSpec, PaymentSystemDao}
import ru.yandex.vertis.billing.banker.model.PaymentSystemIds

class JdbcDowntimeMethodDaoSpec extends DowntimeMethodDaoSpec with JdbcSpecTemplate {

  override def payments: PaymentSystemDao with CleanableDao =
    new JdbcPaymentSystemDao(database, PaymentSystemIds.YandexKassaV3) with CleanableJdbcPaymentSystemDao

  override def downtimeMethods: DowntimeMethodDao =
    new JdbcDowntimeMethodDao(database, PaymentSystemIds.YandexKassaV3)
}
