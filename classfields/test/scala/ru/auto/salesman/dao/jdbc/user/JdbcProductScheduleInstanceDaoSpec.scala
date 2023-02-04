package ru.auto.salesman.dao.jdbc.user

import ru.auto.salesman.dao.impl.jdbc.user.JdbcProductScheduleInstanceDao
import ru.auto.salesman.dao.jdbc.JdbcScheduleInstanceDaoSpec

class JdbcProductScheduleInstanceDaoSpec extends JdbcScheduleInstanceDaoSpec {

  override protected def table: String = JdbcProductScheduleInstanceDao.table

  override protected def scheduleIdColumn: String =
    JdbcProductScheduleInstanceDao.scheduleIdColumn
}
