package ru.yandex.vertis.billing.api.routes.main.v1

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.api.RootHandlerSpecBase

/** Specs on root handler [[Handler]]
  */
class HandlerSpec extends AnyWordSpec with RootHandlerSpecBase {

  override def basePath: String = s"/api/1.x/service"

  "/service not registered" should {
    "not found on not registered domain on get" in {
      Get(url("/realty")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
    "not found on not registered domain on post" in {
      Post(url("/realty")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "not found on not registered domain on put" in {
      Put(url("/realty")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "not found on not registered domain on delete" in {
      Delete(url("/realty")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
  }

}
