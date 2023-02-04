package ru.yandex.vertis.passport.redis

import java.util.concurrent.ForkJoinPool
import io.lettuce.core.RedisURI
import io.lettuce.core.masterreplica.MasterReplica
import org.joda.time.DateTimeUtils
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import ru.yandex.vertis.passport.redis.RedisCodecs.IntBinaryCodec
import ru.yandex.vertis.tracing.Traced

import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}
import io.lettuce.core.RedisClient
import io.lettuce.core.codec.ByteArrayCodec
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import redis.embedded.RedisCluster

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import scala.collection.JavaConverters._
import scala.util.Random

class DefaultRedisCacheIntTest extends FunSuite with BeforeAndAfterAll with Matchers {
  import DefaultRedisCacheIntTest._

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
    implicit val redisPool: ExecutionContextExecutor = ExecutionContext.fromExecutor(new ForkJoinPool(10))
    val key = UUID.randomUUID().toString
    val cache = createCache("cas-test")

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

  test("compare and set ttl test") {

    implicit val t: Traced = Traced.empty
    implicit val redisPool: ExecutionContextExecutor = ExecutionContext.fromExecutor(new ForkJoinPool(1))
    val key = UUID.randomUUID().toString
    val cache = createCache("cas-test")

    @tailrec
    def inc(thread: Int): Unit = {
      val f = cache.get(key).flatMap {
        case Some(v) => cache.compareAndSet(key, Some(v), v + 1, 10.seconds)
        case None => cache.compareAndSet(key, None, 1, 10.seconds)
      }
      val result = Await.result(f, 10.seconds)
      if (!result) {
        inc(thread)
      }
    }

    val thread = new Thread(() => {
      inc(1)
    })

    thread.start()

    Thread.sleep(12000)
    cache.get(key).futureValue shouldBe None
  }

  test("listModify test") {
    implicit val t: Traced = Traced.empty
    implicit val redisPool: ExecutionContextExecutor = ExecutionContext.fromExecutor(new ForkJoinPool(10))
    val key = UUID.randomUUID().toString
    val cache = createCache("lm-test")
    cache.listAdd(key, 0, 2, 4, 6, 8).futureValue
    @tailrec
    def modify(thread: Int, count: Int): Unit = {
      val f = cache.listModify[Int](key) { cur =>

        val res = if (cur.contains(thread) && (thread % 2 == 0)) {
          cur.filterNot(_ == thread)
        } else if ((!cur.contains(thread)) && (thread % 2 == 1)) {
          cur :+ thread
        } else cur
        res
      }
      val result = Await.result(f, 10.seconds)
      if (!result) {
        if (count > 0) {
          modify(thread, count - 1)
        } else throw new RuntimeException("script not working")
      }
    }

    val start = System.currentTimeMillis() + 1000

    val threads = (0 to 9).map { thread =>
      val t = new Thread(() => {
        if (System.currentTimeMillis() < start) Thread.sleep(start - System.currentTimeMillis())
        (0 to 9).foreach { _ =>
          modify(thread, 10)
        }
      })
      t.start()
      t
    }
    threads.foreach(_.join())
    val res = cache.listGet(key).futureValue
    res.toSet shouldBe Set(1, 3, 5, 7, 9)
  }

  test("listModify should create key if it not exists") {
    implicit val t: Traced = Traced.empty
    implicit val redisPool: ExecutionContextExecutor = ExecutionContext.fromExecutor(new ForkJoinPool(10))
    val key = UUID.randomUUID().toString
    val cache = createCache("lm-test")

    @tailrec
    def modify(thread: Int, count: Int): Unit = {
      val f = cache.listModify[Int](key) { cur =>
        val res = if (cur.contains(thread) && (thread % 2 == 0)) {
          cur.filterNot(_ == thread)
        } else if ((!cur.contains(thread)) && (thread % 2 == 1)) {
          cur :+ thread
        } else cur
        res
      }
      val result = Await.result(f, 10.seconds)
      if (!result) {
        if (count > 0) {
          modify(thread, count - 1)
        } else throw new RuntimeException("script not working")
      }
    }

    val start = System.currentTimeMillis() + 1000

    val threads = (0 to 9).map { thread =>
      val t = new Thread(() => {
        if (System.currentTimeMillis() < start) Thread.sleep(start - System.currentTimeMillis())
        (0 to 9).foreach { _ =>
          modify(thread, 10)
        }
      })
      t.start()
      t
    }
    threads.foreach(_.join())
    val res = Await.result(cache.listGet(key), 30.seconds)
    res.toSet shouldBe Set(1, 3, 5, 7, 9)
  }

  test("simple set/get test") {
    implicit val t: Traced = Traced.empty
    implicit val redisPool: ExecutionContextExecutor = ExecutionContext.fromExecutor(new ForkJoinPool(16))
    val key = UUID.randomUUID().toString
    val cache = createCache("simple-test")
    try {
      Await.result(cache.set(key, 1, 20.seconds), 10.seconds)
    } catch {
      case e: Throwable => fail("should not throw", e)
    }
    try {
      val getF = Await.result(cache.get(key), 10.seconds)
      getF shouldBe Some(1)
    } catch {
      case e: Throwable => fail("should not throw", e)
    }
  }

  test("listAdd/listGet test") {
    implicit val t: Traced = Traced.empty
    implicit val redisPool: ExecutionContextExecutor = ExecutionContext.fromExecutor(new ForkJoinPool(16))
    val key = UUID.randomUUID().toString
    val cache = createCache("simple-test")
    cache.listAdd(key, 1, 2, 3).futureValue
    val res = cache.listGet(key).futureValue
    res shouldBe List(1, 2, 3)

  }

  test("getBulk test") {
    implicit val t: Traced = Traced.empty
    implicit val redisPool: ExecutionContextExecutor = ExecutionContext.fromExecutor(new ForkJoinPool(16))
    val keys = (1 to 3).map(_ => UUID.randomUUID().toString)
    val cache = createCache("simple-test")
    for {
      key <- keys
    } yield {
      Await.result(cache.set(key, 1, 30.seconds), 5.seconds)
    }
    val getF = cache.getBulk(keys: _*).futureValue
    getF shouldBe Seq(1, 1, 1)
  }

  test("delete test") {
    implicit val t: Traced = Traced.empty
    implicit val redisPool: ExecutionContextExecutor = ExecutionContext.fromExecutor(new ForkJoinPool(1))
    val keys = (1 to 3).map(_ => UUID.randomUUID().toString)
    val cache = createCache("simple-test")
    for {
      key <- keys
    } yield {
      Await.result(cache.set(key, 1, 20.seconds), 10.seconds)
    }
    cache.delete(keys: _*).futureValue
    val getF = cache.getBulk(keys: _*).futureValue
    getF shouldBe Seq()
  }

  test("expiration test") {
    implicit val t: Traced = Traced.empty
    implicit val redisPool: ExecutionContextExecutor = ExecutionContext.fromExecutor(new ForkJoinPool(1))
    val key = UUID.randomUUID().toString
    val cache = createCache("simple-test")
    val curtime = DateTimeUtils.currentTimeMillis()
    cache.set(key, 1, 10.seconds).futureValue
    Thread.sleep(11000 - (DateTimeUtils.currentTimeMillis - curtime)) // to ensure time has passed
    val getF = cache.get(key).futureValue
    getF shouldBe None
  }

  test("touch test") {
    implicit val t: Traced = Traced.empty
    implicit val redisPool: ExecutionContextExecutor = ExecutionContext.fromExecutor(new ForkJoinPool(1))
    val curtime = DateTimeUtils.currentTimeMillis()
    val key = UUID.randomUUID().toString
    val cache = createCache("simple-test")
    Await.result(cache.set(key, 1, 10.seconds), 5.seconds)
    Await.result(cache.touch(key, Instant.now().plus(5, ChronoUnit.MINUTES)), 5.seconds)
    Thread.sleep(15000) // to ensure time has passed
    val getF = cache.get(key).futureValue
    getF shouldBe Some(1)
  }
}

object DefaultRedisCacheIntTest {

  val SentinelPortFrom = 26700
  val RedisServer1PortFrom = 26800
  val RedisServer2PortFrom = 26900
  val PortsRange = 100

  private val randomPort = new Random().nextInt(PortsRange)
  private val sentinelPort = SentinelPortFrom + randomPort
  private val redisServer1Port = RedisServer1PortFrom + randomPort
  private val redisServer2Port = RedisServer2PortFrom + randomPort

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
