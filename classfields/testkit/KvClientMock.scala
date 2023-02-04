package common.zio.clients.kv.testkit

import common.zio.clients.kv.KvClient.{Decoder, Encoder, KvError}
import common.zio.clients.kv.KvClient
import zio._
import zio.test.mock._

object KvClientMock extends Mock[Has[KvClient.Service]] {
  object PolyOutput extends Poly.Effect.Output[String, KvError]
  object PolyInput extends Poly.Effect.Input[KvError, Unit]

  override val compose: URLayer[Has[Proxy], Has[KvClient.Service]] =
    ZLayer.fromServiceM { proxy =>
      withRuntime.as {
        new KvClient.Service {
          def get[V: Tag](key: String)(implicit valueDecoder: Decoder[V]): IO[KvError, V] =
            proxy(PolyOutput.of[V], key)

          def set[V: Tag](key: String, value: V)(implicit valueEncoder: Encoder[V]): Task[Unit] =
            proxy(PolyInput.of[V], value)
        }
      }
    }
}
