package bootstrap.testcontainers.ydb

import com.dimafeng.testcontainers.SingleContainer
import com.yandex.ydb.core.grpc.GrpcTransport
import com.yandex.ydb.table.TableClient
import com.yandex.ydb.table.rpc.grpc.GrpcTableRpc
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import zio.*

case class YdbContainer private[ydb] (container: YdbContainer.Impl)
    extends SingleContainer[YdbContainer.Impl] {

  def endpoint: UIO[String] =
    ZIO.succeed(s"$containerIpAddress:${mappedPort(YdbContainer.Port)}")

  lazy val transport: GrpcTransport =
    GrpcTransport.forHost(containerIpAddress, mappedPort(2135)).build

  lazy val rpc: GrpcTableRpc        = GrpcTableRpc.ownTransport(transport)
  lazy val tableClient: TableClient = TableClient.newClient(rpc).build()
}

case object YdbContainer {

  val stable: RLayer[Scope, YdbContainer] = live(YdbImage.Stable)
  val latest: RLayer[Scope, YdbContainer] = live(YdbImage.Latest)

  private[YdbContainer] val Port = 2135

  def live(image: YdbImage): RLayer[Scope, YdbContainer] =
    ZLayer.fromZIO {
      ZIO
        .fromAutoCloseable(
          ZIO.succeed(new Impl(image.name)).tap(c => ZIO.attempt(c.start())),
        )
        .map(YdbContainer(_))
    }

  class Impl(image: String) extends GenericContainer[Impl](image) {
    withExposedPorts(Port)
    waitingFor(Wait.forHealthcheck())
  }

}
