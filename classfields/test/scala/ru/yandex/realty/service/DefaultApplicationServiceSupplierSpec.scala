package ru.yandex.realty.service

import com.typesafe.config.{Config, ConfigFactory}
import ru.yandex.realty.SpecBase
import ru.yandex.realty.application.ng.TypesafeConfigProvider
import ru.yandex.realty.auth.Application

class DefaultApplicationServiceSupplierSpec extends SpecBase {
  val config = ConfigFactory.parseString("""
      |authorization {
      |  self-token = test-0c879486712c654ab7b812c48fd73412
      |  applications {
      |    nodejs = {
      |    }
      |    nginx = {
      |      token = nginx-f92ea1839dc16d7396db358365da7066
      |    }
      |    vos = {
      |      tvmId = 12345
      |    }
      |    moderation = {
      |      token = moderation-9d29c98439b35eedf26ea83b1133cf31
      |      tvmId = 3245
      |    }
      |  }
      |}
      |""".stripMargin)

  val testObject = new TypesafeConfigProvider with DefaultApplicationServiceSupplier {
    override def typesafeConfig: Config = config
  }

  "DefaultApplicationServiceSupplier" should {
    "parce self-token" in {
      testObject.selfToken should be("test-0c879486712c654ab7b812c48fd73412")
    }
    "parce app with only token" in {
      val app = testObject.applicationService.getApplicationByToken("nginx-f92ea1839dc16d7396db358365da7066")
      app.get should be(Application.Nginx)
    }
    "parce app with only tvmId" in {
      val app = testObject.applicationService.getApplicationByTvm(12345)
      app.get should be(Application.Vos)
    }
    "parce full app" in {
      val app1 = testObject.applicationService.getApplicationByToken("moderation-9d29c98439b35eedf26ea83b1133cf31")
      app1.get should be(Application.Moderation)
      val app2 = testObject.applicationService.getApplicationByTvm(3245)
      app2.get should be(Application.Moderation)
    }

  }

}
