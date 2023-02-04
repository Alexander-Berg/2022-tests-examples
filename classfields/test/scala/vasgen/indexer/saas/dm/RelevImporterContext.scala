package vasgen.indexer.saas.dm

import saas.dm.client.DmClient
import vasgen.core.saas.db.FactorStorage
import vasgen.core.saas.mock.{TestSetup, ZkSemaphoreStub}
import vasgen.core.zk.StateKeeper
import zio.{Has, ZLayer}
import zio.clock.Clock

import scala.concurrent.duration.Duration

object RelevImporterContext {
  val relevConf: String = "relev.conf"

  val live: ZLayer[Clock with Has[DmClient.Service] with Has[
    StateKeeper.Service[TestSetup, Int],
  ] with Has[FactorStorage.Service[TestSetup]], Nothing, Has[
    RelevImporter[TestSetup, TestSetup],
  ]] = ZLayer.fromServices[DmClient.Service, StateKeeper.Service[
    TestSetup,
    Int,
  ], FactorStorage.Service[TestSetup], RelevImporter[TestSetup, TestSetup]](
    (dmClient, keeper, storage) =>
      RelevImporter(
        DmConfigLoader.Config(relevConf, Duration.Inf),
        dmClient,
        keeper,
        storage,
        ZkSemaphoreStub,
      ),
  )

}
