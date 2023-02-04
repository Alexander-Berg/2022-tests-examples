package ru.yandex.vos2.services.telephony.model

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 02.11.16
  */
@RunWith(classOf[JUnitRunner])
class PhoneCallTest extends AnyFunSuite with Matchers {

  test("parse PhoneCall calls from json") {
    val json =
      """{
        |  "duration": 13,
        |  "source": "4959743581",
        |  "externalId": "4193138125",
        |  "talkDuration": 0,
        |  "createTime": "2016-09-07T19:22:37.508+03:00",
        |  "id": "jRiBUcidGBQ",
        |  "proxy": "+79851542575",
        |  "target": "+79264247766",
        |  "objectId": "1043780190-c84f32",
        |  "time": "2016-09-07T18:27:09.000+03:00",
        |  "redirectId": "odlbHlkZHuM"
        |}""".stripMargin

    val parsed = Json.parse(json).as[PhoneCall]
    val expected = PhoneCall(
      id = "jRiBUcidGBQ",
      redirectId = "odlbHlkZHuM",
      objectId = "1043780190-c84f32",
      createTime = DateTime.parse("2016-09-07T19:22:37.508+03:00"),
      externalId = "4193138125",
      source = Some("4959743581"),
      proxy = "+79851542575",
      target = "+79264247766",
      time = DateTime.parse("2016-09-07T18:27:09.000+03:00"),
      duration = 13,
      talkDuration = 0,
      recordId = None
    )
    parsed shouldBe expected
  }

  test("parse PhoneCall calls from json without source") {
    val json =
      """{
        |  "duration": 13,
        |  "externalId": "4193138125",
        |  "talkDuration": 0,
        |  "createTime": "2016-09-07T19:22:37.508+03:00",
        |  "id": "jRiBUcidGBQ",
        |  "proxy": "+79851542575",
        |  "target": "+79264247766",
        |  "objectId": "1043780190-c84f32",
        |  "time": "2016-09-07T18:27:09.000+03:00",
        |  "redirectId": "odlbHlkZHuM"
        |}""".stripMargin

    Json.parse(json).as[PhoneCall] shouldBe PhoneCall(
      id = "jRiBUcidGBQ",
      redirectId = "odlbHlkZHuM",
      objectId = "1043780190-c84f32",
      createTime = DateTime.parse("2016-09-07T19:22:37.508+03:00"),
      externalId = "4193138125",
      source = None,
      proxy = "+79851542575",
      target = "+79264247766",
      time = DateTime.parse("2016-09-07T18:27:09.000+03:00"),
      duration = 13,
      talkDuration = 0,
      recordId = None
    )
  }
}
