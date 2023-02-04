package ru.yandex.common.monitoring

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner

/**
 * Specs on [[MetricBuilder]]
 */
@RunWith(classOf[JUnitRunner])
class MetricBuilderSpec
  extends WordSpec
  with Matchers {

  "MetricBuilder" should {
    val metricBuilder = new MetricBuilder(this.getClass, Metrics.defaultRegistry())

    "register gauge and return it on subsequent call" in {
      val registered = metricBuilder.gauge("foo") {
        0
      }
      val returned = metricBuilder.gauge("foo") {
        0
      }
      returned.value should be(registered.value)
    }
  }

}
