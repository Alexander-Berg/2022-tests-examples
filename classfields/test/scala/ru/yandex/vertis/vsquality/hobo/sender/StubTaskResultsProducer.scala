package ru.yandex.vertis.vsquality.hobo.sender

import akka.kafka.ProducerSettings
import ru.yandex.vertis.vsquality.hobo.kafka.taskresults.TaskResultsProducer
import ru.yandex.vertis.vsquality.hobo.model.{Task, TaskKey}

import scala.concurrent.{ExecutionContext, Future}

class StubTaskResultsProducer(
    override val producerSettings: ProducerSettings[TaskKey, Task]
  )(implicit override protected val ec: ExecutionContext)
  extends TaskResultsProducer(producerSettings) {

  override def append(value: Task): Future[Unit] = Future.successful(())
}
