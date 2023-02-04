package vs.core.distribution

import bootstrap.test.TestBase
import bootstrap.tracing.*
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.BoundedExponentialBackoffRetry
import zio.*

object DistributionContext {

  val config = DistributionWorker.Config(
    domain = "test",
    application = "test",
    profile = "",
    inputSuffix = "input",
    childPrefix = "distribution",
    outputSuffix = "output",
    partitions = 8,
  )

  def instanceLayer(
    connectionString: String,
    id: String,
  ): ZLayer[Any, Throwable, CuratorFramework & ShardDistributionNotifier] =
    curatorFrameworkLayer(connectionString) >+>
      (TestBase.bootstrap >>> distributionServiceLayer(id))

  private def curatorFrameworkLayer(
    connectionString: String,
  ): ZLayer[Any, Throwable, CuratorFramework] =
    ZLayer.fromZIO(
      ZIO.succeed {
        val curator = CuratorFrameworkFactory
          .builder()
          .namespace("test")
          .connectString(connectionString)
          .retryPolicy(new BoundedExponentialBackoffRetry(10000, 60000, 29))
          .build()
        curator.start()
        curator
      },
    )

  private def distributionServiceLayer(
    id: String,
  ): ZLayer[$ & CuratorFramework, Throwable, ShardDistributionNotifier] =
    ZLayer.fromZIO(
      for {
        curator   <- ZIO.service[CuratorFramework]
        rts       <- ZIO.runtime[Any]
        log       <- ZIO.service[$]
        queue     <- Queue.unbounded[Map[InstanceId, Seq[Int]]]
        ref       <- Ref.make(Map.empty[Int, Seq[Int] => RIO[$, Unit]])
        instances <- Ref.make(Map.empty[InstanceId, Seq[Int]])
      } yield new DistributionWorker(
        config = config,
        rts = rts,
        queue = queue,
        mapRef = instances,
        instanceId = InstanceId(id),
      )(curator = curator, subscriptions = ref): ShardDistributionNotifier,
    )

}
