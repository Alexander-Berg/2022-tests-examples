package ru.yandex.vos2.services.telephony.model

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}
import play.api.libs.json.Json

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 02.11.16
  */
@RunWith(classOf[JUnitRunner])
class PhoneRedirectTest extends FunSuite with Matchers {

  test("parse PhoneRedirect from json") {
    val json = """{
                 |  "source": "+79175446755",
                 |  "createTime": "2016-11-02T12:34:13.578+03:00",
                 |  "deadline": "2016-11-02T13:40:45.839+03:00",
                 |  "id": "3kJPPqQ5gmc",
                 |  "target": "+79671686464",
                 |  "objectId": "1024459451"
                 |}""".stripMargin

    Json.parse(json).as[PhoneRedirect] shouldBe PhoneRedirect(
      id = "3kJPPqQ5gmc",
      objectId = "1024459451",
      createTime = DateTime.parse("2016-11-02T12:34:13.578+03:00"),
      deadline = DateTime.parse("2016-11-02T13:40:45.839+03:00"),
      source = "+79175446755",
      target = "+79671686464"
    )
  }
}
