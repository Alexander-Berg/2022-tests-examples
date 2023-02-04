package vasgen.core.saas.mock

object FactorStorageMock extends Mock[FactorStorage.Service[TestSetup]] {

  override val compose: URLayer[mock.Proxy, FactorStorage.Service[TestSetup]] =
    ZLayer.fromServiceM { proxy =>
      ZIO.succeed {
        new FactorStorage.Service[TestSetup] {
          override def store(
            version: Int,
            factors: Seq[Factor],
          ): ZIO[Clock, VasgenStatus, Unit] = proxy(Store, version, factors)

          override def retrieve(
            version: Int,
          ): ZIO[Clock, VasgenStatus, List[Factor]] = proxy(Retrieve, version)

          override def createTable: UIO[Unit] = proxy(CreateTable)
        }
      }
    }

  object Store extends Effect[(Int, Seq[Factor]), VasgenStatus, Unit]

  object Retrieve extends Effect[Int, VasgenStatus, List[Factor]]

  object CreateTable extends Effect[Unit, Nothing, Unit]

}
