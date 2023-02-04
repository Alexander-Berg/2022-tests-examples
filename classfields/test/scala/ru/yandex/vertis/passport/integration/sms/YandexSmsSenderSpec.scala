package ru.yandex.vertis.passport.integration.sms

import org.scalatest.FreeSpec
import ru.yandex.vertis.passport.integration.sms.YandexSmsSender.{SendSmsException, SmsLimitExceeded}
import ru.yandex.vertis.passport.test.SpecBase

import scala.util.Success

/**
  * Test for response parsing in [[YandexSmsSender]]
  *
  * @author zvez
  */
class YandexSmsSenderSpec extends FreeSpec with SpecBase {

  "YandexSmsSender response parsing: " - {
    "ok response" in {
      val xml =
        """<?xml version="1.0" encoding="windows-1251"?>
           |<doc>
           |    <message-sent id="127000000003456" />
           |</doc>""".stripMargin

      YandexSmsSender.parseResponse(xml) shouldBe Success("127000000003456")
    }

    "error response" in {
      val xml =
        """<?xml version="1.0" encoding="windows-1251"?>
          |<doc>
          |    <error>User does not have an active phone to recieve messages</error>
          |    <errorcode>NOCURRENT</errorcode>
          |</doc>""".stripMargin

      val ex = YandexSmsSender.parseResponse(xml).failed.get
      ex shouldBe a[SendSmsException]
      ex.getMessage shouldBe "NOCURRENT: User does not have an active phone to recieve messages"
    }

    "error response: LIMITEXCEEDED" in {
      val xml =
        """<?xml version="1.0" encoding="windows-1251"?>
          |<doc>
          |    <error>Sms limit for this phone exceeded</error>
          |    <errorcode>LIMITEXCEEDED</errorcode>
          |</doc>""".stripMargin

      val ex = YandexSmsSender.parseResponse(xml).failed.get
      ex shouldBe a[SmsLimitExceeded]
    }
  }

}
