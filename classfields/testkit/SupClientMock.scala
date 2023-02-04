package ru.yandex.vertis.general.common.clients.sup.testkit

import io.circe.Json
import ru.yandex.vertis.general.clients.sup.SupClient
import ru.yandex.vertis.general.clients.sup.SupClient.SupClient
import ru.yandex.vertis.general.common.clients.sup.model.SupPush
import zio.macros.accessible
import zio.{Has, Ref, Task, UIO, ULayer, ZIO, ZRef}

@accessible
object SupClientMock {

  type SupClientMock = Has[Service]

  trait Service {
    def setPushResponse(response: (SupPush, Boolean) => Task[String]): UIO[Unit]
  }

  val layer: ULayer[SupClient with SupClientMock] = {
    val creationEffect = for {
      responseRef <- ZRef.make[(SupPush, Boolean) => Task[String]]((_, _) => ZIO.succeed(""))

      responseSetter: Service = new ServiceImpl(responseRef)
      supClientTest: SupClient.Service = new SupClientTestImpl(responseRef)

    } yield Has.allOf(responseSetter, supClientTest)
    creationEffect.toLayerMany
  }

  private class ServiceImpl(responseRef: Ref[(SupPush, Boolean) => Task[String]]) extends Service {

    override def setPushResponse(response: (SupPush, Boolean) => Task[String]): UIO[Unit] =
      responseRef.set(response)
  }

  private class SupClientTestImpl(
      responseRes: Ref[(SupPush, Boolean) => Task[String]])
    extends SupClient.Service {

    override def sendPush(push: SupPush, dryRun: Boolean): Task[String] =
      responseRes.get.map(f => f(push, dryRun)).flatten
  }
}
