package ru.auto.salesman.dao.user

import ru.auto.salesman.model.user.schedule.ProductSchedule

import scala.util.Try

trait ProductScheduleInsertDao {
  def insert(schedules: Iterable[ProductSchedule]): Try[Unit]
}
