package vasgen.core.saas.mock

object FactorKeeperMock
    extends Mock[FactorKeeper.Service[TestSetup]]
       with Logging {

  override val compose: URLayer[mock.Proxy, FactorKeeper.Service[TestSetup]] =
    ZLayer.fromServiceM { proxy =>
      ZIO.succeed {
        new FactorKeeper.Service[TestSetup] {
          override def start
            : URIO[Clock, Fiber.Runtime[VasgenStatus, Nothing]] = ZIO.never.fork

          override def getFactors(
            tpe: Factor.Type,
          ): IO[VasgenStatus, Seq[Factor]] = proxy(GetFactors, tpe)
        }
      }
    }

  object GetFactors extends Effect[Factor.Type, VasgenStatus, Seq[Factor]]

}
