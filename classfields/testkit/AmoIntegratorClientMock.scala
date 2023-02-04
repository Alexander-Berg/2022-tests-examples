package auto.dealers.amoyak.storage.testkit

import io.circe.Encoder
import auto.dealers.amoyak.model.{AmoJson, AmoMessageType}
import auto.dealers.amoyak.storage.clients.AmoIntegratorClient
import auto.dealers.amoyak.storage.clients.AmoIntegratorClient.AmoIntegratorClient
import zio.{Has, Tag, Task, URLayer, ZIO, ZLayer}
import zio.test.mock._

object AmoIntegratorClientMock extends Mock[AmoIntegratorClient] {
  object PushMessage extends Poly.Effect.Input[Throwable, Unit]
  object PushFinance extends Poly.Effect.Input[Throwable, Unit]

  override val compose: URLayer[Has[Proxy], AmoIntegratorClient] = ZLayer.fromServiceM { proxy =>
    withRuntime.map { rts =>
      new AmoIntegratorClient.Service {

        override def pushMessage[T: Encoder: Tag](message: AmoJson[T] with AmoMessageType): Task[Unit] =
          proxy(PushMessage.of[AmoJson[T] with AmoMessageType], message)

        override def pushFinance[T: Encoder: Tag](message: AmoJson[T] with AmoMessageType): Task[Unit] =
          proxy(PushFinance.of[AmoJson[T] with AmoMessageType], message)
      }
    }
  }

  val empty0 = ZLayer.succeed(new AmoIntegratorClient.Service {
    override def pushMessage[T: Encoder: Tag](message: AmoJson[T] with AmoMessageType): Task[Unit] = ZIO.unit

    override def pushFinance[T: Encoder: Tag](message: AmoJson[T] with AmoMessageType): Task[Unit] = ZIO.unit
  })
}
