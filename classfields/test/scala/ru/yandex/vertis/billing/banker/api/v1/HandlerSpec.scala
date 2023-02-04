package ru.yandex.vertis.billing.banker.api.v1

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.billing.banker.api.{MockedApiBackend, RootHandlerSpecBase}

/**
  * Spec on [[Handler]]
  *
  * @author ruslansd
  */
class HandlerSpec extends AnyWordSpecLike with RootHandlerSpecBase with MockedApiBackend {

  override def basePath: String = "/api/1.x/service"

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

  "/service unknown" should {
    "not found on unknown domain on get" in {
      Get(url("/ababaca")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
    "not found on unknown domain on post" in {
      Post(url("/ababaca")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "not found on unknown domain on put" in {
      Put(url("/ababaca")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "not found on unknown domain on delete" in {
      Delete(url("/ababaca")) ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
  }
}
