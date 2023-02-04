package ru.yandex.vertis.billing.banker.api.base_admin

import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.StatusCodes.SeeOther
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.billing.banker.api.Handler

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

  "/" should {
    "redirect to swagger html" in {
      Get(url("/")) ~> route ~> check {
        status shouldBe SeeOther
        header("Location").get.value() shouldBe "swagger/index.html?url=/api/1.x"
      }
    }
  }

  "/swagger/index.html" should {
    "return swagger html" in {
      Get(url("/swagger/index.html")) ~> route ~> check {
        status shouldBe OK
        contentType shouldBe ContentTypes.`text/html(UTF-8)`
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
