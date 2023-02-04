package bootstrap.testcontainers.zookeeper

import bootstrap.testcontainers.Container
import org.testcontainers.utility.DockerImageName
import zio.*

case class ZKContainer private[zookeeper] (container: ZKTestContainer)
    extends Container[ZKTestContainer] {
  def connectString: UIO[String] = ZIO.succeed(container.getConnectString)
}

case object ZKContainer {
  val Image: DockerImageName = DockerImageName.parse("zookeeper:3.6")

  val live: RLayer[Scope, ZKContainer] = ZLayer.fromZIO {
    ZIO
      .acquireRelease(
        ZIO.attempt(new ZKTestContainer()).tap(c => ZIO.attempt(c.start())),
      )(c => ZIO.attempt(c.stop()).orDie)
      .map(ZKContainer(_))
  }

  def connectString: URIO[ZKContainer, String] =
    ZIO.environmentWithZIO[ZKContainer](_.get.connectString)

}
