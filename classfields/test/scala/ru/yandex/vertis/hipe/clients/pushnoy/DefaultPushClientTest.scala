package ru.yandex.vertis.hipe.clients.pushnoy

import com.netaporter.uri.dsl._
import org.joda.time.{DateTime, LocalDate, LocalTime}
import org.junit.runner.RunWith
import org.scalatest.OptionValues
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.vertis.hipe.clients.{HttpClientSpec, MockedHttpClient}
import ru.yandex.vertis.hipe.pushes.PublishTheDraft
import ru.yandex.vertis.hipe.util.PushId
import ru.yandex.vertis.hipe.util.http.exceptions.DeviceNotFoundException

/**
  * Created by andrey on 10/2/17.
  */
@RunWith(classOf[JUnitRunner])
class DefaultPushClientTest extends HttpClientSpec with MockedHttpClient with PropertyChecks with OptionValues {
  private val pushClient = new DefaultPushClient(http)

  "pushClient" should {
    "return push history" in {
      val uuid = "uuid"
      val pushId = PushId.SeveralDaysInactivity
      http.expect("GET", "/api/v1/auto/device/uuid/push/severalDaysInactivity/history")
      http.respondWithJson(200, """{"name":"severalDaysInactivity","history":[1506499391268]}""")

      val result = pushClient.getPushHistory(uuid, pushId).futureValue
      result.length shouldBe 1
      result.head shouldBe new DateTime(1506499391268L)
    }

    "throw DeviceNotFoundException on unknown device response" in {
      val uuid = "uuid"
      val pushId = PushId.SeveralDaysInactivity
      http.expect("GET", "/api/v1/auto/device/uuid/push/severalDaysInactivity/history")
      http.respondWithJson(404, "No uuid device found")

      intercept[DeviceNotFoundException] {
        pushClient.getPushHistory(uuid, pushId).futureValue
      }
    }

    "send push v2" in {
      val uuid = "uuid"
      val pushInfo = PublishTheDraft(uuid)

      val localDate = LocalDate.now()
      val deliverFrom = localDate.toLocalDateTime(new LocalTime(10, 0))
      val deliverTo = localDate.toLocalDateTime(new LocalTime(22, 0))

      http.expect(
        "POST",
        s"/api/v2/auto/device/uuid/send" ? (
          "delivery_name" -> pushInfo.deliveryName &
            "deliver_from" -> deliverFrom &
            "deliver_to" -> deliverTo
        )
      )
      http.expectProto(pushInfo.toRequest)
      http.respondWithStatus(200)

      pushClient.sendPush(pushInfo)
    }
  }
}
