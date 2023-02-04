package ru.yandex.vertis.chat.api.v1

import akka.http.scaladsl.model.StatusCodes
import ru.yandex.vertis.chat.Domains
import ru.yandex.vertis.chat.api.{EchoHandler, HandlerSpecBase}
import ru.yandex.vertis.chat.util.DMap

/**
  * Specs on [[ApiHandler]].
  *
  * @author dimas
  */
class ApiHandlerSpec extends HandlerSpecBase {

  private val route = seal(
    new ApiHandler(DMap.forAllDomains(EchoHandler), EchoHandler, EchoHandler).route
  )

  s"GET $root" should {
    "provide API info" in {
      Get(root) ~> route ~> check {
        status should be(StatusCodes.OK)
      }
    }
  }

  s"ANY $root<domain>" should {
    val entity = "foo"
    "route to known domain" in {
      Post(s"$root${Domains.Auto}", entity) ~> route ~> check {
        status should be(StatusCodes.OK)
        responseAs[String] should be(entity)
      }
    }
    "respond 404 for unknown domain" in {
      Get(s"${root}medicine") ~> route ~> check {
        status should be(StatusCodes.NotFound)
      }
    }
  }

}
