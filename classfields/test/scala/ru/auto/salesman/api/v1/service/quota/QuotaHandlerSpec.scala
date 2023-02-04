package ru.auto.salesman.api.v1.service.quota

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat._
import org.mockito.Mockito.verify
import org.scalatest.OneInstancePerTest
import ru.auto.salesman.api.DeprecatedMockitoRoutingSpec
import ru.auto.salesman.api.view.QuotaRequestView
import ru.auto.salesman.dao.QuotaRequestDao.{Actual, AddedInPeriod}
import ru.auto.salesman.model.{QuotaEntities, QuotaRequest}
import ru.auto.salesman.service.async.{AsyncQuotaRequestService, AsyncQuotaService}
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.util.DateTimeInterval
import spray.json._

import scala.concurrent.Future

class QuotaHandlerSpec extends DeprecatedMockitoRoutingSpec with OneInstancePerTest {

  private val ClientId = 1111
  private val QuotaRequest = QuotaRequestGen.next.copy(clientId = ClientId)

  private val asyncService = {
    val m = mock[AsyncQuotaRequestService]
    when(m.add(?[List[QuotaRequest]])(?))
      .thenReturn(Future.successful(()))
    when(m.get(?)(?))
      .thenReturn(Future.successful(List(QuotaRequest)))
    m
  }

  private val AsyncQuotaService = mock[AsyncQuotaService]

  private val route = new QuotaHandler(asyncService, AsyncQuotaService).route

  "POST /" should {
    val uri = "/"
    val entity = HttpEntity(
      ContentTypes.`application/json`,
      Iterable(QuotaRequestView.asView(QuotaRequest)).toJson.compactPrint
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
      }
    }
  }

  "GET /" should {
    import ru.auto.salesman.environment.IsoDateFormatter
    val time = QuotaRequest.from.plusDays(1).withTimeAtStartOfDay()
    val timeString = IsoDateFormatter.print(time)
    val uri = s"/?clientId=$ClientId"

    "don't operate without operator" in {
      Get(s"$uri&time=$timeString") ~> seal(route) ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }
    "operate without time parameter" in {
      Get(uri).withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[Array[QuotaRequestView]]
        response.length shouldBe 1
        response.head.asModel
          .copy(from = QuotaRequest.from) shouldBe QuotaRequest
      }
    }

    "successfully operate correct get query" in {

      Get(s"$uri&time=$timeString")
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK
        verify(asyncService)
          .get(Actual(ClientId, time, allowDisabled = false))(operatorContext)
        val response = responseAs[Array[QuotaRequestView]]
        response.length shouldBe 1
        response.head.asModel
          .copy(from = QuotaRequest.from) shouldBe QuotaRequest
      }
    }
  }

  "GET /requests" should {

    val from = QuotaRequest.from
    val to = QuotaRequest.from.plusDays(1).withTimeAtStartOfDay()

    val fromStr = formatTime(from)
    val toStr = formatTime(to)

    val uri = "/requests"

    "successfully operate correct get query" in {

      Get(s"$uri?from=$fromStr&to=$toStr&entity=dealer")
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK

        verify(asyncService)
          .get(AddedInPeriod(DateTimeInterval(from, to), QuotaEntities.Dealer))(
            operatorContext
          )

        val response = responseAs[Array[QuotaRequestView]]
        response.length shouldBe 1
        response.head.asModel
          .copy(from = QuotaRequest.from) shouldBe QuotaRequest
      }
    }

    "successfully operate without dateTo" in {

      Get(s"$uri?from=$fromStr&entity=dealer")
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK

        verify(asyncService).get(any[AddedInPeriod])(eq(operatorContext))

        val response = responseAs[Array[QuotaRequestView]]
        response.length shouldBe 1
        response.head.asModel
          .copy(from = QuotaRequest.from) shouldBe QuotaRequest
      }
    }

    "do not operate with incorrect entity" in {
      Get(s"$uri?from=$fromStr&entity=e")
        .withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }
  }

  def formatTime(dateTime: DateTime): String =
    dateHourMinuteSecondMillis.print(dateTime)

  implicit def actorRefFactory: ActorSystem = system
}
