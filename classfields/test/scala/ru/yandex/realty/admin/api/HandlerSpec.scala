package ru.yandex.realty.admin.api

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class HandlerSpec extends ApiHandlerSpecBase {

  import akka.http.scaladsl.model.StatusCodes.OK

  override def basePath: String = ""

  "/ping" should {

    "return OK" in {
      Get(url("/ping")) ~> route ~> check {
        status shouldBe OK
      }
    }
  }

}
