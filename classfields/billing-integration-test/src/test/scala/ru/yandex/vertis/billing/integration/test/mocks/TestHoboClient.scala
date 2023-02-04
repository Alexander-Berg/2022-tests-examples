package ru.yandex.vertis.billing.integration.test.mocks

import ru.yandex.vertis.hobo.client.{CreateOptions, HoboClient}

import scala.concurrent.Future
import ru.yandex.vertis.hobo.proto.Model
import scala.jdk.CollectionConverters._

class TestHoboClient extends HoboClient {

  private var callIds = Set.empty[String]

  override def createTask(
      queueId: Model.QueueId,
      taskSource: Model.TaskSource,
      options: CreateOptions): Future[Model.Task] = synchronized {
    // id имеет формат 'call_id:complain_flag'
    val addedCallIds = taskSource.getPayload.getExternal.getIdsList.asScala.map(_.split(':').head).toSet
    callIds = callIds ++ addedCallIds
    Future.successful(Model.Task.getDefaultInstance)
  }

  override def cancelTask(queueId: Model.QueueId, key: String): Future[Unit] = ???

  def getCallIds: Set[String] = synchronized {
    callIds
  }
}
