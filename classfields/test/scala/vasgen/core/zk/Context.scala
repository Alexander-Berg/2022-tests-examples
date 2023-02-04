package vasgen.core.zk

import bootstrap.logging.Logging
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.BoundedExponentialBackoffRetry
import vasgen.core.saas.mock.TestSetup
import zio._

object Context extends Logging {
  private val node = "/test/stateKeeper"

  def curatorFrameworkLayer(
    connectionString: String,
  ): ZLayer[Any, Nothing, CuratorFrameworkLayer] =
    log
      .info(s"Curator instance started")
      .as {
        val curator = CuratorFrameworkFactory
          .builder()
          .namespace("test")
          .connectString(connectionString)
          .retryPolicy(new BoundedExponentialBackoffRetry(10000, 60000, 29))
          .build()
        curator.start()
        curator
      }
      .toLayer

  private val keeper: ZLayer[CuratorFramework, Nothing, StateKeeper.Service[TestSetup, Int]] = ZLayer.fromService((client: CuratorFramework) =>
    StateKeeper[TestSetup, Int](
      node,
      client,
      s => s.toIntOption,
      i => i.toString,
    ),
  )

  private val listener: ZLayer[CuratorFramework, Nothing, StateKeeper.Listener[TestSetup, Int]] = ZLayer.fromService((client: CuratorFramework) =>
    StateListener[TestSetup, Int](
      node,
      client,
      Runtime.default,
      s => s.toIntOption,
    ),
  )

  def instanceLayer(connectionString: String): ULayer[
    StateKeeper.Service[TestSetup, Int] &
      StateKeeper.Listener[TestSetup, Int],
  ] = curatorFrameworkLayer(connectionString) >>> (keeper ++ listener)

}
