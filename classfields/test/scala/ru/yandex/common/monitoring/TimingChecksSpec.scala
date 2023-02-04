package ru.yandex.common.monitoring

import java.util.Random
import java.util.concurrent.TimeUnit

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import ru.yandex.common.monitoring.healthchecks.{beHealthy, beUnhealthy, beWarning}
import scala.collection.JavaConverters._

/**
 * Tests for [[TimingChecks]]
 *
 * @author incubos
 */
@RunWith(classOf[JUnitRunner])
class TimingChecksSpec
  extends WordSpec
  with Matchers
  with Instrumented
  with BeforeAndAfterEach {

  val m = metrics
  val c = HealthChecks.defaultRegistry()

  override protected def beforeEach(): Unit = {
    // Unregister all
    c.runHealthChecks().asScala.toMap.foreach(kv => c.unregister(kv._1))
  }

  "TimingChecks" should {
    "accept no request" in {
      val name = "test" + new Random().nextInt()
      val timer = m.timer(name)
      TimingChecks.timingHealthCheck(name, 100, 300, timer)

      c.runHealthChecks().get(name) should beHealthy
    }

    "accept 1K fast requests" in {
      val name = "test" + new Random().nextInt()
      val timer = m.timer(name)
      TimingChecks.timingHealthCheck(name, 100, 300, timer)

      (0 until 1000).foreach(_ => timer.update(1, TimeUnit.MILLISECONDS))

      c.runHealthChecks().get(name) should beHealthy
    }

    "warn about 0.5% slow requests" in {
      val name = "test" + new Random().nextInt()
      val timer = m.timer(name)
      TimingChecks.timingHealthCheck(name, 100, 300, timer)

      (0 until 1000).foreach(_ => timer.update(1, TimeUnit.MILLISECONDS))
      (0 until 5).foreach(_ => timer.update(1, TimeUnit.SECONDS))

      c.runHealthChecks().get(name) should beWarning
    }

    "err about 10% slow requests" in {
      val name = "test" + new Random().nextInt()
      val timer = m.timer(name)
      TimingChecks.timingHealthCheck(name, 100, 300, timer)

      (0 until 1000).foreach(_ => timer.update(1, TimeUnit.MILLISECONDS))
      (0 until 100).foreach(_ => timer.update(1, TimeUnit.SECONDS))

      c.runHealthChecks().get(name) should beUnhealthy
    }

    "switch off critical after becoming fast" in {
      val name = "test" + new Random().nextInt()
      val timer = m.timer(name)
      TimingChecks.timingHealthCheck(name, 100, 300, timer)

      (0 until 100).foreach(_ => timer.update(1, TimeUnit.SECONDS))
      c.runHealthChecks().get(name) should beUnhealthy

      (0 until 50000).foreach(_ => timer.update(1, TimeUnit.MILLISECONDS))
      c.runHealthChecks().get(name) should not(beUnhealthy)
    }
  }
}
