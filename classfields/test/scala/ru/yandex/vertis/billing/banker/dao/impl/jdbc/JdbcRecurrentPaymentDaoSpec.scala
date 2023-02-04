package ru.yandex.vertis.billing.banker.dao.impl.jdbc

import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.RecurrentPaymentDaoSpec
import ru.yandex.vertis.billing.banker.dao.util.CleanableJdbcRecurrentPaymentDao

class JdbcRecurrentPaymentDaoSpec extends RecurrentPaymentDaoSpec with JdbcSpecTemplate {

  override protected def recurrentPaymentDao: CleanableJdbcRecurrentPaymentDao =
    new JdbcRecurrentPaymentDao(database) with CleanableJdbcRecurrentPaymentDao
}
