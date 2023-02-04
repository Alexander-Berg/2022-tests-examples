package ru.yandex.vertis.billing.dao

import ru.yandex.vertis.billing.model_core.notNull

/**
  * @author ruslansd
  */
trait CallsSearchAutocloseableDaoBaseSpec extends CallsSearchDaoBaseSpec {

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    if (notNull(callsSearchDao)) callsSearchDao.cleanup().get
    callsSearchDao = callRequestsDaoFactory.instance().futureValue
  }

}
