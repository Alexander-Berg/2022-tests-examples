package ru.yandex.vertis.billing.banker.notification.v1

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.billing.banker.notification.RootHandlerSpecBase

/**
  * Spec on [[Handler]]
  *
  * @author ruslansd
  */
class HandlerSpec extends AnyWordSpecLike with RootHandlerSpecBase {

  override def basePath: String = "/api/1.x/service"

  "/service" should {
    "not found on unknown domain on get" in {
      Get(url("realty")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
    "not found on unknown domain on post" in {
      Post(url("realty")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "not found on unknown domain on put" in {
      Put(url("realty")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "not found on unknown domain on delete" in {
      Delete(url("realty")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
  }
}
