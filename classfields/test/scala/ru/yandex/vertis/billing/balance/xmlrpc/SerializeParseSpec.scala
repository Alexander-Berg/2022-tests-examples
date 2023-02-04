package ru.yandex.vertis.billing.balance.xmlrpc

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.billing.balance._
import ru.yandex.vertis.billing.balance.model._
import ru.yandex.vertis.billing.balance.xmlrpc.model._
import ru.yandex.vertis.billing.balance.xmlrpc.parse.Parsers
import ru.yandex.vertis.billing.balance.xmlrpc.serialize.Serializers

/**
  * Created by IntelliJ IDEA.
  * User: alesavin
  * Date: 17.09.14
  * Time: 16:09
  */
class SerializeParseSpec extends AnyFlatSpec with Matchers {

  "serialize/parse" should "work on CreateRequest" in {
    val mcv = MethodCallValue(
      "Balance2.CreateRequest",
      StringValue(operatorUid) ::
        StringValue(4036064L.toString) ::
        Serializers.of(
          PaymentRequest(OrderId(97, 22L), BigDecimal(0.001), returnPath = Some("http://ya.ru"))
        ): _*
    )
    assert(mcv === Parsers.asMethodCall(mcv.asXml))
  }

  it should "work on CreateClient" in {
    val mcv = MethodCallValue(
      "Balance2.CreateClient",
      StringValue(operatorUid),
      Serializers.of(
        ClientRequest(
          Some(1),
          ClientProperties(
            ClientTypes.IndividualPerson,
            isAgency = false,
            None,
            Some("Client"),
            Some("email@ya.ru"),
            Some("123-11-11"),
            Some("124-11-11"),
            Some("http://ya.ru"),
            Some("Spb")
          )
        )
      )
    )
    assert(mcv === Parsers.asMethodCall(mcv.asXml))
  }

  it should "work on CreateClient agency" in {
    val mcv = MethodCallValue(
      "Balance2.CreateClient",
      StringValue(operatorUid),
      Serializers.of(
        ClientRequest(
          None,
          ClientProperties(
            ClientTypes.ClosedCompany,
            isAgency = true,
            None,
            Some("Agency"),
            Some("email@ya.ru"),
            Some("123-11-11"),
            Some("124-11-11"),
            Some("http://ya.ru"),
            Some("Spb")
          )
        )
      )
    )
    assert(mcv === Parsers.asMethodCall(mcv.asXml))
  }

  it should "work on CreateClient agency client" in {
    val mcv = MethodCallValue(
      "Balance2.CreateClient",
      StringValue(operatorUid),
      Serializers.of(
        ClientRequest(
          None,
          ClientProperties(
            ClientTypes.Jsc,
            isAgency = false,
            Some(2),
            Some("Agency client"),
            Some("email@ya.ru"),
            Some("123-11-11"),
            Some("124-11-11"),
            Some("http://ya.ru"),
            Some("Spb")
          )
        )
      )
    )
    assert(mcv === Parsers.asMethodCall(mcv.asXml))
  }

  it should "work on CreateUserClientAssociation" in {
    val mcv = MethodCallValue(
      "Balance2.CreateUserClientAssociation",
      StringValue(operatorUid),
      StringValue(111L.toString),
      StringValue("111111")
    )
    assert(mcv === Parsers.asMethodCall(mcv.asXml))
  }

  it should "work on RemoveUserClientAssociation" in {
    val mcv = MethodCallValue(
      "Balance2.RemoveUserClientAssociation",
      StringValue(operatorUid),
      StringValue(111L.toString),
      StringValue("111111")
    )
    assert(mcv === Parsers.asMethodCall(mcv.asXml))
  }

  it should "work on UpdateCampaigns" in {
    val css = Seq(
      CampaignSpending(
        OrderId(97, 10001),
        campaignSpendingDatePattern.parseDateTime("20140924163719"),
        stop = false,
        bucks = Some(BigDecimal(3))
      )
    )
    val mcv = MethodCallValue("Balance2.UpdateCampaigns", ArrayValue(css.map(cs => Serializers.of(cs))))
    assert(mcv === Parsers.asMethodCall(mcv.asXml))
  }

  it should "work on CreateOrUpdateOrdersBatch" in {
    val orders = Seq(Order(OrderId(97, 10001), 4036064L, 503794L))
    val mcv = MethodCallValue(
      "Balance2.CreateOrUpdateOrdersBatch",
      StringValue(operatorUid),
      ArrayValue(orders.map(order => Serializers.of(order)))
    )
    assert(mcv === Parsers.asMethodCall(mcv.asXml))
  }

  it should "work on GetClientByIdBatch" in {
    val mcv =
      MethodCallValue("Balance2.GetClientByIdBatch", ArrayValue(Seq(111L, 222L).map(l => StringValue(l.toString))))
    assert(mcv === Parsers.asMethodCall(mcv.asXml))
  }

  it should "work on ListClientPassports" in {
    val mcv = MethodCallValue("Balance2.ListClientPassports", StringValue(operatorUid), StringValue(111L.toString))
    assert(mcv === Parsers.asMethodCall(mcv.asXml))
  }

  it should "work on GetPassportByLogin" in {
    val mcv = MethodCallValue("Balance2.GetPassportByLogin", StringValue(operatorUid), StringValue("samehome"))
    assert(mcv === Parsers.asMethodCall(mcv.asXml))
  }

  it should "work on GetPassportByUid" in {
    val mcv = MethodCallValue("Balance2.GetPassportByUid", StringValue(operatorUid), StringValue("123"))
    assert(mcv === Parsers.asMethodCall(mcv.asXml))
  }

  it should "work on GetOrdersInfo" in {
    val ors = Seq(OrderRequest(OrderId(97, 10001L)))

    val mcv = MethodCallValue("Balance2.GetOrdersInfo", ArrayValue(ors.map(or => Serializers.of(or))))
    assert(mcv === Parsers.asMethodCall(mcv.asXml))
  }

  it should "work on GetClientPersons" in {
    val mcv = MethodCallValue("Balance2.GetClientPersons", StringValue("111"), StringValue("0"))
    assert(mcv === Parsers.asMethodCall(mcv.asXml))
  }

  "FaultValue response" should "be serialized and parsed" in {
    val mcv = MethodResponseValue(FaultValue(-1, "Test fault"))
    assert(mcv === Parsers.asMethodResponse(mcv.asXml))
  }

}
