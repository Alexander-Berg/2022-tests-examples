package ru.yandex.vertis.curator.recipes.discovery

import org.scalatest.{Matchers, WordSpec}

/**
 * Specs on [[ServiceInstanceUtils]]
 */
class ServiceInstanceUtilsSpec
  extends WordSpec
  with Matchers {

  "toServiceInstanceBuilder" should {
    "produce same service instance" in {
      val initial = TestData.
        serviceInstance("foo", "bar", "test-service")

      ServiceInstanceUtils.
        toServiceInstanceBuilder(initial).
        build() should equal(initial)
    }
  }
}
