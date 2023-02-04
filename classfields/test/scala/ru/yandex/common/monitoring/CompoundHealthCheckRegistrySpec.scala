package ru.yandex.common.monitoring

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.common.monitoring.healthchecks.{HealthyCheck, UnhealthyCheck}

/**
 * Tests for [[CompoundHealthCheckRegistry]]
 *
 * @author dimas
 */
@RunWith(classOf[JUnitRunner])
class CompoundHealthCheckRegistrySpec
  extends WordSpec
  with Matchers {

  val r = new CompoundHealthCheckRegistry()

  "CompoundHealthCheckRegistry" should {
    "register common and operational checks" in {

      r.register("healthy", HealthyCheck, false)
      r.register("unhealthy", UnhealthyCheck, false)
      r.register("op-healthy", HealthyCheck, true)
      r.register("op-unhealthy", HealthyCheck, true)

      r.getNames.contains("healthy") should be(false)
      r.getNames.contains("unhealthy") should be(false)

      r.getNames.contains("op-healthy") should be(true)
      r.getNames.contains("op-unhealthy") should be(true)

      r.runHealthChecks().size() should be(2)
      r.runDeveloperChecks().size() should be(4)
    }
  }
}