package ru.yandex.vertis.broker.distribute.test

import ru.yandex.vertis.broker.distribute.Job
import ru.yandex.vertis.broker.distribute.strategy.splitters.JobSplitter
import vertis.core.model.{DataCenter, DataCenters}

/**
  */
case class ConditionallySplittableJob(
    name: String,
    override val dataCenters: Set[DataCenter] = DataCenters.logbrokerDcs,
    partitions: Set[Int] = Set(0))
  extends Job {

  override val id: String =
    s"${dataCenters.mkString("-")}@${partitions.mkString("-")}->$name"
}

object ConditionallySplittableJob {

  implicit val halfSpliitableJobSplitter: JobSplitter[ConditionallySplittableJob] =
    new JobSplitter[ConditionallySplittableJob] {

      override def isSplittable(job: ConditionallySplittableJob): Boolean =
        job.partitions.size > 1

      override def subTasks(
          dataCenters: Set[DataCenter]
        )(job: ConditionallySplittableJob): Seq[ConditionallySplittableJob] = {
        Seq(
          job.copy(
            dataCenters = dataCenters
          )
        )
      }
    }
}
