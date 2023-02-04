package ru.yandex.vertis.billing.dao.impl.jdbc

import ru.yandex.vertis.billing.dao.{EmonEventDao, EmonEventDaoSpec}
import ru.yandex.vertis.billing.util.clean.{CleanableDao, CleanableEmonEventDao}

/**
  * @author rmuzhikov
  */
class JdbcEmonEventDaoSpec extends EmonEventDaoSpec with JdbcSpecTemplate {

  override protected val emonEventDao: EmonEventDao with CleanableDao = new JdbcEmonEventDao(eventStorageDatabase)
    with CleanableEmonEventDao
}
