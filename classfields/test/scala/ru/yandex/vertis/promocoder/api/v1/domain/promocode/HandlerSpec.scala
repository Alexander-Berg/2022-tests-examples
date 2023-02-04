package ru.yandex.vertis.promocoder.api.v1.domain.promocode

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Route
import org.joda.time.DateTime
import org.mockito.Mockito
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.promocoder.api.ApiRouteTest
import ru.yandex.vertis.promocoder.model.gens.ModelGenerators
import ru.yandex.vertis.promocoder.service.{PromocodeInstanceService, PromocodeService}
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.Future

class HandlerSpec extends ApiRouteTest with MockitoSupport with ModelGenerators {

  private val userHeader = RawHeader("X-Vertis-User", "some-user")
  private val requestIdHeader = RawHeader("X-Vertis-Request-ID", "some-request-id")

  trait TestEnv {
    val promocodeService: PromocodeService = mock[PromocodeService]
    val promocodeInstanceService: PromocodeInstanceService = mock[PromocodeInstanceService]

    val handler: Route = seal(
      new HandlerImpl(promocodeService, promocodeInstanceService).route
    )
  }

  "PromocodeHandler" should {
    "update deadline" in new TestEnv {
      val request: HttpRequest =
        Put(s"/123-321?current_deadline=2022-12-01&new_deadline=2022-12-02").withHeaders(userHeader, requestIdHeader)
      when(
        promocodeService.updateDeadline(
          MockitoSupport.eq("123-321"),
          MockitoSupport.eq(new DateTime("2022-12-02T03:00:00.000+03:00", DateTimeUtil.DefaultTimeZone)),
          MockitoSupport.eq(new DateTime("2022-12-01T03:00:00.000+03:00", DateTimeUtil.DefaultTimeZone))
        )(?)
      )
        .thenReturn(Future.unit)

      request ~> handler ~> check {
        status shouldEqual StatusCodes.OK
      }
      Mockito.verify(promocodeService).updateDeadline(?, ?, ?)(?)
    }

    "return 400 bad request on promocode deadline update error" in new TestEnv {
      val request: HttpRequest =
        Put(s"/123-321?current_deadline=2022-12-01&new_deadline=2022-12-02").withHeaders(userHeader, requestIdHeader)
      when(
        promocodeService.updateDeadline(
          MockitoSupport.eq("123-321"),
          MockitoSupport.eq(new DateTime("2022-12-02T03:00:00.000+03:00", DateTimeUtil.DefaultTimeZone)),
          MockitoSupport.eq(new DateTime("2022-12-01T03:00:00.000+03:00", DateTimeUtil.DefaultTimeZone))
        )(?)
      )
        .thenReturn(Future.failed(new IllegalArgumentException("fail")))

      request ~> handler ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
      Mockito.verify(promocodeService).updateDeadline(?, ?, ?)(?)
    }
  }

}
