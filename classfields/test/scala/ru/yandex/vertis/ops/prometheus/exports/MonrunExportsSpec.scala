package ru.yandex.vertis.ops.prometheus.exports

import com.codahale.metrics.health.HealthCheckRegistry
import io.prometheus.client.Collector.MetricFamilySamples.Sample
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.common.monitoring.healthchecks.{HealthyCheck, UnhealthyCheck, WarningCheck}
import ru.yandex.vertis.ops.prometheus.CompositeCollector
import ru.yandex.vertis.ops.prometheus.exports.MonrunExportsSpec.Setup

import scala.collection.JavaConverters.asScalaBufferConverter
import MonrunCollector._

/**
  * Specs on [[MonrunExports]]
  *
  * @author alex-kovalenko
  */
class MonrunExportsSpec
  extends WordSpec
    with Matchers {

  "MonrunExports" should {
    "observe 1 when there are no healthchecks" in new Setup {
      MonrunExports(healthChecks, prometheus)
      val sample = getMonrun
      sample.value shouldBe WarningValue
      samples.size shouldBe 1
    }
    "observe 0 if ok" in new Setup {
      healthChecks.register("ok", HealthyCheck)
      MonrunExports(healthChecks, prometheus)
      val sample = getMonrun
      sample.value shouldBe OkValue
      samples.size shouldBe 2
    }
    "observer 1 if warning" in new Setup {
      healthChecks.register("warn", WarningCheck)
      MonrunExports(healthChecks, prometheus)
      val sample = getMonrun
      sample.value shouldBe WarningValue
      samples.size shouldBe 2
    }
    "observe 1 if there is at least one warning and no errors" in new Setup {
      healthChecks.register("ok", HealthyCheck)
      healthChecks.register("warn", WarningCheck)
      healthChecks.register("ok2", HealthyCheck)

      MonrunExports(healthChecks, prometheus)
      val sample = getMonrun
      sample.value shouldBe WarningValue
      samples.size shouldBe 4
    }
    "observe 2 if error" in new Setup {
      healthChecks.register("error", UnhealthyCheck)
      MonrunExports(healthChecks, prometheus)
      val sample = getMonrun
      sample.value shouldBe CriticalValue
      samples.size shouldBe 2
    }
    "observe 2 if there is at least one error" in new Setup {
      healthChecks.register("ok", HealthyCheck)
      healthChecks.register("warn", WarningCheck)
      healthChecks.register("ok2", HealthyCheck)
      healthChecks.register("error", UnhealthyCheck)
      healthChecks.register("warn2", WarningCheck)

      MonrunExports(healthChecks, prometheus)
      val sample = getMonrun
      sample.value shouldBe CriticalValue
      samples.size shouldBe 6
    }
  }
}

object MonrunExportsSpec {

  trait Setup {
    val prometheus = new CompositeCollector
    val healthChecks = new HealthCheckRegistry

    def samples: Iterable[Sample] = prometheus.collect().asScala
      .flatMap(_.samples.asScala)

    def getMonrunOpt: Option[Sample] = {
      samples
        .find {
          sample =>
            sample.name == "monrun" &&
              sample.labelValues.contains("status")
        }
    }

    def getMonrun: Sample =
      getMonrunOpt.getOrElse(sys.error("There is no monrun status sample"))
  }

}
