package ru.auto.salesman.test.model.gens.user

import org.scalacheck.Gen
import ru.auto.salesman.service.ScheduleInstanceService
import ru.yandex.vertis.generators.DateTimeGenerators._

trait ScheduleServiceModelGenerators {

  val ScheduleInstanceSourceGen: Gen[ScheduleInstanceService.Source] = for {
    scheduleId <- Gen.posNum[Long]
    fireTime <- dateTimeInFuture()
    scheduleUpdateTime <- dateTimeInPast()
  } yield ScheduleInstanceService.Source(scheduleId, fireTime, scheduleUpdateTime)

}
