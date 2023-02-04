package ru.auto.salesman.dao.jvm.user

import ru.auto.salesman.dao.ScheduleInstanceDao
import ru.auto.salesman.dao.impl.jvm.JvmScheduleInstanceDao
import ru.auto.salesman.dao.user.ScheduleInstanceDaoSpec
import ru.auto.salesman.model.ScheduleInstance

class JvmProductScheduleInstanceDaoSpec extends ScheduleInstanceDaoSpec {

  def newDao(instances: Iterable[ScheduleInstance]): ScheduleInstanceDao =
    new JvmScheduleInstanceDao(instances)
}
