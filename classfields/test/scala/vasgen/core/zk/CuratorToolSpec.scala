package vasgen.core.zk

import scala.util.Try

object CuratorToolSpec extends ZIOSpecDefault with Logging {

  override def spec =
    suite("CuratorTool")(
      test("Get data from non-existent  node") {
        for {
          tool <- ZIO.service[Tool]
          _    <- ZIO.succeed(tool.getData("non-existent"))
        } yield assert(())(isUnit)
      },
      test("Delete non-existent  node") {
        for {
          tool <- ZIO.service[Tool]
          _    <- ZIO.succeed(tool.deleteNode("non-existent"))
        } yield assert(())(isUnit)
      },
    ).provideLayerShared(layer)

  def layer: ZLayer[Any, Nothing, Tool] =
    ZManaged
      .acquireReleaseWith(
        for {
          server: TestingServer <- ZIO.succeed(new TestingServer())
        } yield Tool(
          CuratorFrameworkFactory
            .builder()
            .namespace("test")
            .connectString(server.getConnectString)
            .retryPolicy(new BoundedExponentialBackoffRetry(10000, 60000, 29))
            .build(),
          server,
        ),
      )(tool => ZIO.succeed(tool.server.stop()))
      .toLayer

  case class Tool(curator: CuratorFramework, server: TestingServer)
      extends CuratorTool {

    override def getData(path: String): Try[String] = super.getData(path)

    override def deleteNode(path: String): Unit = super.deleteNode(path)
  }

}
