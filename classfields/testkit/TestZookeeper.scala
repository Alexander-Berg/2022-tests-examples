package common.zookeeper.testkit

import common.zookeeper.Zookeeper
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.BoundedExponentialBackoffRetry
import org.apache.curator.test.TestingCluster
import zio.{Has, UIO, URIO, ZIO, ZLayer}

import scala.concurrent.duration._

object TestZookeeper {

  val testCluster: ZLayer[Any, Nothing, Has[TestingCluster]] = {
    ZLayer
      .fromAcquireRelease {
        ZIO
          .effectTotal(new TestingCluster(1))
          .tap(cluster => ZIO.effect(cluster.start()).orDie)
      }(release = cluster => ZIO.effect(cluster.stop()).orDie)
  }

  val curatorFramework: ZLayer[Has[TestingCluster], Nothing, Has[CuratorFramework]] = {
    ZLayer.fromAcquireRelease {
      for {
        zookeeper <- ZIO.service[TestingCluster]
        zkClient <- makeZkClient(zookeeper).tap(zkClient => ZIO.effect(zkClient.start()).orDie)
      } yield zkClient
    }(release = zkClient => ZIO.effect(zkClient.close()).orDie)
  }

  val live: ZLayer[Any, Nothing, Has[Zookeeper.Service]] = {
    ZLayer.fromEffect {
      ZIO
        .effectSuspendTotal {
          for {
            zkClient <- ZIO.service[CuratorFramework]
          } yield new Zookeeper.Service {
            override def zkClientForGroup: URIO[Any, CuratorFramework] = UIO(zkClient)

            override def zkClientForService: URIO[Any, CuratorFramework] = UIO(zkClient)
          }
        }
        .provideLayer {
          testCluster >+> curatorFramework
        }
    }
  }

  private def makeZkClient(zookeeper: TestingCluster): UIO[CuratorFramework] = {
    ZIO
      .effectTotal {
        val retryPolicy = new BoundedExponentialBackoffRetry(10000, 60000, 29)
        CuratorFrameworkFactory
          .builder()
          .namespace("test")
          .connectString(zookeeper.getConnectString)
          .sessionTimeoutMs(15.seconds.toMillis.toInt)
          .connectionTimeoutMs(5.seconds.toMillis.toInt)
          .retryPolicy(retryPolicy)
          .build()
      }
  }
}
