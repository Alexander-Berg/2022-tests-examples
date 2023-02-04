package ru.yandex.memcached.support

import java.util.Random

import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import org.testcontainers.containers.GenericContainer
import ru.yandex.memcached.{DataCenter, Environment, Support, SupportBase}

import scala.concurrent.duration._
import scala.concurrent.{Await, Awaitable, ExecutionContext}

/**
 * Unit tests for [[Spymemcached]]
 *
 * @author incubos
 */
class SpymemcachedIntSpec
    extends WordSpec
    with Matchers
      with BeforeAndAfterAll {

  private val memcached = new GenericContainer("memcached:latest")
  memcached.start()

  private val timeout = 5.seconds

  class SingleHostCache(host: String) extends Spymemcached with Environment {
    override def globalDataCenters = List(DataCenter(List(host)))

    override def localDataCenter = DataCenter(List(host))

    override val keyPrefix = s"test-${new Random().nextInt().toString}:"

    override def localTimeout = timeout

    override def globalTimeout = timeout

    override def expire = 5.seconds

    override def executionContext = ExecutionContext.global
  }

  private val onlineCache = new SingleHostCache(memcached.getContainerIpAddress + ":" + memcached.getMappedPort(11211))
  private val offlineCache = new SingleHostCache("localhost:11212")

  override protected def afterAll(): Unit = {
    onlineCache.close()
    offlineCache.close()
    memcached.stop()
    super.afterAll()
  }

  def result[T](awaitable: Awaitable[T]): T =
    Await.result(awaitable, timeout)

  "Online client" should {
    "not get nonexistent key" in {
      val KEY = "uid-" + math.abs(new Random().nextInt())
      result(onlineCache.get(KEY)) should be('empty)
    }

    "set and get" in {
      val KEY = "uid-" + math.abs(new Random().nextInt())
      val VALUE = "value-" + math.abs(new Random().nextInt())
      result(onlineCache.set(KEY, VALUE.getBytes))
      val cached = result(onlineCache.get(KEY))
      cached should be('nonEmpty)
      new String(cached.get) should equal(VALUE)
    }

    "reset and get" in {
      val KEY = "uid-" + math.abs(new Random().nextInt())
      val VALUE1 = "value-" + math.abs(new Random().nextInt())
      val VALUE2 = "value-" + math.abs(new Random().nextInt())

      result(onlineCache.set(KEY, VALUE1.getBytes))
      val cached1 = result(onlineCache.get(KEY))
      cached1 should be('nonEmpty)
      new String(cached1.get) should equal(VALUE1)

      result(onlineCache.set(KEY, VALUE2.getBytes))
      val cached2 = result(onlineCache.get(KEY))
      cached2 should be('nonEmpty)
      new String(cached2.get) should equal(VALUE2)
    }

    "not get deleted key" in {
      val KEY = "uid-" + math.abs(new Random().nextInt())
      val VALUE = "value-" + math.abs(new Random().nextInt())
      result(onlineCache.set(KEY, VALUE.getBytes))
      result(onlineCache.delete(KEY))
      val cached = result(onlineCache.get(KEY))
      cached should be('empty)
    }

    "expire" in {
      val KEY = "uid-" + math.abs(new Random().nextInt())
      val VALUE = "value-" + math.abs(new Random().nextInt())
      val EXPIRE = 3.seconds
      result(onlineCache.set(KEY, VALUE.getBytes, EXPIRE))
      Thread.sleep((EXPIRE * 2).toMillis)
      result(onlineCache.get(KEY)) should be('empty)
    }

    "set and multi get" in {
      val KEY = "uid-" + math.abs(new Random().nextInt())
      val VALUE = "value-" + math.abs(new Random().nextInt())
      result(onlineCache.set(KEY, VALUE.getBytes))
      val cached = result(onlineCache.multiGet(Iterable(KEY, "abc")))
      cached should be('nonEmpty)
      new String(cached.get(KEY).get) should equal(VALUE)
      cached.size should equal(1)
    }

    "multi set and multi get" in {
      val KEY1 = "uid-" + math.abs(new Random().nextInt())
      val VALUE1 = "value-" + math.abs(new Random().nextInt())
      val KEY2 = "uid-" + math.abs(new Random().nextInt())
      val VALUE2 = "value-" + math.abs(new Random().nextInt())
      val map = Map(
        KEY1 -> VALUE1.getBytes,
        KEY2 -> VALUE2.getBytes
      )
      result(onlineCache.multiSet(map))
      val cached = result(onlineCache.multiGet(Iterable(KEY1, KEY2, "abc")))
      cached should be('nonEmpty)
      new String(cached.get(KEY1).get) should equal(VALUE1)
      new String(cached.get(KEY2).get) should equal(VALUE2)
      cached.size should equal(2)
    }
  }

  "Offline client" should {
    "not set" in {
      intercept[RuntimeException](
        result(offlineCache.set("key", "value".getBytes)))
    }

    "not get" in {
      intercept[RuntimeException](
        result(offlineCache.get("key")))
    }

    "not delete" in {
      intercept[RuntimeException](
        result(offlineCache.delete("key")))
    }
  }
}
