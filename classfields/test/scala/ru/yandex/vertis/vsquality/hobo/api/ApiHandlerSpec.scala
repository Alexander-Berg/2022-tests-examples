package ru.yandex.vertis.vsquality.hobo.api

import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.StatusCodes.PermanentRedirect
import akka.http.scaladsl.model.headers.Location
import ru.yandex.vertis.vsquality.hobo.util.HandlerSpecBase

/**
  * @author semkagtn
  */

class ApiHandlerSpec extends HandlerSpecBase {

  override def basePath: String = ""

  import akka.http.scaladsl.model.StatusCodes.{NotFound, OK, SeeOther}

  "/ping" should {

    "return OK" in {
      Get(url("/ping")) ~> route ~> check {
        status shouldBe OK
      }
    }
  }

  "/docs/" should {

    "return swagger html" in {
      Get(url("/docs")) ~> route ~> check {
        status shouldBe PermanentRedirect
        header(Location.name) shouldBe Some(Location("/docs/index.html?url=/docs/docs.yaml"))
        contentType shouldBe ContentTypes.`text/html(UTF-8)`
      }
    }

    "redirect from root" in {
      Get(url("/")) ~> route ~> check {
        status shouldBe SeeOther
        header(Location.name) shouldBe Some(Location("/docs"))
      }
    }
  }

  "wrong path" should {

    "return 404" in {
      Get(url("/something-wrong")) ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }
}
