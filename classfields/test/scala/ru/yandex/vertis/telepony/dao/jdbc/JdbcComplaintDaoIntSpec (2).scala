package ru.yandex.vertis.telepony.dao.jdbc

import ru.yandex.vertis.telepony.dao.ComplaintDaoSpec
import ru.yandex.vertis.telepony.util.SharedDbSupport

class JdbcComplaintDaoIntSpec extends ComplaintDaoSpec with SharedDbSupport {
  override val dao = new JdbcComplaintDao(sharedDualDb)
  override val numberDao = new SharedOperatorNumberDao(sharedDualDb)
}
