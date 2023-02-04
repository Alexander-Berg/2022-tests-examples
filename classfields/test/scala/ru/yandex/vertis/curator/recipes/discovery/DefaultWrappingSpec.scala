package ru.yandex.vertis.curator.recipes.discovery

import org.scalatest.{Matchers, WordSpec}

import scala.util.{Failure, Success}

/**
 * Specs on [[DefaultWrapping]]
 */
class DefaultWrappingSpec
  extends WordSpec
  with Matchers {

  "DefaultWrapping" should {
    val wrapping = new DefaultWrapping[String]

    "unwrap wrapped" in {
      val deploy = TestData.Deploys.csbo1ft
      val instance = TestData.serviceInstance("foo", "bar", "test")
      val initial = DeployedServiceInstance(deploy, instance)

      val wrapped = wrapping.wrap(initial)

      wrapping.unwrap(wrapped) should be(Success(initial))
    }

    "unable unwrap invalid wrapped" in {
      val beatenInstance = TestData.serviceInstance("fol##instance-id", "bar", "test")
      wrapping.unwrap(beatenInstance) match {
        case Success(_) =>
          fail("unwrapped beaten service instance")
        case Failure(e) if e.isInstanceOf[IllegalArgumentException] =>

        case Failure(e) =>
          fail(s"Expected IllegalArgumentException, but got $e")
      }
    }
  }

}
