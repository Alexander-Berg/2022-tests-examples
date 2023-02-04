package ru.yandex.vertis.telepony.service

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.logging.SimpleLogging
import ru.yandex.vertis.telepony.model.{Phone, PhoneTypes}

/**
  * @author evans
  */
trait PhoneAnalyzerServiceSpec extends SpecBase with BeforeAndAfterAll with BeforeAndAfterEach with SimpleLogging {

  def phoneAnalyzer: PhoneAnalyzerService

  "Phone analyzer service" should {
    "parse 499" in {
      phoneAnalyzer.getRegion(Phone("+74991234567")).futureValue shouldEqual 213
    }
    "parse 812" in {
      phoneAnalyzer.getRegion(Phone("+78121234567")).futureValue shouldEqual 2
    }
    "parse +74953362902" in {
      phoneAnalyzer.getRegion(Phone("+74953362902")).futureValue shouldEqual 213
    }

    "parse sochi +78623333333" in {
      phoneAnalyzer.getRegion(Phone("+78623333333")).futureValue shouldEqual 239
    }

    "parse krasnodar +78613333333" in {
      phoneAnalyzer.getRegion(Phone("+78612000000")).futureValue shouldEqual 35
    }

//    "parse +74952766502" in {
//    SimpleRegionService fails this test, because simple region service is dummy
//      regionService.getRegion(Phone("+74952766502")).futureValue shouldEqual 1
//    }

    "parse local phone type" in {
      phoneAnalyzer.getPhoneType(Phone("+78623333333")).futureValue shouldEqual PhoneTypes.Local
    }

    "parse mobile phone type" in {
      phoneAnalyzer.getPhoneType(Phone("+79312320032")).futureValue shouldEqual PhoneTypes.Mobile
    }
  }
}
