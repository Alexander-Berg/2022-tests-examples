package ru.yandex.hydra.profile.api

import akka.http.scaladsl.model.StatusCodes.{NotFound, OK}
import akka.http.scaladsl.server.Route

/** Tests for root [[RootHandler]]
  *
  * @author incubos
  */
class RootHandlerSpec extends BaseHandlerSpec {

  val route: Route = seal {
    new RootHandler {
      override def v2Handler: HttpHandler = EchoHandler
    }
  }

  "/api/v2" should {
    "be there" in {
      Get("/api/v2") ~> route ~> check {
        status shouldBe OK
      }
    }
  }

  "other routes" should {
    "return 404" in {
      Get("/api/v1") ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }
}
