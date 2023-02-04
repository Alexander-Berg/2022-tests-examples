package ru.auto.salesman.service.impl.user

import ru.auto.salesman.dao.impl.jvm.JvmScheduleInstanceDao
import ru.auto.salesman.dao.impl.jvm.user.JvmProductScheduleDao
import ru.auto.salesman.model.ScheduleInstance
import ru.auto.salesman.model.user.schedule.ProductSchedule
import ru.auto.salesman.service.ScheduleService
import ru.auto.salesman.service.user.ProductScheduleServiceSpec

class ProductScheduleServiceImplSpec extends ProductScheduleServiceSpec {

  def newService(
      schedules: Iterable[ProductSchedule],
      scheduleInstances: Iterable[ScheduleInstance]
  ): ScheduleService[ProductSchedule] =
    new ProductScheduleServiceImpl(
      new JvmProductScheduleDao(schedules),
      new JvmScheduleInstanceDao(scheduleInstances)
    )
}
