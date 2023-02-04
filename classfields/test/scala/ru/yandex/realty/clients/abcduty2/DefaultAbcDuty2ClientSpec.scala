package ru.yandex.realty.clients.abcduty2

import java.time._

import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.StatusCodes
import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.clients.abcduty2.model.ListShiftsResponse.{ShiftInfo, StaffInfo}
import ru.yandex.realty.clients.abcduty2.model.{ListShiftsRequest, ListShiftsResponse}
import ru.yandex.realty.http.HttpClientMock
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.TimeUtils

// scalastyle:off
@RunWith(classOf[JUnitRunner])
class DefaultAbcDuty2ClientSpec extends SpecBase with HttpClientMock with ScalaFutures {

  implicit val trace: Traced = Traced.empty

  "AbcDuty2Client.shifts" should {
    "return correct mapped json data for expected url" in {
      val request1 = ListShiftsRequest(
        scheduleId = Some(1234),
        startLess = Some(ZonedDateTime.of(2022, 6, 28, 12, 31, 25, 5, TimeUtils.MSK).toInstant),
        endGreater = Some(ZonedDateTime.of(2022, 6, 29, 13, 31, 25, 0, TimeUtils.MSK).toInstant),
        pageSize = Some(100)
      )
      val eq = "%3D"
      val expectedUrl =
        s"/api/watcher/v1/shift/?filter=schedule_id${eq}1234,start__lt${eq}2022-06-28T09:31:25.000000005Z,end__gt${eq}2022-06-29T10:31:25Z&page_size=100"

      val abcClient = new DefaultAbcDuty2Client(httpService)

      httpClient.expect(GET, expectedUrl)
      httpClient.respondWith(StatusCodes.OK, responseJson)

      val result: ListShiftsResponse = abcClient.listShifts(request1).futureValue

      result shouldEqual responseScala
    }

  }

  private val responseJson: String =
    s"""
           |{
           |  "result": [
           |    {
           |      "slot_id": 2057,
           |      "start": "2022-06-28T15:00:00+03:00",
           |      "end": "2022-06-28T16:00:00+03:00",
           |      "status": "active",
           |      "approved": true,
           |      "approved_by_id": 60325,
           |      "approved_at": "2022-06-28T13:17:00+03:00",
           |      "empty": false,
           |      "replacement_for_id": 972239,
           |      "schedule_id": 30529,
           |      "staff_id": 60325,
           |      "is_primary": true,
           |      "id": 1015913,
           |      "staff": {
           |        "id": 60325,
           |        "staff_id": 60325,
           |        "uid": 1120000000158634,
           |        "login": "lyubortk",
           |        "first_name": "Константин",
           |        "first_name_en": "Konstantin",
           |        "last_name": "Люборт",
           |        "last_name_en": "Lyubort",
           |        "is_robot": false,
           |        "telegram_account": "lyubortk"
           |      },
           |      "slot": {
           |        "is_primary": true,
           |        "show_in_staff": false
           |      },
           |      "replacement_for": {
           |        "slot_id": 2057,
           |        "start": "2022-06-24T00:00:00+03:00",
           |        "end": "2022-06-29T00:00:00+03:00",
           |        "status": "scheduled",
           |        "approved": true,
           |        "approved_by_id": 60325,
           |        "approved_at": "2022-06-28T13:23:00+03:00",
           |        "empty": false,
           |        "replacement_for_id": null,
           |        "schedule_id": 30529,
           |        "staff_id": 287233,
           |        "is_primary": true,
           |        "id": 972239
           |      }
           |    },
           |    {
           |      "slot_id": 2058,
           |      "start": "2022-06-24T00:00:00+03:00",
           |      "end": "2022-06-29T00:00:00+03:00",
           |      "status": "active",
           |      "approved": true,
           |      "approved_by_id": 270084,
           |      "approved_at": "2022-06-24T00:02:00+03:00",
           |      "empty": false,
           |      "replacement_for_id": null,
           |      "schedule_id": 30529,
           |      "staff_id": 102026,
           |      "is_primary": true,
           |      "id": 972240,
           |      "staff": {
           |        "id": 102026,
           |        "staff_id": 67026,
           |        "uid": 1120000000177585,
           |        "login": "zhukovrv",
           |        "first_name": "Роман",
           |        "first_name_en": "Roman",
           |        "last_name": "Жуков",
           |        "last_name_en": "Zhukov",
           |        "is_robot": false,
           |        "telegram_account": "ZhukovRoman"
           |      },
           |      "slot": {
           |        "is_primary": true,
           |        "show_in_staff": false
           |      },
           |      "replacement_for": null
           |    }
           |  ],
           |  "next": null,
           |  "prev": null
           |}
      """.stripMargin

  private val responseScala = ListShiftsResponse(
    result = List(
      ShiftInfo(
        slotId = 2057,
        start = OffsetDateTime.of(LocalDate.of(2022, 6, 28), LocalTime.of(15, 0), ZoneOffset.ofHours(3)),
        end = OffsetDateTime.of(LocalDate.of(2022, 6, 28), LocalTime.of(16, 0), ZoneOffset.ofHours(3)),
        scheduleId = 30529,
        staff = StaffInfo(login = "lyubortk", isRobot = false)
      ),
      ShiftInfo(
        slotId = 2058,
        start = OffsetDateTime.of(LocalDate.of(2022, 6, 24), LocalTime.of(0, 0), ZoneOffset.ofHours(3)),
        end = OffsetDateTime.of(LocalDate.of(2022, 6, 29), LocalTime.of(0, 0), ZoneOffset.ofHours(3)),
        scheduleId = 30529,
        staff = StaffInfo(login = "zhukovrv", isRobot = false)
      )
    )
  )
}
