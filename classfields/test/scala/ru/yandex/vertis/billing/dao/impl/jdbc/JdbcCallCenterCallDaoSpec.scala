package ru.yandex.vertis.billing.dao.impl.jdbc

import ru.yandex.vertis.billing.dao.CallCenterCallDaoSpec
import ru.yandex.vertis.billing.util.clean.CleanableCallCenterCallDao

class JdbcCallCenterCallDaoSpec extends CallCenterCallDaoSpec with JdbcSpecTemplate {

  override protected def callCenterDao: CleanableCallCenterCallDao = {
    new JdbcCallCenterCallDao(campaignEventDatabase) with CleanableCallCenterCallDao
  }

}
