package vasgen.core.saas.mock

object StateKeeperMock extends Mock[StateKeeper.Service[TestSetup, Int]] {

  override val compose
    : URLayer[mock.Proxy, StateKeeper.Service[TestSetup, Int]] = ZLayer
    .fromServiceM { proxy =>
      ZIO.succeed {
        new StateKeeper.Service[TestSetup, Int] {
          override def current: IO[VasgenStatus, Int] = proxy(Current)

          override def set(item: Int): IO[VasgenStatus, Unit] = proxy(Set, item)
        }
      }
    }

  object Current extends Effect[Unit, VasgenStatus, Int]

  object Set extends Effect[Int, VasgenStatus, Unit]

}
