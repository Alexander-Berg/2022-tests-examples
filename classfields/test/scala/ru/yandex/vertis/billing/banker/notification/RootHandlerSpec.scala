package ru.yandex.vertis.billing.banker.notification

import org.scalatest.wordspec.AnyWordSpecLike

/**
  * Spec on [[Handler]]
  *
  * @author ruslansd
  */
class RootHandlerSpec extends AnyWordSpecLike with RootHandlerSpecBase {

  import akka.http.scaladsl.model.StatusCodes.{NotFound, OK}

  "/ping" should {
    "return OK" in {
      Get(url("/ping")) ~> route ~> check {
        status shouldBe OK
      }
    }
  }

  "on completely wrong path" should {
    "return NotFound" in {
      Get(url("/something-wrong")) ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

}
