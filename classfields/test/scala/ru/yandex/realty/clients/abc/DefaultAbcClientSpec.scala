package ru.yandex.realty.clients.abc

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes
import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.realty.clients.abc.model.{AbcPerson, AbcSchedule, AbcShift, GetShiftsRequest, GetShiftsResponse}
import ru.yandex.realty.http.HttpClientMock
import ru.yandex.realty.request.RequestImpl
import ru.yandex.realty.tracing.Traced

import java.time.{LocalDate, LocalDateTime, LocalTime, ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter

// scalastyle:off
@RunWith(classOf[JUnitRunner])
class DefaultAbcClientSpec extends SpecBase with HttpClientMock with GeneratorDrivenPropertyChecks with ScalaFutures {

  "AbcClient.shifts" should {
    "return correct mapped json data for expected url" in new Wiring with Data {

      httpClient.expect(GET, expectedUrl)
      httpClient.respondWith(StatusCodes.OK, sampleRawJsonResponse)

      val result: GetShiftsResponse = abcClient.shifts(sampleRequest).futureValue

      result shouldEqual expectedShiftsResponse
    }

  }

  trait Wiring {
    val abcClient = new DefaultAbcClient(httpService)
    implicit val request: RequestImpl = {
      val request = new RequestImpl
      request.setIp("127.0.0.1")
      request
    }

    implicit val trace: Traced = Traced.empty
    val DateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val ZonedDateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
  }

  trait Data {
    this: Wiring =>

    val sampleShift1: AbcShift = {
      val start = LocalDate.of(2021, 12, 10)
      val end = start.plusDays(1)
      val msk = ZoneOffset.ofHours(3)
      val startDateTime = ZonedDateTime.of(LocalDateTime.of(start, LocalTime.of(0, 0, 0)), msk)
      val endDateTime = ZonedDateTime.of(LocalDateTime.of(end, LocalTime.of(0, 0, 0)), msk)

      AbcShift(
        id = Some(1000001),
        person = Some(
          AbcPerson(
            login = Some("abcdefgh1"),
            uid = Some("7770000000111111L")
          )
        ),
        schedule = Some(
          AbcSchedule(
            id = Some(1001),
            name = Some("name-1")
          )
        ),
        isApproved = Some(true),
        start = Some(start),
        end = Some(end),
        startDatetime = Some(startDateTime),
        endDatetime = Some(endDateTime)
      )
    }

    val sampleShift2: AbcShift = {
      val start = LocalDate.of(2021, 12, 12)
      val end = start.plusDays(1)
      val msk = ZoneOffset.ofHours(3)
      val startDateTime = ZonedDateTime.of(LocalDateTime.of(start, LocalTime.of(0, 0, 0)), msk)
      val endDateTime = ZonedDateTime.of(LocalDateTime.of(end, LocalTime.of(0, 0, 0)), msk)

      AbcShift(
        id = Some(1000002),
        person = Some(
          AbcPerson(
            login = Some("abcdefgh2"),
            uid = Some("7770000000222222L")
          )
        ),
        schedule = Some(
          AbcSchedule(
            id = Some(1002),
            name = Some("name-2")
          )
        ),
        isApproved = Some(false),
        start = Some(start),
        end = Some(end),
        startDatetime = Some(startDateTime),
        endDatetime = Some(endDateTime)
      )
    }

    val expectedShiftsResponse: GetShiftsResponse =
      GetShiftsResponse(
        count = None,
        next = None,
        previous = None,
        results = Seq(
          sampleShift1,
          sampleShift2
        )
      )

    val sampleRequest: GetShiftsRequest = GetShiftsRequest(
      dateFrom = LocalDate.now(),
      dateTo = LocalDate.now().plusDays(2),
      serviceSlug = Some("some_service_slug"),
      scheduleSlug = Some("some_schedule_slug"),
      pageSize = Some(50)
    )

    val expectedUrl =
      s"/api/v4/duty/shifts/?service__slug=${sampleRequest.serviceSlug.get}&schedule__slug=${sampleRequest.scheduleSlug.get}&date_from=${sampleRequest.dateFrom
        .format(DateFormat)}&date_to=${sampleRequest.dateTo.format(DateFormat)}&page_size=${sampleRequest.pageSize.get}"

    val sampleRawJsonResponse: String =
      s"""
         |{
         |  "results": [
         |    {
         |      "id": ${sampleShift1.id.get},
         |      "person": {
         |        "login": "${sampleShift1.person.get.login.get}",
         |        "uid": "${sampleShift1.person.get.uid.get}"
         |      },
         |      "schedule": {
         |        "id": ${sampleShift1.schedule.get.id.get},
         |        "name": "${sampleShift1.schedule.get.name.get}"
         |      },
         |      "is_approved": ${sampleShift1.isApproved.get},
         |      "start": "${sampleShift1.start.get.format(DateFormat)}",
         |      "end": "${sampleShift1.end.get.format(DateFormat)}",
         |      "start_datetime": "${sampleShift1.startDatetime.get.format(ZonedDateTimeFormat)}",
         |      "end_datetime": "${sampleShift1.endDatetime.get.format(ZonedDateTimeFormat)}"
         |    },
         |    {
         |      "id": ${sampleShift2.id.get},
         |      "person": {
         |        "login": "${sampleShift2.person.get.login.get}",
         |        "uid": "${sampleShift2.person.get.uid.get}"
         |      },
         |      "schedule": {
         |        "id": ${sampleShift2.schedule.get.id.get},
         |        "name": "${sampleShift2.schedule.get.name.get}"
         |      },
         |      "is_approved": ${sampleShift2.isApproved.get},
         |      "start": "${sampleShift2.start.get.format(DateFormat)}",
         |      "end": "${sampleShift2.end.get.format(DateFormat)}",
         |      "start_datetime": "${sampleShift2.startDatetime.get.format(ZonedDateTimeFormat)}",
         |      "end_datetime": "${sampleShift2.endDatetime.get.format(ZonedDateTimeFormat)}"
         |    }
         |  ]
         |}
         |""".stripMargin
  }
}
//scalastyle:on
