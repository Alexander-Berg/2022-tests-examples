package vasgen.indexer.saas.util

object RtyStateReaderMock
    extends Mock[Has[StateKeeper.Reader[TestSetup, RtyServerState]]] {

  override val compose: URLayer[Has[mock.Proxy], Has[
    StateKeeper.Reader[TestSetup, RtyServerState],
  ]] = ZLayer.fromServiceM { proxy =>
    ZIO.succeed {
      new StateKeeper.Reader[TestSetup, RtyServerState] {
        override def current: IO[VasgenStatus, RtyServerState] = proxy(Current)
      }
    }
  }

  object Current extends Effect[Unit, VasgenStatus, RtyServerState]

}
