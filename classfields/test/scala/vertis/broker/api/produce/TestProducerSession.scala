package vertis.broker.api.produce

import ru.yandex.vertis.broker.requests.WriteRequest.WriteData
import vertis.broker.api.model.ProduceError
import vertis.zio.BaseEnv
import zio.{UIO, ZIO}

/** @author zvez
  */
object TestProducerSession {

  def make: UIO[ProducerSession] = UIO {
    new ProducerSession {
      override def write(request: WriteData): ZIO[BaseEnv, ProduceError, Unit] = ZIO.unit

      override def close: UIO[Unit] = UIO.unit
    }
  }

}
