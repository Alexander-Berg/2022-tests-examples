package ru.yandex.vertis.billing.banker.api.v1.service.keyvalue

import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.billing.banker.api.base_admin.RootHandlerSpecBase

import scala.concurrent.Future

/**
  * Spec on [[Handler]]
  *
  * @author alex-kovalenko
  */
class HandlerSpec extends AnyWordSpecLike with RootHandlerSpecBase {

  override def basePath: String = "/api/1.x/service/autoru/keyvalue"

  "get /settings" should {
    "provide all settings" in {
      when(backend.keyValueDao.getAll)
        .thenReturn(Future.successful(Map("k1" -> "v1", "k2" -> "v2")))

      Get(url("/settings")) ~> route ~> check {
        status shouldBe StatusCodes.OK
        val bodyRows = responseAs[String].trim.split("\n")
        (bodyRows should contain).allOf("k1 -> v1", "k2 -> v2")
      }
    }
  }

  "get /settings/{key}" should {
    "provide setting by key" in {
      when(backend.keyValueDao.get("k1"))
        .thenReturn(Future.successful("v1"))

      Get(url("/settings/k1")) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String].trim shouldBe "k1 -> v1"
      }
    }

  }

  "post /settings/{key}" should {
    "update setting by key" in {
      when(backend.keyValueDao.put("k1", "v1"))
        .thenReturn(Future.successful(()))

      Post(url("/settings/k1"))
        .withEntity(ContentTypes.`text/plain(UTF-8)`, "v1") ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String].trim shouldBe "k1 -> v1"
      }
    }
  }
}
