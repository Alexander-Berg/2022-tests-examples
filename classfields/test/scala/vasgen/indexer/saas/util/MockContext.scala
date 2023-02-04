package vasgen.indexer.saas.util

object MockContext {

  def dmClient: ZLayer[Any, Nothing, Has[DmClient.Service]] =
    ZIO.succeed(new MockDmClient).toLayer

  def tvm: ZLayer[Any, Nothing, Has[TVM.Service]] = ZIO.succeed(MockTVM).toLayer

}

class MockDmClient extends DmClient.Service {

  override def clusterService: ServiceName = ServiceName("")

  override def getConfig(
    serviceType: ServiceType,
    service: ServiceName,
    name: String,
  ): Task[ConfigData] = ZIO.succeed(ConfigData("", None))

  override def getVersion(
    serviceType: ServiceType,
    service: ServiceName,
    name: String,
  ): Task[Option[Int]] = ZIO.none

}
