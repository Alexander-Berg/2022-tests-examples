package ru.auto.salesman.test.model.gens

import org.scalacheck.Gen
import ru.auto.salesman.model.ScheduleInstance
import ru.auto.salesman.model.ScheduleInstance.{Status, Statuses}
import ru.auto.salesman.test.model.gens.BasicSalesmanGenerators._

trait ScheduleInstanceGenerators {

  val StatusGen: Gen[Status] = enumGen(Statuses)

  val NonCompletedStatus: Gen[Status] = Gen.oneOf(
    Statuses.Cancelled,
    Statuses.Acquired,
    Statuses.Skipped,
    Statuses.Pending
  )
  val FailedStatus: Gen[Status] = Gen.const(Statuses.Failed)
  val DoneStatus: Gen[Status] = Gen.const(Statuses.Done)

  val ScheduleInstanceGen: Gen[ScheduleInstance] = for {
    id <- posNum[Long]
    scheduleId <- Gen.posNum[Long]
    fireTime <- dateTimeInFuture()
    createTime <- dateTimeInPast()
    scheduleUpdateTime <- dateTimeInPast()
    status <- StatusGen
    epoch = createTime
  } yield
    ScheduleInstance(
      id,
      scheduleId,
      fireTime,
      createTime,
      scheduleUpdateTime,
      status,
      epoch
    )
}

object ScheduleInstanceGenerators extends ScheduleInstanceGenerators
