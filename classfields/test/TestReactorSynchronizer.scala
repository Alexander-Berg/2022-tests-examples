package vertis.yt.zio.reactor

import common.clients.reactor.ReactorClient
import common.zio.logging.Logging
import vertis.yt.storage.AtomicStorageTestImpl
import vertis.yt.zio.reactor.ReactorInstanceFinder.byUserDayAndYtPath
import vertis.yt.zio.reactor.ReactorSynchronizer.ReactorSynchronizerConfig
import vertis.yt.zio.reactor.instances.ReactorInstance
import vertis.zio.BaseEnv
import zio.UIO

object TestReactorSynchronizer {

  /** Creates almost real [[ReactorSynchronizer]]. The only testing difference - in-memory [[AtomicStorageTestImpl]]
    */
  def create[T: ReactorInstance](
      reactorClient: ReactorClient.Service,
      reactorPath: String,
      log: Logging.Service,
      findPrevInstance: ReactorInstanceFinder = byUserDayAndYtPath,
      config: ReactorSynchronizerConfig = testConfig): UIO[ReactorSynchronizerImpl[BaseEnv, T]] = {
    AtomicStorageTestImpl.create[Seq[T]](Nil).map { storage =>
      new ReactorSynchronizerImpl[BaseEnv, T](
        storage,
        reactorClient,
        reactorPath,
        findPrevInstance,
        config,
        log
      )
    }

  }

  val testConfig: ReactorSynchronizerConfig = ReactorSynchronizerConfig(ttlDays = 30, projectId = 1)
}
