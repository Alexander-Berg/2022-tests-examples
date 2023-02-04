package ru.yandex.vertis.picapica.misc


import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import org.slf4j.LoggerFactory
import ru.yandex.vertis.picapica.misc.PerHostQueue.Result.{Elem, Wait}
import ru.yandex.vertis.picapica.misc.impl.PerHostQueueImpl

import scala.concurrent.duration._

/**
  * @author @logab
  */
//scalastyle:off

@RunWith(classOf[JUnitRunner])
class PerHostQueueSpec extends WordSpecLike with Matchers {
  val log = LoggerFactory.getLogger(classOf[PerHostQueueSpec])

  def now = System.currentTimeMillis()

  private val MaxSize: Int = 1000


  val hosts = (0 until 1000).map({_.toString}).toVector

  private val DefaultWaitInterval: FiniteDuration = 500.millis

  private val MaxPerHost: Int = 10

  def buildQueue(
      maxSize: Int = MaxSize,
      maxPerHost: Int = MaxPerHost,
      hostIntervals: Map[String, FiniteDuration] = Map.empty,
      interval: FiniteDuration = DefaultWaitInterval,
      maxRps: Int = 100) = {
    new PerHostQueueImpl[Long](maxSize, maxPerHost, hostIntervals, interval, maxRps)
  }

  "perHostQueue" should {
    "retrieve exactly what was added" in {
      val queue = buildQueue()
      val expected: Long = now
      queue.enqueue(hosts.head, expected)
      queue.dequeue() match {
        case Wait(_) => fail("wait when should dequeue")
        case Elem(actual) => actual should be(expected)
      }
    }
    "wait for second dequeue" in {
      val waitInterval = 200.millis
      val queue = buildQueue(interval = waitInterval)
      val first: Long = 1
      queue.enqueue(hosts.head, first)
      Thread.sleep(2)
      val expected: Long = 2
      queue.enqueue(hosts.head, expected)
      queue.dequeue() match {
        case Wait(_) => fail("wait when should dequeue")
        case Elem(actual) =>
          actual should be(first)
      }
      queue.dequeue() match {
        case wait: Wait =>
          shouldWaitLessOrEqual(waitInterval, wait)
        case _ => fail("dequeue when should wait")
      }
      Thread.sleep(waitInterval.toMillis)
      queue.dequeue() match {
        case Wait(_) => fail("wait when should dequeue")
        case Elem(actual) => actual should be(expected)
      }
    }
    "apply perHostInterval" in {
      val perHostInterval = 100.millis
      val waitInterval = 200.millis
      val hs@Vector(h0, h1) = hosts.take(2)
      val hostIntervals = Map(h0 -> perHostInterval)
        .withDefaultValue(waitInterval)
      val queue = buildQueue(interval = waitInterval, hostIntervals = hostIntervals)
      val first: Long = 1
      hs.foreach(queue.enqueue(_, first))
      Thread.sleep(10)
      val expectedSpecial: Long = 2
      val expectedUsual: Long = 3
      queue.enqueue(h0, expectedSpecial)
      queue.enqueue(h1, expectedUsual)
      hs.foreach { _ =>
        queue.dequeue() match {
          case Wait(_) => fail("wait when should dequeue")
          case Elem(actual) => actual should be(first)
        }
      }
      hs.foreach { host =>
        val interval = hostIntervals(host)
        queue.dequeue() match {
          case wait: Wait =>
            shouldWaitLessOrEqual(interval, wait)
          case _ => fail("dequeue when should wait")
        }
      }
      Thread.sleep(perHostInterval.toMillis)
      queue.dequeue() match {
        case Wait(_) => fail("wait when should dequeue")
        case Elem(actual) => actual should be(expectedSpecial)
      }
      queue.dequeue() match {
        case wait: Wait =>
          val interval = waitInterval - perHostInterval
          shouldWaitLessOrEqual(interval, wait)
          Thread.sleep(interval.toMillis)
        case _ => fail("dequeue when should wait")
      }
      queue.dequeue() match {
        case Wait(_) => fail("wait when should dequeue")
        case Elem(actual) => actual should be(expectedUsual)
      }
    }
    "not add to the same host" in {
      val queue = buildQueue()
      (0 until MaxPerHost) foreach { o =>
        queue.canAdd(hosts.head) shouldEqual true
        queue.enqueue(hosts.head, o)
      }
      queue.canAdd(hosts.head) shouldEqual false
      queue.canAdd(hosts(2)) shouldEqual true
    }
    "not add to overflowed queue" in {
      val queue = buildQueue()
      hosts foreach { host =>
        queue.canAdd(host) shouldEqual true
        queue.enqueue(host, host.toLong)
      }
      hosts foreach { host =>
        queue.canAdd(host) shouldEqual false
      }
    }
    "limit rps" in {
      val queue = buildQueue(maxRps = 10)
      val elements = 1 to 10
      elements.foreach { el =>
        queue.enqueue(el.toString, el)
      }
      val results = elements.map { _ =>
        Thread.sleep(50)
        queue.dequeue()
      }
      val resPerClass = results.groupBy(_.getClass)
      resPerClass.size shouldBe 2
    }
  }

  private def shouldWaitLessOrEqual(interval: FiniteDuration, wait: Wait) = {
    wait.time should be <= interval
    wait.time should be > 0.millis
  }

}

//scalastyle:on
