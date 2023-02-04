package ru.yandex.vertis.moderation.api

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.util.HandlerSpecBase

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class ApiHandlerSpec extends HandlerSpecBase {

  override def basePath: String = ""

  import akka.http.scaladsl.model.StatusCodes.{NotFound, OK}

  "/ping" should {

    "return OK" in {
      Get(url("/ping")) ~> route ~> check {
        status shouldBe OK
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
