package ru.yandex.vertis.billing.dao.impl.jdbc.order

import ru.yandex.vertis.billing.dao.impl.jdbc.JdbcNotifyClientDao
import ru.yandex.vertis.billing.dao.NotifyClientDaoSpec
import ru.yandex.vertis.billing.util.clean.CleanableNotifyClientDao

/**
  * @author ruslansd
  */
class JdbcNotifyClientDaoSpec extends NotifyClientDaoSpec {

  override protected def dao: JdbcNotifyClientDao with CleanableNotifyClientDao =
    new JdbcNotifyClientDao(billingDatabase) with CleanableNotifyClientDao

}
