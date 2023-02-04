package ru.yandex.vertis.personal.api.history

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import ru.yandex.vertis.personal.api.history.v1.HistoryV1Backend
import ru.yandex.vertis.personal.util.HandlerSpec
import spray.json.DefaultJsonProtocol
import ru.yandex.vertis.mockito.MockitoSupport.mock

class HistoryHandlerSpec extends HandlerSpec with SprayJsonSupport with DefaultJsonProtocol {

  private val v1Backend = mock[HistoryV1Backend]

  object HistoryHandlerMock extends HistoryHandler(v1Backend) {
    override protected def v1Handler: Route = dummyRoute("v1")
  }

  private val route = sealRoute(HistoryHandlerMock.routes)

  "HistoryHandler" should {
    "route /v1 to v1 handler" in {
      Get(s"/1.0/test") ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldBe "/test v1"
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
