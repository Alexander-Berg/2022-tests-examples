package ru.yandex.vertis.billing.balance.xmlrpc

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.balance.model.{paymentRequestDatePattern, NotifyClient}
import ru.yandex.vertis.billing.balance.xmlrpc.parse.Parsers
import ru.yandex.vertis.billing.balance.xmlrpc.serialize.Deserializers

import scala.util.Success

/**
  * @author ruslansd
  */
class DeserializeSpec extends AnyWordSpec with Matchers {

  "Deserializers" should {
    "parse notifyClient" in {
      val notification =
        scala.xml.XML.load(getClass.getResourceAsStream("/scenario/client/015_rs_Balance2.NotifyClient2.xml"))
      val expectedNotification = NotifyClient(
        4021792,
        "20130712141215818",
        230,
        60,
        Some(paymentRequestDatePattern.parseLocalDate("2013-07-15")),
        overdraftBan = false,
        "EUR",
        resident = false,
        migrateDone = Some(false)
      )
      Deserializers.safeHeadNotifyClient(Parsers.asMethodCall(notification)) shouldBe Success(expectedNotification)
    }
  }

}
