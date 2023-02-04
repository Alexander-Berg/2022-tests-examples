package ru.yandex.vertis.billing.banker.dao.impl.jdbc

import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.TrustRecurrentDaoSpec
import ru.yandex.vertis.billing.banker.dao.util.CleanableJdbcTrustRecurrentDao

class JdbcTrustRecurrentDaoSpec extends TrustRecurrentDaoSpec with JdbcSpecTemplate {

  override protected def recurrentDao: CleanableJdbcTrustRecurrentDao =
    new JdbcTrustRecurrentDao(database) with CleanableJdbcTrustRecurrentDao
}
