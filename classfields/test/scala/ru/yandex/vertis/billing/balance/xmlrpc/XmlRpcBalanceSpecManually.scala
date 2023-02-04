package ru.yandex.vertis.billing.balance.xmlrpc

import org.joda.time.DateTime
import org.scalatest.Ignore
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.billing.balance._
import ru.yandex.vertis.billing.balance.model._
import ru.yandex.vertis.billing.balance.xmlrpc.XmlRpcBalanceSpecManually._

/**
  * Created by IntelliJ IDEA.
  * User: alesavin
  * Date: 17.09.14
  * Time: 16:09
  */
@Ignore
class XmlRpcBalanceSpecManually extends AnyFlatSpec with Matchers {

  // TODO replace returnPath
  "CreateRequest" should "return PaymentRequestResult" in {
    val prr = balance.createRequest(
      448659L,
      PaymentRequest(OrderId(97, 297L), BigDecimal(1), returnPath = Some("http://dealer.auto.csfront01gt.yandex.ru"))
    )
    println(prr)
    assert(prr.isSuccess)
    assert(prr.get.clientUrl.length > 0)
    assert(prr.get.adminUrl.length > 0)
  }

  "ListClientPassports" should "return ClientUsers" in {
    val cu = balance.listClientPassports(4036064L)
    println(cu)
    assert(cu.isSuccess)
  }

  "ListClientPassports" should "return none" in {
    val cu = balance.listClientPassports(1)
    println(cu)
    assert(cu.isFailure)
  }

  "ListClientPassports" should "return too many ClientUsers" in {
    val cu = balance.listClientPassports(417938L)
    println(cu)
  }

  "CreateUserClientAssociation" should "fail" in {
    val prr = balance.createUserClientAssociation(4036064L, "1234")
    println(prr)
    assert(prr.isFailure)
  }

  "RemoveUserClientAssociation" should "fail" in {
    val prr = balance.removeUserClientAssociation(4036064L, "1234")
    println(prr)
    assert(prr.isFailure)
  }

  "UserClientAssociation" should "remove" in {
    assert(balance.removeUserClientAssociation(4036064L, "37161071").isSuccess)
    val empty = balance.listClientPassports(4036064L)
    assert(empty.isSuccess)
    assert(empty.get.size === 0)
  }

  "UserClientAssociation" should "add" in {
    assert(balance.createUserClientAssociation(4036064L, "37161071").isSuccess)
    val back = balance.listClientPassports(4036064L)
    assert(back.isSuccess)
    assert(back.get.size === 1)
  }

  "GetClientByIdBatch" should "return client" in {
    val prr = balance.getClientsByIdBatch(Seq(8190332L))
    //      val prr = balance.GetClientByIdBatch(Seq(4036064L))
    println(prr)
  }

  "GetClientByIdBatch" should "return clients" in {
    val prr = balance.getClientsByIdBatch(Seq(4036064L, 417938L))
    println(prr)
  }

  "CreateClient" should "create new client and edit it" in {
    val regionId = 101521
    val cid = balance.createClient(
      ClientRequest(None, ClientProperties(clientType = ClientTypes.IndividualPerson, regionId = Some(regionId)))
    )
    println(cid)
    assert(cid.isSuccess)
    val client = balance.getClientsByIdBatch(Seq(cid.get))
    println(client)
    assert(client.isSuccess)
    assert(client.get.head.id === cid.get)
    assert(client.get.head.properties.clientType === ClientTypes.IndividualPerson)
    assert(client.get.head.properties.regionId === Some(regionId))

    balance.createClient(
      ClientRequest(
        client.get.head.copy(properties = client.get.head.properties.copy(name = Some("Test"), fax = Some("0001-111")))
      )
    )
    val client2 = balance.getClientsByIdBatch(cid.get)
    println(client2)
    assert(client2.isSuccess)
  }

  "CreateClient" should "create new agency and edit it" in {
    val cid = balance.createClient(
      ClientRequest(None, ClientProperties(clientType = ClientTypes.IndividualPerson, isAgency = true))
    )
    println(cid)
    assert(cid.isSuccess)
    val client = balance.getClientsByIdBatch(Seq(cid.get))
    println(client)
    assert(client.isSuccess)
    assert(client.get.head.id === cid.get)
    assert(client.get.head.properties.clientType === ClientTypes.IndividualPerson)

    balance.createClient(
      ClientRequest(
        client.get.head.copy(properties = client.get.head.properties.copy(city = Some("Spb"), email = Some("1@ya.ru")))
      )
    )
    val client2 = balance.getClientsByIdBatch(Seq(cid.get))
    println(client2)
    assert(client2.isSuccess)
  }

  "CreateOrUpdateOrdersBatch" should "update 97-402" in {
    // 37161071
    val or = balance.createOrUpdateOrdersBatch(
      Order(OrderId(97, 402L), 1480908L, 503794L, agencyId = Some(5338340L), managerUid = Some(""))
    )
    println(or)
    assert(or.isSuccess)
  }

  "CreateOrUpdateOrdersBatch" should "create new campaign and edit it" in {
    val or = balance.createOrUpdateOrdersBatch(Order(OrderId(97, 10001L), 4036064L, 503794L))
    println(or)
    assert(or.isSuccess)

    val or2 = balance.createOrUpdateOrdersBatch(
      Order(
        OrderId(97, 10001L),
        4036064L,
        503794L,
        text = Some("text"),
        actText = Some("Act text"),
        startTime = Some(DateTime.now().plusDays(5))
      )
    )
    println(or2)
    assert(or2.isSuccess)

    val or3 = balance.createOrUpdateOrdersBatch(Order(OrderId(97, 10001L), 4036064L, 503794L, agencyId = Some(417938L)))
    println(or3)
    assert(or3.isSuccess)
  }

  "CreateOrUpdateOrdersBatch" should "edit campaign - remove agencyId, text" in {
    val or = balance.createOrUpdateOrdersBatch(Order(OrderId(97, 10001L), 4036064L, 503794L))
    println(or)
    assert(or.isSuccess)
  }

  "CreateOrUpdateOrdersBatch" should "edit campaign multi" in {
    val or = balance.createOrUpdateOrdersBatch(
      Order(OrderId(97, 10001L), 4036064L, 503794L),
      Order(OrderId(97, 10001L), 4036064L, 503794L, agencyId = Some(417938L)),
      Order(OrderId(97, 10001L), 4036064L, 503794L, agencyId = Some(-1L))
    )
    println(or)
    assert(or.isSuccess)
  }

  "UpdateCampaigns" should "set campaign spendings" in {
    val or = balance.updateCampaigns(
      CampaignSpending(OrderId(97, 10001L), DateTime.now(), stop = false, bucks = Some(BigDecimal(3)))
    )
    println(or)
    assert(or.isSuccess)
  }

  "UpdateCampaigns" should "set campaign spendings multi" in {
    val or = balance.updateCampaigns(
      CampaignSpending(OrderId(97, 10001L), DateTime.now(), stop = false, bucks = Some(BigDecimal(3))),
      CampaignSpending(OrderId(97, 10001L), DateTime.now(), stop = false, bucks = Some(BigDecimal(5)))
    )
    println(or)
    assert(or.isSuccess)
  }

  "GetOrdersInfo" should "return orders info" in {
    val or = balance.getOrdersInfo(OrderRequest(OrderId(97, 10001L)))
    println(or)
    assert(or.isSuccess)

    val or2 = balance.getOrdersInfo(OrderRequest(OrderId(99, 30829L)))
    println(or2)
    assert(or2.isSuccess)
    val or3 = balance.getOrdersInfo(OrderRequest(OrderId(99, 43939L)))
    println(or3)
    assert(or3.isSuccess)

  }

  "GetPassportByLogin" should "return client users" in {
    val or = balance.getPassportByLogin("samehome")
    println(or)
    assert(or.isSuccess)
  }

  "GetPassportByUid" should "return client users" in {
    val or = balance.getPassportByUid("37161071")
    println(or)
    assert(or.isSuccess)
  }

  "UpdateNotificationUrl" should "update url" in {
    println(
      balance.updateNotificationUrl(
        NotificationUrlChangeRequest(81, "http://csback2ft.yandex.ru:34103/api/1.x/service/realty/balance/notify")
      )
    )
    println(
      balance.updateNotificationUrl(
        NotificationUrlChangeRequest(
          82,
          "http://csback2ft.yandex.ru:34103/api/1.x/service/realty_commercial/balance/notify"
        )
      )
    )
    println(
      balance.updateNotificationUrl(
        NotificationUrlChangeRequest(99, "http://csback2ft.yandex.ru:34103/api/1.x/service/autoru/balance/notify")
      )
    )
    /*
    println(balance.updateNotificationUrl(
      NotificationUrlChangeRequest(99, "http://dev04i.vs.os.yandex.net:34103/api/1.x/service/autoru/balance/notify")))
     */
  }
}

object XmlRpcBalanceSpecManually {
  val balance = new XmlRpcBalance("http://127.0.0.1:8002/xmlrpc")
}
