package ru.auto.api.services.pushnoy.apns

import play.api.libs.json.Json
import ru.auto.api.BaseSpec

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 03.04.18
  */
class AlertSpec extends BaseSpec {
  "Alert" should {
    "serialize to json" in {
      val alert1: Alert = Alert.Simple("aaa")
      Json.stringify(Json.toJson(alert1)) shouldBe "\"aaa\""

      val alert2: Alert = Alert.Compound(title = Some("aaa"), body = "bb")
      Json.stringify(Json.toJson(alert2)) shouldBe "{\"title\":\"aaa\",\"body\":\"bb\"}"
    }
  }
}
