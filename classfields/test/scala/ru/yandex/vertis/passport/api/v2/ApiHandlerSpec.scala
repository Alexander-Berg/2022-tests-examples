package ru.yandex.vertis.passport.api.v2

import akka.http.scaladsl.model.StatusCodes._
import org.scalatest.WordSpec
import ru.yandex.vertis.passport.api.{MockedBackend, RootedSpecBase}
import ru.yandex.vertis.passport.api.NoTvmAuthorization

/**
  *
  * @author zvez
  */
class ApiHandlerSpec extends WordSpec with RootedSpecBase with MockedBackend with NoTvmAuthorization {
  "ApiHandler" should {
    "response NotFound on unknown service" in {
      Get("/api/2.x/realty") ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }
}
