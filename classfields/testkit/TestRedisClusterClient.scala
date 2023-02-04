package common.zio.redis.cluster.testkit

import com.dimafeng.testcontainers.GenericContainer
import common.zio.redis.cluster.RedisClusterClient.RedisClusterClient
import common.zio.redis.cluster.{RedisClusterClient, RedisClusterConfig}
import common.zio.redis.cluster.RedisClusterConfig.ClusterNode
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import zio.blocking.Blocking
import zio.{Has, ZIO, ZLayer}

import scala.concurrent.duration._

object TestRedisClusterClient {

  private val exposedPorts = List(7000, 7001, 7002)

  val managedRedisContainer: ZLayer[Any, Nothing, Has[GenericContainer]] =
    ZLayer
      .fromAcquireRelease {
        ZIO
          .effectTotal(
            GenericContainer(
              "grokzen/redis-cluster:5.0.9",
              exposedPorts = exposedPorts,
              env = Map("MASTERS" -> "3", "SLAVES_PER_MASTER" -> "0"),
              waitStrategy = new LogMessageWaitStrategy().withRegEx(".*Cluster state changed: ok.*").withTimes(3)
            )
          )
          .tap(c => ZIO.effect(c.start()).orDie)
      }(c => ZIO.effect(c.stop()).orDie)

  val test: ZLayer[Blocking, Nothing, RedisClusterClient] =
    ZLayer.requires[Blocking] ++
      managedRedisContainer.map { container =>
        val host = container.get.host
        val ports = exposedPorts.map(container.get.mappedPort)
        val nodes = ports.map(port => ClusterNode(host, port))
        Has(RedisClusterConfig(nodes, None, 2.seconds, 2.seconds, 5, 1))
      } >>> RedisClusterClient.live
}
