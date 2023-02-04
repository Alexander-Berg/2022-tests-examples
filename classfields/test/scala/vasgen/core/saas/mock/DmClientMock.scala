package vasgen.core.saas.mock

object DmClientMock extends Mock[DmClient.Service] {

  override val compose: URLayer[mock.Proxy, DmClient.Service] = ZLayer
    .fromServiceM { proxy =>
      withRuntime.map { rts =>
        new DmClient.Service {
          override def clusterService: ServiceName =
            rts.unsafeRunZIO.from(proxy(ClusterService))

          override def getVersion(
            serviceType: ServiceType,
            service: ServiceName,
            name: String,
          ): Task[Option[Int]] = proxy(GetVersion, serviceType, service, name)

          override def getConfig(
            serviceType: ServiceType,
            service: ServiceName,
            name: String,
          ): Task[ConfigData] = proxy(GetConfig, serviceType, service, name)
        }

      }

    }

  object ClusterService extends Method[Unit, Nothing, ServiceName]

  object GetVersion
      extends Effect[(ServiceType, ServiceName, String), Nothing, Option[Int]]

  object GetConfig
      extends Effect[(ServiceType, ServiceName, String), Nothing, ConfigData]

}
