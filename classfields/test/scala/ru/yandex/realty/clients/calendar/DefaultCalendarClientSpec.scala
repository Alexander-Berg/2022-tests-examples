package ru.yandex.realty.clients.calendar

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes
import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.time.{Millis, Seconds, Span}
import ru.yandex.realty.SpecBase
import ru.yandex.realty.clients.blackbox.BlackboxClient
import ru.yandex.realty.clients.blackbox.model.{Karma, KarmaStatus, OAuth, OAuthValidResponse, Status, Uid}
import ru.yandex.realty.clients.calendar.model.{
  Availability,
  BusyOverlapResponse,
  CreateEventBody,
  CreateEventRequest,
  CreateEventResponse,
  CreateEventSuccessResponse
}
import ru.yandex.realty.http.HttpClientMock
import ru.yandex.realty.request.RequestImpl
import ru.yandex.realty.tracing.Traced

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.{ExecutionContext, Future}

@RunWith(classOf[JUnitRunner])
class DefaultCalendarClientSpec
  extends SpecBase
  with HttpClientMock
  with GeneratorDrivenPropertyChecks
  with ScalaFutures {

  "CalendarClient.createEvent" should {
    "return correct mapped json response with status=ok for expected url" in new Wiring with Data {
      httpClient.expect(POST, expectedUrl)
      httpClient.respondWith(StatusCodes.OK, sampleRawSuccessJsonResponse)

      (mockBlackboxClient
        .oauth(_: String, _: String, _: Map[String, String])(_: Traced))
        .expects(sampleToken, *, *, *)
        .returning(Future.successful(sampleOAuthValidResponse))

      val result: CreateEventResponse =
        calendarClient.createEvent(sampleUid, sampleCreateEventRequest, sampleToken).futureValue

      result shouldEqual expectedCreateEventSuccessResponse
    }

    "return correct mapped json response with status=overlap-busy for expected url" in new Wiring with Data {
      httpClient.expect(POST, expectedUrl)
      httpClient.respondWith(StatusCodes.OK, sampleRawBusyOverlapJsonResponse)

      (mockBlackboxClient
        .oauth(_: String, _: String, _: Map[String, String])(_: Traced))
        .expects(sampleToken, *, *, *)
        .returning(Future.successful(sampleOAuthValidResponse))

      val result: CreateEventResponse =
        calendarClient.createEvent(sampleUid, sampleCreateEventRequest, sampleToken).futureValue

      result shouldEqual expectedCreateEventBusyOverlapResponse

    }

  }

  implicit override def patienceConfig: PatienceConfig = PatienceConfig(Span(2, Seconds), Span(500, Millis))

  trait Wiring {
    val mockBlackboxClient: BlackboxClient = mock[BlackboxClient]
    val calendarClient = new DefaultCalendarClient(httpService, mockBlackboxClient)(ExecutionContext.global)
    implicit val request: RequestImpl = {
      val request = new RequestImpl
      request.setIp("127.0.0.1")
      request
    }

    implicit val trace: Traced = Traced.empty
  }

  trait Data {
    this: Wiring =>

    val sampleInstantString1: String = "2022-01-25T10:20:30Z"

    val sampleLocalDateStr = "2022-01-25"
    val sampleLocalDate: LocalDate = LocalDate.parse(sampleLocalDateStr)
    val sampleUid: Long = 10015L
    val sampleToken: String = "sample-token-12345"

    val sampleOAuthValidResponse: OAuthValidResponse = OAuthValidResponse(
      oauth = OAuth(
        uid = "",
        tokenId = "",
        deviceId = "",
        deviceName = "",
        scope = "",
        ctime = LocalDateTime.now(),
        issueTime = LocalDateTime.now(),
        isTtlRefreshable = true,
        clientId = "",
        clientName = "",
        clientIcon = "",
        clientHomepage = "",
        clientCTime = LocalDateTime.now(),
        clientIsYandex = true,
        xtokenId = "",
        meta = ""
      ),
      status = Status("", 0),
      error = "",
      uid = Uid(""),
      login = "",
      havePassword = true,
      haveHint = true,
      karma = Karma(0),
      karmaStatus = KarmaStatus(0),
      userTicket = "",
      connectionId = ""
    )

    val sampleCreateEventRequest: CreateEventRequest = CreateEventRequest(
      uid = Some(sampleUid),
      body = CreateEventBody(
        `type` = Some("user"),
        startTs = Some(sampleInstantString1),
        endTs = Some(sampleInstantString1),
        name = Some("sample-name"),
        description = Some("sample-description"),
        availability = Some(Availability.Busy),
        layerId = Some("1000"),
        attendees = Nil,
        participantsCanEdit = Some(true)
      )
    )

    val expectedCreateEventSuccessResponse: CreateEventSuccessResponse = CreateEventSuccessResponse(
      sequence = 0,
      showEventId = 127,
      showDate = sampleLocalDate,
      externalIds = Seq("XXX", "YYY")
    )

    val expectedCreateEventBusyOverlapResponse: BusyOverlapResponse = BusyOverlapResponse(
      resourceEmail = "sample@email.ru",
      instanceStart = LocalDateTime.parse("2013-05-31T22:30:00"),
      overlapStart = LocalDateTime.parse("2013-05-31T22:38:00")
    )
    val expectedUrl: String = s"/internal/create-event?uid=$sampleUid"

    val sampleRawSuccessJsonResponse: String =
      s"""{
         |    "sequence": 0,
         |    "showEventId": 127,
         |    "showDate": "2022-01-25",
         |    "externalIds": ["XXX", "YYY"],
         |    "status": "ok"
         |}""".stripMargin

    val sampleRawBusyOverlapJsonResponse: String =
      s"""{
         |    "status": "busy-overlap",
         |    "resourceEmail": "sample@email.ru",
         |    "instanceStart": "2013-05-31T22:30:00",
         |    "overlapStart": "2013-05-31T22:38:00"
         |}""".stripMargin
  }
}
