package vertis.yt.zio.reactor

import common.clients.reactor.ReactorClient
import common.zio.logging.Logging
import ru.yandex.inside.yt.kosher.cypress.YPath
import vertis.yt.zio.reactor.ReactorInstanceFinder.byUserDayAndYtPath
import vertis.yt.zio.reactor.ReactorSynchronizerSource.YtReactorSynchronizerSource
import vertis.yt.zio.reactor.instances.ReactorInstance
import vertis.zio.BaseEnv
import zio.RIO

object TestReactorSynchronizerSource {

  def createYt[T](
      reactorClient: ReactorClient.Service,
      makeInstance: YPath => ReactorInstance[T],
      log: Logging.Service,
      findPrevInstance: ReactorInstanceFinder = byUserDayAndYtPath): ReactorSynchronizerSource[BaseEnv, YPath, T] = {
    new YtReactorSynchronizerSource[T] {
      override def get(k: YPath, reactorPath: String): RIO[BaseEnv, ReactorSynchronizer[BaseEnv, T]] = {
        implicit val instance: ReactorInstance[T] = makeInstance(k)
        TestReactorSynchronizer.create(
          reactorClient,
          reactorPath,
          log,
          findPrevInstance
        )
      }
    }
  }

}
