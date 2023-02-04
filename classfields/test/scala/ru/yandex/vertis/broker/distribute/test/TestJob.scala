package ru.yandex.vertis.broker.distribute.test

import ru.yandex.vertis.broker.distribute.Job
import ru.yandex.vertis.broker.distribute.strategy.splitters.JobSplitter
import vertis.core.model.{DataCenter, DataCenters}

/**
  */
case class TestJob(
    name: String,
    override val dataCenters: Set[DataCenter] = DataCenters.logbrokerDcs,
    someConfig: String = "")
  extends Job {
  override val id: String = name + dataCenters.map(_.toString).mkString("(", ",", ")")
}

object TestJob {

  implicit val testJobSplitter: JobSplitter[TestJob] = new JobSplitter[TestJob] {

    override def subTasks(dataCenters: Set[DataCenter])(job: TestJob): Seq[TestJob] =
      Seq(
        job.copy(
          dataCenters = dataCenters
        )
      )
  }
}
