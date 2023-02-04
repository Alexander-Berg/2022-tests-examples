package bootstrap.ydb

import bootstrap.config.Source
import bootstrap.metrics.Registry
import bootstrap.testcontainers.ydb.YdbContainer
import bootstrap.tracing.$
import bootstrap.ydb.YDB.Config
import zio.*

case object YdbTest {

  def fromContainer[S <: Source[Config] : Tag](setup: S): ZLayer[
    YdbContainer & Scope & Registry & Clock & $ & setup.R,
    setup.E,
    YDB[S],
  ] = {
    ZLayer.fromZIO {
      for {
        config    <- setup.read
        container <- ZIO.service[YdbContainer]
        ydb <- YDB.fromClient[S](
          config.copy(database = container.transport.getDatabase),
          container.tableClient,
        )
      } yield ydb
    }
  }

}
