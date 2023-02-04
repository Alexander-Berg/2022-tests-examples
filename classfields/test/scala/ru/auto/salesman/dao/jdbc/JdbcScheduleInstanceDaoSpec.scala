package ru.auto.salesman.dao.jdbc

import ru.auto.salesman.dao.ScheduleInstanceDao
import ru.auto.salesman.dao.impl.jdbc.database.Database
import ru.auto.salesman.dao.impl.jdbc.test.JdbcScheduleInstanceDaoForTests
import ru.auto.salesman.dao.user.ScheduleInstanceDaoSpec
import ru.auto.salesman.model.ScheduleInstance
import ru.auto.salesman.test.template.SalesmanUserJdbcSpecTemplate

trait JdbcScheduleInstanceDaoSpec
    extends ScheduleInstanceDaoSpec
    with SalesmanUserJdbcSpecTemplate {

  protected def database: Database

  protected def table: String

  protected def scheduleIdColumn: String

  private val dao =
    new JdbcScheduleInstanceDaoForTests(database, table, scheduleIdColumn)

  def newDao(instances: Iterable[ScheduleInstance]): ScheduleInstanceDao = {
    dao.clean()
    if (instances.nonEmpty) dao.insertInstances(instances)
    dao
  }
}
