package ru.auto.api.app.redis

import java.util.concurrent.ForkJoinPool

import io.lettuce.core.RedisURI
import io.lettuce.core.masterreplica.MasterReplica
import org.joda.time.DateTimeUtils
import org.scalatest.BeforeAndAfterAll
import ru.auto.api.app.redis.RedisCodecs.IntBinaryCodec
import ru.yandex.vertis.tracing.Traced

import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}
import io.lettuce.core.RedisClient
import io.lettuce.core.codec.ByteArrayCodec
import org.scalatest.funsuite.AnyFunSuite
import redis.embedded.RedisCluster

import scala.jdk.CollectionConverters._
import scala.util.Random

class DefaultRedisCacheIntTest extends AnyFunSuite with BeforeAndAfterAll {
  import DefaultRedisCacheIntTest._

  private val randomPort = new Random().nextInt(PortsRange)
  private val sentinelPort = SentinelPortFrom + randomPort
  private val redisServer1Port = RedisServer1PortFrom + randomPort
  private val redisServer2Port = RedisServer2PortFrom + randomPort

  private val cluster: RedisCluster = {
    val sentinels = List(Integer.valueOf(sentinelPort)).asJava
    val servers = List(Integer.valueOf(redisServer1Port), Integer.valueOf(redisServer2Port)).asJava
    RedisCluster.builder
      .sentinelPorts(sentinels)
      .quorumSize(1)
      .serverPorts(servers)
      .replicationGroup("master1", 1)
      .build
  }

  override protected def beforeAll(): Unit = {
    cluster.start()
  }

  override protected def afterAll(): Unit = {
    cluster.stop()
  }

  test("compare and set test") {

    implicit val t: Traced = Traced.empty
    implicit val redisPool: ExecutionContextExecutor = ExecutionContext.fromExecutor(new ForkJoinPool(16))
    val namespace = "cas-test"
    val key = DateTimeUtils.currentTimeMillis().toString

    val redisUriBuilder = RedisURI
      .builder()
      .withSentinelMasterId("master1")
      .withSentinel("localhost", sentinelPort)

    val client = RedisClient.create()
    val connection = MasterReplica.connect(client, ByteArrayCodec.INSTANCE, redisUriBuilder.build())
    val cache = new DefaultRedisCache(namespace, connection)

    Await.result(cache.set(key, 0, 10.minutes), 10.seconds)

    @tailrec
    def inc(thread: Int): Unit = {
      val f = cache.get(key).flatMap {
        case Some(v) => cache.compareAndSet(key, Some(v), v + 1, 10.minutes)
        case None => cache.compareAndSet(key, None, 1, 10.minutes)
      }
      val result = Await.result(f, 10.seconds)
      if (!result) {
        inc(thread)
      }
    }

    val start = System.currentTimeMillis() + 1000

    val threads = (1 to 10).map { thread =>
      val t = new Thread(() => {
        if (System.currentTimeMillis() < start) Thread.sleep(start - System.currentTimeMillis())
        (1 to 10).foreach { _ =>
          inc(thread)
        }
      })
      t.start()
      t
    }
    threads.foreach(_.join())

    assert(Await.result(cache.get(key), 10.seconds).contains(100))
  }
}

object DefaultRedisCacheIntTest {
  val SentinelPortFrom = 26700
  val RedisServer1PortFrom = 26800
  val RedisServer2PortFrom = 26900
  val PortsRange = 100
}
