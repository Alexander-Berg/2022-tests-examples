package ru.yandex.common.monitoring.metric

import com.codahale.metrics.MetricRegistry
import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.common.monitoring.Instrumented

import org.scalatestplus.junit.JUnitRunner

import scala.collection.JavaConverters._
import scala.concurrent.duration._

/**
 * Tests on [[ExpiringGaugeReservoir]].
 *
 * @author dimas
 */
@RunWith(classOf[JUnitRunner])
class ExpiringGaugeReservoirSpec
  extends WordSpec
  with Matchers {

  def fixture =
    new {
      val instrumented = new Instrumented {
        override lazy val metricRegistry: MetricRegistry =
          new MetricRegistry
      }
      val registry = instrumented.metricRegistry
      val reservoir = new ExpiringGaugeReservoir[Long](
        ExpireAfterWrite,
        instrumented.metrics)
    }

  val ExpireAfterWrite = 700.millis

  "ExpiringGaugeReservoir" should {
    "evict gauge after expire" in {
      val f = fixture
      import f._

      reservoir.update("foo", 10)

      val gauge = singleGauge(registry)

      gauge.getValue should be(10)

      Thread.sleep(ExpireAfterWrite.toMillis)

      Option(gauge.getValue) should be(None)

      registry.getGauges.size() should be(0)
    }

    "not evict gauge before expire" in {
      val f = fixture
      import f._

      reservoir.update("foo", 10)

      val gauge = singleGauge(registry)

      gauge.getValue should be(10)

      Thread.sleep(ExpireAfterWrite.toMillis / 4)

      registry.getGauges.size() should be(1)

      gauge.getValue should be(10)

      reservoir.update("foo", 20)

      registry.getGauges.size() should be(1)

      gauge.getValue should be(20)

      Thread.sleep(ExpireAfterWrite.toMillis / 4)

      gauge.getValue should be(20)

      Thread.sleep(ExpireAfterWrite.toMillis)

      Option(gauge.getValue) should be(None)
    }
  }

  private def singleGauge(registry: MetricRegistry) = {
    registry.getGauges.size() should be(1)
    registry.getGauges.asScala.head._2
  }

}
