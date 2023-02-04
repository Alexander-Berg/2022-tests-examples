package ru.yandex.vertis.billing.dao.impl.jdbc

import ru.yandex.vertis.billing.dao.{EmonBillingOperationTaskDao, EmonBillingOperationTaskDaoSpec}
import ru.yandex.vertis.billing.util.clean.{CleanableDao, CleanableEmonBillingOperationTaskDao}

/**
  * @author rmuzhikov
  */
class JdbcEmonBillingOperationTaskDaoSpec extends EmonBillingOperationTaskDaoSpec with JdbcSpecTemplate {

  override protected val taskDao: EmonBillingOperationTaskDao with CleanableDao = new JdbcEmonBillingOperationTaskDao(
    eventStorageDatabase
  ) with CleanableEmonBillingOperationTaskDao
}
