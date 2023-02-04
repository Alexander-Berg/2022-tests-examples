package ru.yandex.vertis.promocoder.api.v1.domain.feature

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Route
import org.mockito.Mockito
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.promocoder.api.ApiRouteTest
import ru.yandex.vertis.promocoder.model.FeatureInstance
import ru.yandex.vertis.promocoder.model.gens.ModelGenerators
import ru.yandex.vertis.promocoder.service.FeatureInstanceService
import ru.yandex.vertis.promocoder.view.FeatureInstanceView

import scala.concurrent.Future

class HandlerSpec extends ApiRouteTest with MockitoSupport with ModelGenerators {

  private val userHeader = RawHeader("X-Vertis-User", "some-user")
  private val requestIdHeader = RawHeader("X-Vertis-Request-ID", "some-request-id")

  trait TestEnv {
    val mockFeatureService: FeatureInstanceService = mock[FeatureInstanceService]

    val handler: Route = seal(
      new HandlerImpl(mockFeatureService).route
    )
  }

  "Feature Handler" should {
    "finish feature by id, ignoring key" in new TestEnv {
      import FeatureInstanceView.modelUnmarshaller

      val request: HttpRequest = Delete(s"/123-321/finish?key=1").withHeaders(userHeader, requestIdHeader)

      val featureInstance: FeatureInstance = FeatureInstanceGen.next
      when(mockFeatureService.finishNow(MockitoSupport.eq("123-321"))(?))
        .thenReturn(Future.successful(featureInstance))

      request ~> handler ~> check {
        status shouldEqual StatusCodes.OK
        val response = responseAs[FeatureInstance]
        response shouldBe featureInstance
      }

      Mockito.verify(mockFeatureService).finishNow(?)(?)
    }

    "get features for user" in new TestEnv {
      import FeatureInstanceView.modelIterableUnmarshaller

      val request: HttpRequest = Get(s"/user/1234").withHeaders(userHeader, requestIdHeader)

      val featureInstances: Iterable[FeatureInstance] = FeatureInstanceGen.next(5)
      when(mockFeatureService.get(?, ?, ?)(?)).thenReturn(Future.successful(featureInstances))

      request ~> handler ~> check {
        status shouldEqual StatusCodes.OK
        val response = responseAs[Iterable[FeatureInstance]]
        response should contain theSameElementsAs featureInstances
      }

      Mockito.verify(mockFeatureService).get(?, ?, ?)(?)
    }
  }
}
