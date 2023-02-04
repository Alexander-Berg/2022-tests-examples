package ru.yandex.vertis.billing.dao.impl.jdbc

import ru.yandex.vertis.billing.dao.CallFactDaoSpec

/**
  * Runnable spec on [[JdbcCallFactDao]]
  *
  * @author ruslansd
  */
class JdbcCallFactDaoSpec extends CallFactDaoSpec with JdbcSpecTemplate {

  override protected val callFactDao = new JdbcCallFactDao(campaignEventDualDatabase)

}
