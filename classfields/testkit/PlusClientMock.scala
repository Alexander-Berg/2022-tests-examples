package common.clients.plus.testkit

import common.clients.plus.{CreateRequest, PlusClient, Result}
import common.zio.sttp.model.SttpError
import zio.{IO, ZIO, ZLayer}

class PlusClientMock extends PlusClient.Service {

  override def create(request: CreateRequest): IO[SttpError, Result] =
    ZIO.succeed(Result("SCHEDULED", None))

  override def status(transactionId: String): IO[SttpError, Result] =
    ZIO.succeed(Result("RUNNING", None))

}

object PlusClientMock {
  val live = ZLayer.succeed[PlusClient.Service](new PlusClientMock)
}
