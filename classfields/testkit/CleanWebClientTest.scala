package ru.yandex.vertis.general.common.clients.clean_web.testkit

import io.circe.Encoder
import ru.yandex.vertis.general.common.clients.clean_web.CleanWebClient
import ru.yandex.vertis.general.common.clients.clean_web.CleanWebClient._
import zio.{Ref, Task, ULayer, ZLayer}

object CleanWebClientTest extends CleanWebClient.Service {

  override def process[T](request: ProcessRequest[T])(implicit encoder: Encoder[T]): Task[ProcessResult] =
    Task.succeed(ProcessResult(Seq.empty))

  val live: ULayer[CleanWebClient] = ZLayer.succeed(CleanWebClientTest)
}
