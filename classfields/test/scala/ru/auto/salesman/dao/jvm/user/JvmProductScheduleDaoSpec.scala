package ru.auto.salesman.dao.jvm.user

import ru.auto.salesman.dao.impl.jvm.user.JvmProductScheduleDao
import ru.auto.salesman.dao.user.{
  ProductScheduleDao,
  ProductScheduleDaoSpec,
  ProductScheduleInsertDao
}
import ru.auto.salesman.model.user.schedule.ProductSchedule

import scala.util.Try

class JvmProductScheduleDaoSpec extends ProductScheduleDaoSpec {

  def newDao(
      data: Iterable[ProductSchedule]
  ): ProductScheduleDao with ProductScheduleInsertDao =
    new JvmProductScheduleDao(data) with ProductScheduleInsertDao {

      override def insert(requests: Iterable[ProductSchedule]): Try[Unit] =
        super.insert(requests)
    }
}
