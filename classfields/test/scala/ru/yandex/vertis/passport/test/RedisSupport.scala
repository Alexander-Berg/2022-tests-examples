package ru.yandex.vertis.passport.test

import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.masterreplica.MasterReplica
import io.lettuce.core.{RedisClient, RedisURI}
import org.scalatest.TestSuite
import redis.embedded.RedisCluster
import ru.yandex.vertis.passport.redis.{DefaultRedisCache, RedisCache}

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._
import scala.util.Random

trait RedisSupport extends TestSuite {
  import RedisSupport._

  implicit protected val ec: ExecutionContext = ExecutionContext.global
  protected val cluster: RedisCluster = makeRedisCluster

  def createCache(namespace: String)(implicit ec: ExecutionContext): RedisCache = {
    val redisUriBuilder = RedisURI
      .builder()
      .withSentinelMasterId("master1")
      .withSentinel("localhost", sentinelPort)

    val client = RedisClient.create()
    val connection = MasterReplica.connect(client, ByteArrayCodec.INSTANCE, redisUriBuilder.build())
    val cache = new DefaultRedisCache(namespace, connection)
    cache
  }
}

object RedisSupport {
  private val SentinelPortFrom = 26700
  private val RedisServer1PortFrom = 26800
  private val RedisServer2PortFrom = 26900
  private val PortsRange = 100

  private val randomPort = new Random().nextInt(PortsRange)
  private val sentinelPort = SentinelPortFrom + randomPort
  private val redisServer1Port = RedisServer1PortFrom + randomPort
  private val redisServer2Port = RedisServer2PortFrom + randomPort

  def makeRedisCluster: RedisCluster = {
    val sentinels = List(Integer.valueOf(sentinelPort)).asJava
    val servers = List(Integer.valueOf(redisServer1Port), Integer.valueOf(redisServer2Port)).asJava
    RedisCluster.builder
      .sentinelPorts(sentinels)
      .quorumSize(1)
      .serverPorts(servers)
      .replicationGroup("master1", 1)
      .build
  }

}
