package ru.yandex.vertis.hipe.clients.pushnoy

import org.joda.time.{LocalDate, LocalTime}
import ru.yandex.pushnoy.PushRequestModel.DealerPhoneCallTemplate.TextType
import ru.yandex.vertis.hipe.clients.{HttpClientSpec, MockedHttpClient}
import ru.yandex.vertis.hipe.pushes.dealer.{CallDealerBase, CallDealerFromContactsShow}

class LocalDelayedPushHistoryTest extends HttpClientSpec with MockedHttpClient {
  private val pushClient = new DefaultPushClient(http) with LocalDelayedPushHistory

  "localDelayedPushHistory" should {
    "save pushes in local history" in {
      val pushInfo = CallDealerFromContactsShow(
        "someUUID",
        "OfferId",
        "rid",
        "dealerId",
        Some(
          CallDealerBase.Target(
            "offerId",
            "mark",
            "model",
            2.4,
            "salon",
            Map.empty,
            TextType.LOWER_PRICE.toString,
            "NEW"
          )
        )
      )

      val localDate = LocalDate.now()
      val deliverFrom = localDate.toLocalDateTime(new LocalTime(10, 0))
      val deliverTo = localDate.toLocalDateTime(new LocalTime(22, 0))

      http.expect(
        "POST",
        s"/api/v2/auto/device/${pushInfo.uuid}/template/send?delivery_name=personalRecommendations&deliver_from=$deliverFrom&deliver_to=$deliverTo"
      )
      http.respondWithStatus(200)
      pushClient.sendPush(pushInfo)

      pushClient.delayedPushHistory should have size 1
      pushClient.delayedPushHistory.get(
        pushInfo.uuid -> pushInfo.pushId.toString
      ) should not be null
    }
  }

}
