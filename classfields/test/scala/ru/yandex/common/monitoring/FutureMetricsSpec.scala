package ru.yandex.common.monitoring

import com.codahale.metrics.MetricRegistry
import nl.grons.metrics4.scala.{Meter, Timer}
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}

import scala.concurrent.Future

/**
 * @author evans
 */
class FutureMetricsSpec
  extends WordSpec
    with Matchers
    with FutureMetrics
    with Instrumented
    with BeforeAndAfter
    with Eventually {

  var t: Timer = _
  var m: Meter = _

  override def metrics = new MetricBuilder(instrumentedClass, new MetricRegistry)

  override def metricRegistry = new MetricRegistry

  before {
    t = metrics.timer("test-timer")
    m = metrics.meter("test-meter")
  }

  import scala.concurrent.ExecutionContext.Implicits.global

  "Future metrics call only once by-name parameter" should {
    "for timing successful" in {
      val wr = new CountedCallValue(Future.successful(1))
      timingSuccessful(t)(wr.get)
      ready()
      wr.count shouldBe 1
    }

    "for metering" in {
      val wr = new CountedCallValue(Future.successful(1))
      metering(m)(wr.get)
      ready()
      wr.count shouldBe 1
    }

    "for metering successful" in {
      val wr = new CountedCallValue(Future.successful(1))
      meteringError(m)(wr.get)
      ready()
      wr.count shouldBe 1
    }

    "for full metering" in {
      val wr = new CountedCallValue(Future.successful(1))
      metering(m, m)(wr.get)
      ready()
      wr.count shouldBe 1
    }
  }

  "meters total" in {
    metering(m)(Future.successful(1))
    ready()
    m.count shouldBe 1
    metering(m)(Future.successful(1))
    ready()
    m.count shouldBe 2
    ready()
    intercept[IllegalArgumentException] {
      metering(m)(Future.failed(throw new IllegalArgumentException))
    }
    ready()
    m.count shouldBe 3
  }

  "meters success" in {
    meteringError(m)(Future.successful(1))
    ready()
    m.count shouldBe 0
    intercept[IllegalArgumentException] {
      meteringError(m)(Future.failed(throw new IllegalArgumentException))
    }
    ready()
    m.count shouldBe 1
  }

  "timer success" in {
    val f = Future {
      Thread.sleep(15)
      1
    }
    timingSuccessful(t)(f)
    eventually {
      t.count shouldBe 1
    }
    intercept[IllegalArgumentException] {
      metering(m)(Future.failed(throw new IllegalArgumentException))
    }
    ready()
    t.count shouldBe 1
  }

  private def ready(): Unit = Thread.sleep(30)
}
