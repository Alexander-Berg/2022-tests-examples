package ru.yandex.vertis.curator.recipes.map

import java.util.concurrent.CountDownLatch

import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vertis.curator.ZooKeeperAware
import ru.yandex.vertis.curator.util.CuratorUtils

import scala.concurrent.duration._
import scala.util.Success

/**
 * Specs on [[ZooKeeperMap]]
 */
class ZookeeperMapSpec
  extends WordSpec
  with Matchers
  with ZooKeeperAware {

  val curator = CuratorUtils.fixNamespace(curatorBase, "zookeeper-map-spec")

  val map = new ZooKeeperMap[String](
    curator,
    "/project/module/special-map",
    StringValueSerializer)(ZooKeeperMap.DefaultSyncScheduler)
    with LoggingZooKeeperMap[String]

  val ephemeral = false

  "DistributedMap" should {
    "create and produce event" in {
      using(map)(_.put("foo", "bar", ephemeral) should be(Success(true))) {
        case Event.Updated("foo", "bar") => true
      }

      using(map)(_.put("foo", "baz", ephemeral) should be(Success(true))) {
        case Event.Updated("foo", "baz") => true
      }
    }

    "putIfAbsent and produce event" in {
      using(map)(_.putIfAbsent("bar", "baz", ephemeral) should be(Success(true))){
        case Event.Updated("bar", "baz") => true
      }

      map.putIfAbsent("bar", "baaz", ephemeral) should be(Success(false))
    }

    "remove and produce event" in {
      using(map)(_.remove("foo") should be(Success(true))) {
        case Event.Removed("foo", "baz") => true
      }
    }

    "not delete non-exists node" in {
      map.remove("foo") should be(Success(false))
    }

    "remove with predicate" in {
      val key = "if-predicate-key"
      val value = "predicate holds"
      using(map)(_.put(key, value, ephemeral) should be(Success(true))) {
        case Event.Updated(`key`, `value`) => true
      }
      map.removeIf(key) {
        v => v == "predicate doesn't hold"
      } should be(Success(false))
      map.removeIf(key) {
        v => v == value
      } should be(Success(true))
      map.removeIf(key) {
        v => v == value
      } should be(Success(false))
    }

    "put with predicate" in {
      val key = "predicate"
      val value = "value"
      val newValue = "new-value"

      using(map)(_.put(key, value, ephemeral) should be(Success(true))) {
        case Event.Updated(`key`, `value`) => true
      }
      map.putIf(key, newValue, ephemeral) {
        v => v == "unexpected value"
      } should be(Success(false))

      using(map)(_.putIf(key, newValue, ephemeral) {
        v => v == value
      } should be(Success(true))) {
        case Event.Updated(`key`, `newValue`) => true
      }
    }

    "update with updater" in {
      val key = "update"
      val value = "value"

      intercept[NoSuchElementException] {
        map.update(key)(_ => "foo").get
      }

      using(map)(_.put(key, value, ephemeral) should be(Success(true))) {
        case Event.Updated(`key`, `value`) => true
      }

      using(map)(_.update(key) {
        v => v + "-updated"
      } should be(Success(true))) {
        case Event.Updated(`key`, v) if v == value + "-updated" => true
      }
    }

    "tryModify with updater" in {
      val key = "tryModify"
      val value = "value"

      intercept[NoSuchElementException] {
        map.tryModify(key)(_ => ("foo", "bar")).get
      }

      using(map)(_.put(key, value, ephemeral) should be(Success(true))) {
        case Event.Updated(`key`, `value`) => true
      }

      using(map)(_.tryModify(key) {
          v => (v + "-updated", v + "-result")
      } should be(Success(Some(value + "-result")))) {
        case Event.Updated(`key`, v) if v == value + "-updated" => true
      }
    }

    "neighbour map should see values" in {
      using(map)(_.put("foo", "bar", ephemeral) should be(Success(true))) {
        case Event.Updated("foo", "bar") => true
      }

      val map2 = new ZooKeeperMap[String](
        curator,
        "/project/module/special-map",
        StringValueSerializer)(ZooKeeperMap.DefaultSyncScheduler)

      map.snapshot.get("foo") should be(Some("bar"))
      map2.snapshot.get("foo") should be(Some("bar"))
      map2.close()
    }

  }

  override protected def afterAll() = {
    map.close()
    super.afterAll()
  }

  private def using[A](map: ZooKeeperMap[String])
                      (actionWithMap: ZooKeeperMap[String] => A)
                      (awaitFor: PartialFunction[Event[String, String], Boolean]) = {
    val waiter = AwaitListener(awaitFor)
    map.subscribe(waiter)
    try {
      actionWithMap(map)
      waiter.await(10.seconds) should be(true)
    }
    finally {
      map.unsubscribe(waiter)
    }
  }

  private class AwaitListener(awaitFor: PartialFunction[Event[String, String], Boolean])
    extends EventListener[Event[String, String]] {

    val l = new CountDownLatch(1)

    override def onEvent(e: Event[String, String]) = {
      if (awaitFor.isDefinedAt(e) && awaitFor(e))
        l.countDown()
    }

    def await(d: Duration) = l.await(d.length, d.unit)

    override def toString: String = s"AwaitListener($hashCode)"
  }

  private object AwaitListener {
    def apply(awaitFor: PartialFunction[Event[String, String], Boolean]) =
      new AwaitListener(awaitFor)
  }

}
