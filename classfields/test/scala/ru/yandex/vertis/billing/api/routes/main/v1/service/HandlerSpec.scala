package ru.yandex.vertis.billing.api.routes.main.v1.service

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.api.RootHandlerSpecBase

/** Specs on service handler [[Handler]]
  */
class HandlerSpec extends AnyWordSpec with RootHandlerSpecBase {

  override def basePath: String = s"/api/1.x/service/autoru"

  "Handler" should {
    "reject unknown" in {
      Get(url("/unknown")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
  }
}
