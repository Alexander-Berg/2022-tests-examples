package common.clients.grafana.testkit

import common.clients.grafana.GrafanaClient
import zio.{Has, Task, ULayer, ZLayer}

import java.time.Instant

class TestGrafanaClient extends GrafanaClient.Service {

  override def createAnnotation(time: Instant, timeEnd: Instant, tags: Seq[String], text: String): Task[Long] =
    Task.succeed(1)
}

object TestGrafanaClient {
  val test: ULayer[GrafanaClient.GrafanaClient] = ZLayer.succeed(new TestGrafanaClient)
}
