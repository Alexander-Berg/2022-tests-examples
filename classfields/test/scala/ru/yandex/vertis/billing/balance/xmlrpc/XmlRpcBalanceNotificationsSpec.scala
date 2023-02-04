package ru.yandex.vertis.billing.balance.xmlrpc

import com.google.common.base.Charsets
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.billing.balance.xmlrpc.parse.Parsers
import ru.yandex.vertis.billing.balance.xmlrpc.serialize.Deserializers

/**
  * Created by IntelliJ IDEA.
  * User: alesavin
  * Date: 17.09.14
  * Time: 16:09
  */
class XmlRpcBalanceNotificationsSpec extends AnyFlatSpec with Matchers {

  import Deserializers._

  "BalanceNotification" should "be parsed" in {
    val notify =
      XmlRpcBalanceNotifications.parseNotifyOrder(read("/balance/xmlrpc_rq_BalanceNotification.xml"))
    assert(notify.isSuccess)
  }

  it should "be parsed 2" in {
    val notify =
      XmlRpcBalanceNotifications.parseNotifyOrder(read("/balance/xmlrpc_rq_BalanceNotification_2.xml"))
    assert(notify.isSuccess)
  }

  it should "fail on unformat data" in {
    val notify =
      XmlRpcBalanceNotifications.parseNotifyOrder(read("/balance/xmlrpc_rq_CreateClient.xml"))
    assert(notify.isFailure)
  }

  it should "fail on unformat data 2" in {
    val notify =
      XmlRpcBalanceNotifications.parseNotifyOrder(read("/balance/xmlrpc_rs_RequestPayment.xml"))
    assert(notify.isFailure)
  }

  it should "return success" in {
    val success = XmlRpcBalanceNotifications.getSuccess()
//    print(success)
    assert(success.contains("methodResponse"))
    assert(success.contains(Deserializers.SuccessValue))

    val parsed = Parsers.asMethodResponse(scala.xml.XML.loadString(success))
    asResponseArray(parsed, codeStatusParser)
  }

  it should "return fault" in {
    val fault = XmlRpcBalanceNotifications.getFault(1, "Message")
//    print(fault)
    assert(fault.contains("methodResponse"))
    assert(fault.contains("Message"))

    val parsed = Parsers.asMethodResponse(scala.xml.XML.loadString(fault))
    intercept[BalanceXmlRpcFaultException] {
      asResponseArray(parsed, codeStatusParser)
    }
  }

  private def read(path: String) =
    scala.io.Source.fromInputStream(getClass.getResourceAsStream(path), Charsets.UTF_8.name()).mkString

}
