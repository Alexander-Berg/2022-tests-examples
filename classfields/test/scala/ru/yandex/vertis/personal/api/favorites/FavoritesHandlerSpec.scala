package ru.yandex.vertis.personal.api.favorites

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import ru.yandex.vertis.personal.util.HandlerSpec
import ru.yandex.vertis.personal.api.favorites.v1.FavoritesV1Backend
import ru.yandex.vertis.personal.api.favorites.v2.FavoritesV2Backend
import ru.yandex.vertis.mockito.MockitoSupport.mock

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 29.06.16
  */
class FavoritesHandlerSpec extends HandlerSpec {

  private val v1backend = mock[FavoritesV1Backend]
  private val v2backend = mock[FavoritesV2Backend]

  object FavoritesHandlerMock extends FavoritesHandler(v1backend, v2backend) {
    override protected def v1Handler: Route = dummyRoute("v1")
    override protected def v2Handler: Route = dummyRoute("v2")
  }

  private val route = sealRoute(FavoritesHandlerMock.routes)

  "FavoritesHandler" should {
    "route /v1 to v1 handler" in {
      Get(s"/1.0/test") ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldBe "/test v1"
      }
    }
    "route /v2 to v2 handler" in {
      Get(s"/2.0/test") ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldBe "/test v2"
      }
    }
    "return NotFound on unknown version" in {
      Get(s"/1.x/test") ~> route ~> check {
        status shouldBe StatusCodes.NotFound
        responseAs[String] shouldBe "version [1.x] not found"
      }
    }
  }
}
