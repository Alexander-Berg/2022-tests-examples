package common.clients.hobo.testkit

import common.clients.hobo.HoboClient.HoboClient
import common.clients.hobo.{CreateHoboTaskOptions, HoboClient}
import ru.yandex.vertis.hobo.proto.model.{QueueId, Task => HoboTask, TaskSource}
import zio._

object TestHoboClient {

  case class Stub() extends HoboClient.Service {

    override def createTask(queueId: QueueId, taskSource: TaskSource, options: CreateHoboTaskOptions): Task[HoboTask] =
      ZIO.succeed(
        HoboTask(
          version = 1
        )
      )
  }

  val layer: ULayer[HoboClient] = ZLayer.succeed(Stub())
}
