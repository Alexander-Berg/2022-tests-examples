package ru.auto.salesman.api.v1.service.tariff

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import org.mockito.Mockito.verify
import ru.auto.salesman.api.DeprecatedMockitoRoutingSpec
import ru.auto.salesman.api.view.TariffView
import ru.auto.salesman.dao.TariffDao.Actual
import ru.auto.salesman.service.TariffService
import ru.auto.salesman.test.model.gens._
import spray.json._

class TariffHandlerSpec extends DeprecatedMockitoRoutingSpec {

  private val ClientId = 1111
  private val Tariff = TariffGen.next.copy(clientId = ClientId)

  private val asyncService = {
    val m = mock[TariffService]
    when(m.get(?))
      .thenReturnZ(Some(Tariff))
    when(m.upsert(?))
      .thenReturnZ(())
    when(m.delete(?))
      .thenReturnZ(())
    m
  }

  private val route = new TariffHandler(asyncService).route

  "POST /" should {
    val uri = "/"
    val entity = HttpEntity(
      ContentTypes.`application/json`,
      TariffView.asView(Tariff).toJson.compactPrint
    )

    "don't operate without operator" in {
      Post(uri).withEntity(entity) ~> seal(route) ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "don't operate without entity" in {
      Post(uri) ~> seal(route) ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    val request = Post(uri).withHeaders(RequestIdentityHeaders)

    "successfully operate correct post query" in {
      request.withEntity(entity) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK
        verify(asyncService)
          .upsert(Tariff)
      }
    }
  }

  "GET /" should {
    import ru.auto.salesman.environment.IsoDateFormatter
    val time = Tariff.from.plusDays(1).withTimeAtStartOfDay()
    val timeString = IsoDateFormatter.print(time)
    val uri = s"/?clientId=$ClientId"
    val uriTime = s"$uri&time=$timeString"

    "don't operate without operator" in {
      Get(uriTime) ~> seal(route) ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "operate without time parameter" in {
      Get(uri).withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[TariffView]
        response.asModel shouldBe Tariff
      }
    }

    "successfully operate correct get query" in {
      Get(uriTime).withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK
        verify(asyncService)
          .get(Actual(ClientId, time))
        val response = responseAs[TariffView]
        response.asModel shouldBe Tariff
      }
    }
  }

  "DELETE /" should {
    import ru.auto.salesman.environment.IsoDateFormatter
    val time = Tariff.from.plusDays(1).withTimeAtStartOfDay()
    val timeString = IsoDateFormatter.print(time)
    val uri = s"/?clientId=$ClientId"
    val uriTime = s"$uri&time=$timeString"

    "don't operate without operator" in {
      Delete(uriTime) ~> seal(route) ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "successfully operate correct delete query" in {
      Delete(uriTime).withHeaders(RequestIdentityHeaders) ~> seal(
        route
      ) ~> check {
        status shouldBe StatusCodes.OK
        verify(asyncService)
          .delete(Actual(ClientId, time))
      }
    }

    "operate without time parameter" in {
      Delete(uri).withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  implicit def actorRefFactory = system
}
