package ru.yandex.vertis.moderation.util

import org.scalacheck.Gen
import ru.yandex.vertis.moderation.api.view._
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.RichGen

/**
  * Generators for moderation-tms specific objects
  *
  * @author potseluev
  */
object TmsGenerators {

  val SchedulerJobResultGen: Gen[JobResult] =
    for {
      resultType <- Gen.oneOf(JobResultTypes.values.toSeq)
      finishTime <- LongGen
      duration   <- LongGen
      message    <- StringGen.?
    } yield JobResult(
      resultType = resultType,
      finishTime = finishTime,
      duration = duration,
      message = message
    )

  val SchedulerJobGen: Gen[SchedulerJob] =
    for {
      startTime <- DateTimeGen
      startedOn <- StringGen
      result    <- SchedulerJobResultGen.?
    } yield SchedulerJob(
      startTime = startTime,
      startedOn = startedOn,
      result = result
    )

  val PeriodicTaskParamsGen: Gen[PeriodicTaskParams] =
    for {
      scheduleIntervalSec <- LongGen
      enabled             <- BooleanGen
    } yield PeriodicTaskParams(scheduleIntervalSec, enabled)

  val SchedulerTaskInfoGen: Gen[SchedulerTaskInfo] =
    for {
      taskId             <- StringGen
      lastJob            <- SchedulerJobGen.?
      periodicTaskParams <- PeriodicTaskParamsGen.?
    } yield SchedulerTaskInfo(
      taskId = taskId,
      lastJob = lastJob,
      periodicParams = periodicTaskParams
    )
}
