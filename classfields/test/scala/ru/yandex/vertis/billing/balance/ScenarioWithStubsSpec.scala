package ru.yandex.vertis.billing.balance

import org.scalatest.featurespec.AnyFeatureSpec
import ru.yandex.vertis.billing.balance.ScenarioWithStubsSpec._
import ru.yandex.vertis.billing.balance.model._
import ru.yandex.vertis.billing.balance.xmlrpc._

class ScenarioWithStubsSpec extends AnyFeatureSpec {

  val EmptyClientProperties = ClientProperties(
    clientType = ClientTypes.IndividualPerson,
    isAgency = false,
    agencyId = None,
    name = Some(""),
    email = Some(""),
    phone = Some(""),
    fax = Some(""),
    url = Some(""),
    city = Some("")
  )

  Feature("Balance") {
    Scenario("Direct client interaction") {
      val balance = new XmlRpcBalance(Recorder(s"$baseScenariosDir/client"))

      info("create client")
      val properties = ClientProperties()
      val id = balance.createClient(ClientRequest(None, properties)).get

      val optClient = balance.getClientsById(id).get
      assert(optClient.isDefined)
      val client = optClient.get

      assert(client.id === id)
      assert(client.properties == EmptyClientProperties)

      info("edit client")

      val editedProperties = client.properties.copy(
        name = Some("Client for test"),
        email = Some("client@ya.ru"),
        phone = Some("123-11-12"),
        fax = Some("124-11-12"),
        url = Some("http://www.ya.ru"),
        city = Some("Spb")
      )
      val request = ClientRequest(client).copy(properties = editedProperties)
      val editedClientId = balance.createClient(request).get
      assert(editedClientId === id)
      val optEditedClient = balance.getClientsById(id).get
      assert(optEditedClient.isDefined)
      val editedClient = optEditedClient.get
      assert(editedClient.id === id)
      assert(editedClient.properties === editedProperties)

      info("try to edit client agency flag")
      assert(
        balance
          .createClient(
            ClientRequest(editedClient).copy(
              properties = editedClient.properties.copy(isAgency = true)
            )
          )
          .isSuccess
      )
      val client3 = balance.getClientsById(id).get.get
      assert(client3.id === id)
      assert(client3.properties === editedProperties)

      info("try to clear some fields by new request")
      val newProperties = ClientProperties(ClientTypes.IndividualPerson, name = None, email = Some(""))
      balance.createClient(ClientRequest(Some(id), newProperties)).get
      val client4 = balance.getClientsById(id).get.get
      assert(client4.id === id)
      assert(client4.properties.clientType === ClientTypes.IndividualPerson)
      assert(client4.properties.name.get === "Client for test")
      assert(client4.properties.email.get === "")
      assert(client4.properties.phone.get === "123-11-12")
      assert(client4.properties.fax.get === "124-11-12")
      assert(client4.properties.url.get === "http://www.ya.ru")
      assert(client4.properties.city.get === "Spb")
      assert(client4.properties.agencyId.isEmpty)

      info("try to change client type")
      assert(balance.createClient(ClientRequest(Some(id), ClientProperties(ClientTypes.Jsc))).isSuccess)

      val client5 = balance.getClientsById(id).get.get
      assert(client5.id === id)
      assert(client5.properties.clientType === ClientTypes.Jsc)

      info("create second client")
      val clientIdSecond = balance.createClient(ClientRequest(None, ClientProperties(ClientTypes.LimitedCompany)))
      assert(clientIdSecond.isSuccess)

      info("get clients")
      val clients = balance.getClientsByIdBatch(Seq(id, clientIdSecond.get))
      assert(clients.isSuccess)
      assert(clients.get.size == 2)

      info("edit client type")
      val optClient2 = balance.getClientsById(clientIdSecond.get).get
      assert(optClient2.isDefined)
      assert(optClient2.get.properties == EmptyClientProperties.copy(clientType = ClientTypes.LimitedCompany))

      assert(
        balance
          .createClient(
            ClientRequest(optClient2.get).copy(
              properties = optClient2.get.properties.copy(clientType = ClientTypes.Jsc)
            )
          )
          .isSuccess
      )
      val optClient3 = balance.getClientsById(clientIdSecond.get).get
      assert(optClient3.isDefined)
      assert(optClient3.get.properties == EmptyClientProperties.copy(clientType = ClientTypes.Jsc))
    }

    Scenario("Client users") {
      val balance = new XmlRpcBalance(Recorder(s"$baseScenariosDir/client-users"))

      info("create clients")
      val clientId = balance.createClient(ClientRequest(None, ClientProperties(ClientTypes.IndividualPerson))).get
      val clientId2 = balance.createClient(ClientRequest(None, ClientProperties(ClientTypes.Jsc))).get

      info("uid failures")

      assert(balance.getPassportByUid("a").isFailure)
      intercept[IllegalArgumentException] {
        balance.getPassportByUid("-1").get
      }
      assert(balance.getPassportByUid("0").isFailure)

      val cu0 = balance.getPassportByUid("1")
      assert(cu0.isSuccess)
      assert(cu0.get.isEmpty)

      info("uid client clear")

      val cu = balance.getPassportByUid(uid)
      // if failure - uid is finded
      if (cu.isSuccess && cu.get.isDefined) {
        assert(cu.get.get.uid === uid)
        // Success(ClientUser(37161071,SameHome,SameHome,Some(4036064),true))
        info(cu.toString)
        if (cu.get.get.clientId.isDefined)
          assert(balance.removeUserClientAssociation(cu.get.get.clientId.get, uid).isSuccess)
      }

      val cu2 = balance.getPassportByUid(uid)
      assert(cu2.isSuccess)
      assert(cu2.get.get.uid === uid)
      assert(cu2.get.get.clientId.isEmpty)
      info(cu2.toString)

      val ccu = balance.listClientPassports(clientId)
      assert(ccu.isSuccess)
      assert(ccu.get.size === 0)

      info("create association")

      assert(balance.createUserClientAssociation(clientId, uid).isSuccess)
      val cu3 = balance.getPassportByUid(uid)
      assert(cu3.isSuccess)
      assert(cu3.get.get.uid === uid)
      assert(cu3.get.get.clientId.get === clientId)
      info(cu3.toString)

      val ccu2 = balance.listClientPassports(clientId)
      assert(ccu2.isSuccess)
      assert(ccu2.get.size === 1)
      assert(ccu2.get.head.clientId.get === clientId)
      assert(ccu2.get.head.uid === uid)

      info("create bad association")
      intercept[AlreadyAssociatedWithAnotherException] {
        balance.createUserClientAssociation(clientId2, uid).get
      }
      intercept[ClientNotFoundException] {
        balance.createUserClientAssociation(0, uid).get
      }
      intercept[AlreadyAssociatedException] {
        balance.createUserClientAssociation(clientId, uid).get
      }

      info("remove association")

      assert(balance.removeUserClientAssociation(clientId, uid).isSuccess)

      val ccu3 = balance.listClientPassports(clientId)
      assert(ccu3.isSuccess)
      assert(ccu3.get.size === 0)

      info("remove association 2")
      intercept[NoAssociatedException] {
        balance.removeUserClientAssociation(clientId, uid).get
      }

      info("list by login")
      val cu4 = balance.getPassportByLogin(login)
      assert(cu4.isSuccess)
      assert(cu4.get.isDefined)
      assert(cu4.get.get.login.toLowerCase === login)
    }
  }

  Scenario("Orders for direct clients") {
    val balance = new XmlRpcBalance(Recorder(s"$baseScenariosDir/orders"))

    info("create clients")
    val clientId = balance.createClient(ClientRequest(None, ClientProperties(ClientTypes.UnincorporatedBusiness))).get
    val clientId2 = balance.createClient(ClientRequest(None, ClientProperties(ClientTypes.LimitedCompany))).get

    val orderId1 = getUniqOrderNumber
    val orderId2 = orderId1 + 1

    info("create orders")

    val orderResults = balance
      .createOrUpdateOrdersBatch(
        Order(OrderId(serviceId, orderId1), clientId, productId),
        Order(OrderId(serviceId, orderId2), clientId, productId)
      )
      .get
    assert(orderResults.size === 2)
    assert(orderResults(0).code === 0)
    println(orderResults(1))
    assert(orderResults(1).code === 0)

    info("check orders")
    val order11 = balance.getOrdersInfo(Seq(OrderRequest(OrderId(serviceId, orderId1))))
    assert(order11.isSuccess)
    assert(order11.get.size === 1)
    assert(order11.get.head.serviceId === serviceId)
    assert(order11.get.head.serviceOrderId === orderId1)
    assert(order11.get.head.productId.get === productId)
    assert(order11.get.head.consumeQty.get === 0)
    assert(order11.get.head.consumeMoneyQty.get === 0)

    val order21 = balance.getOrdersInfo(Seq(OrderRequest(OrderId(serviceId, orderId2))))
    assert(order21.isSuccess)
    assert(order21.get.size === 1)
    assert(order21.get.head.serviceId === serviceId)
    assert(order21.get.head.serviceOrderId === orderId2)
    assert(order21.get.head.productId.get === productId)

    val orders =
      balance.getOrdersInfo(Seq(OrderRequest(OrderId(serviceId, orderId1)), OrderRequest(OrderId(serviceId, orderId2))))
    assert(orders.isSuccess)
    assert(orders.get.size === 2)
    assert(
      orders.get.head.serviceOrderId == orderId1 ||
        orders.get.head.serviceOrderId == orderId2
    )

    info("edit order")
    val orderResults2 = balance.createOrUpdateOrdersBatch(
      Seq(
        Order(
          OrderId(serviceId, orderId2),
          clientId,
          productId,
          text = Some("text"),
          actText = Some("act text"),
          managerUid = Some("37161071"),
          startTime = Some(orderDatePattern.parseDateTime("20250101"))
        )
      )
    )
    assert(orderResults2.isSuccess)
    assert(orderResults2.get.size == 1)
    assert(orderResults2.get.head.code == 0)

    val order22 = balance.getOrdersInfo(Seq(OrderRequest(OrderId(serviceId, orderId2))))
    assert(order22.isSuccess)
    assert(order22.get.head.serviceOrderId === orderId2)
    assert(order22.get.head.completionQty.get === 0)

    info("edit orders, change client for order (no payments)")

    val orderResults3 = balance.createOrUpdateOrdersBatch(
      Seq(
        Order(OrderId(serviceId, orderId1), clientId2, productId),
        Order(
          OrderId(serviceId, orderId2),
          clientId,
          productId,
          text = Some("text"),
          actText = Some("act text"),
          managerUid = Some("37161071"),
          startTime = Some(orderDatePattern.parseDateTime("20250101"))
        )
      )
    )
    assert(orderResults3.isSuccess)
    assert(orderResults3.get.size == 2)
    assert(orderResults3.get.head.code == 0)
    assert(orderResults3.get.tail.head.code == 0)

    info("create payment request")

    val pr = balance.createRequest(clientId, PaymentRequest(OrderId(serviceId, orderId2), qty = BigDecimal(17.9)))
    assert(pr.isSuccess)
    assert(pr.get.clientUrl.startsWith("https://"))
    assert(pr.get.adminUrl.startsWith("https://"))

    // for invalid client
    val pr2 = balance.createRequest(clientId, PaymentRequest(OrderId(serviceId, orderId1), qty = BigDecimal(17.9)))
    assert(pr2.isFailure)

    val pr3 = balance.createRequest(clientId, PaymentRequest(OrderId(serviceId, orderId2), qty = BigDecimal(-1)))
    assert(pr3.isFailure)

    val pr4 = balance.createRequest(clientId, PaymentRequest(OrderId(-1, orderId2), qty = BigDecimal(-1)))
    assert(pr4.isFailure)

    info("create order for invalid service =/")
    // why???
    assert(balance.createOrUpdateOrdersBatch(Order(OrderId(-1, orderId1), clientId, productId)).isSuccess)

    info("send spendings")

    val uc = balance.updateCampaigns(
      Seq(
        CampaignSpending(
          OrderId(serviceId, orderId2),
          campaignSpendingDatePattern.parseDateTime("20140924163719"),
          stop = false,
          bucks = Some(BigDecimal(3.3))
        )
      )
    )
    assert(uc.isSuccess)
    assert(uc.get.size === 1)
    assert(uc.get.head.orderId.serviceId === serviceId)
    assert(uc.get.head.orderId.serviceOrderId === orderId2)
    assert(uc.get.head.result === 1)

    // there need sleep in real connection (minutes) for completionQty
    //      Thread.sleep(300000)

    val order23 = balance.getOrderInfo(OrderRequest(OrderId(serviceId, orderId2))).get.get
    assert(order23.serviceId === serviceId)
    assert(order23.serviceOrderId === orderId2)
  }

  Scenario("Agency client interaction") {
    val balance = new XmlRpcBalance(Recorder(s"$baseScenariosDir/agency"))

    info("create agency")
    val agencyId =
      balance.createClient(ClientRequest(None, ClientProperties(ClientTypes.IndividualPerson, isAgency = true)))
    assert(agencyId.isSuccess)

    val agency = balance.getClientsById(agencyId.get).get.get
    assert(agency.id === agencyId.get)
    assert(
      agency.properties ===
        EmptyClientProperties.copy(isAgency = true, agencyId = Some(agency.id))
    )

    info("create non agency")
    val nonAgencyId =
      balance.createClient(ClientRequest(None, ClientProperties(ClientTypes.LimitedCompany, isAgency = false)))
    assert(nonAgencyId.isSuccess)
    val nonAgency = balance.getClientsById(nonAgencyId.get).get.get
    assert(nonAgency.id === nonAgencyId.get)
    assert(
      nonAgency.properties ===
        EmptyClientProperties.copy(clientType = ClientTypes.LimitedCompany, isAgency = false, agencyId = None)
    )

    info("edit agency")
    val agency2request = ClientRequest(
      agency.copy(properties =
        agency.properties.copy(
          name = Some("Agency for test"),
          email = Some("agency@ya.ru"),
          phone = Some("123-44-55"),
          fax = Some("124-77-88"),
          url = Some("http://www.ay.ru"),
          city = Some("Msk")
        )
      )
    )
    balance.createClient(agency2request).get
    val agency2 = balance.getClientsById(agencyId.get).get.get
    assert(agency2.id === agencyId.get)
    assert(agency2.properties.clientType === ClientTypes.IndividualPerson)
    assert(agency2.properties.name.get === "Agency for test")
    assert(agency2.properties.email.get === "agency@ya.ru")
    assert(agency2.properties.phone.get === "123-44-55")
    assert(agency2.properties.fax.get === "124-77-88")
    assert(agency2.properties.url.get === "http://www.ay.ru")
    assert(agency2.properties.city.get === "Msk")
    assert(agency2.properties.agencyId.get === agencyId.get)

    info("try to change agency flag")
    val agencyId3 =
      balance.createClient(ClientRequest(agency2.copy(properties = agency2.properties.copy(isAgency = false))))
    assert(agencyId3.isSuccess)
    assert(agencyId3.get === agencyId.get)

    val agency3 = balance.getClientsById(agencyId.get).get.get
    assert(agency3.id === agencyId.get)
    assert(agency3.properties.clientType === ClientTypes.IndividualPerson)
    assert(agency3.properties.name.get === "Agency for test")
    assert(agency3.properties.email.get === "agency@ya.ru")
    assert(agency3.properties.phone.get === "123-44-55")
    assert(agency3.properties.fax.get === "124-77-88")
    assert(agency3.properties.url.get === "http://www.ay.ru")
    assert(agency3.properties.city.get === "Msk")
    assert(agency3.properties.agencyId.get === agencyId.get)

    info("create agency subclient")
    val clientId = balance
      .createClient(
        ClientRequest(None, ClientProperties(ClientTypes.UnincorporatedBusiness, agencyId = Some(agencyId.get)))
      )
      .get

    val client = balance.getClientsById(clientId).get.get
    assert(client.id === clientId)
    assert(client.properties.clientType === ClientTypes.UnincorporatedBusiness)
    assert(client.properties.agencyId.get === agencyId.get)

    info("create non agency 2")
    val nonAgencyId2 = balance.createClient(
      ClientRequest(None, ClientProperties(ClientTypes.IndividualPerson, isAgency = false, agencyId = Some(0)))
    )
    assert(nonAgencyId2.isSuccess)

    val nonAgency2 = balance.getClientsById(nonAgencyId2.get).get.get
    assert(nonAgency2.id === nonAgencyId2.get)
    assert(
      nonAgency2.properties ===
        EmptyClientProperties.copy(isAgency = false, agencyId = None)
    )

    info("create non agency 3")
    val nonAgencyId3 =
      balance.createClient(ClientRequest(None, ClientProperties(ClientTypes.ClosedCompany, agencyId = Some(0))))
    assert(nonAgencyId3.isSuccess)

    val nonAgency3 = balance.getClientsById(nonAgencyId3.get).get.get
    assert(nonAgency3.id === nonAgencyId3.get)
    assert(
      nonAgency3.properties ===
        EmptyClientProperties.copy(clientType = ClientTypes.ClosedCompany, isAgency = false, agencyId = None)
    )
  }

  Scenario("Agency users") {

    val balance = new XmlRpcBalance(Recorder(s"$baseScenariosDir/agency-users"))

    info("create agency, client")
    val agencyId =
      balance.createClient(ClientRequest(None, ClientProperties(ClientTypes.IndividualPerson, isAgency = true))).get
    val clientId = balance.createClient(ClientRequest(None, ClientProperties(ClientTypes.UnincorporatedBusiness))).get

    info("uid client clear")

    val cu = balance.getPassportByUid(uid)
    // if failure - uid is finded
    if (cu.isSuccess && cu.get.isDefined) {
      assert(cu.get.get.uid === uid)
      // Success(ClientUser(37161071,SameHome,SameHome,Some(4036064),true))
      info(cu.toString)
      if (cu.get.get.clientId.isDefined)
        assert(balance.removeUserClientAssociation(cu.get.get.clientId.get, uid).isSuccess)
    }

    info("create association")
    assert(balance.createUserClientAssociation(agencyId, uid).isSuccess)
    val au = balance.getPassportByUid(uid)
    assert(au.isSuccess)
    assert(au.get.get.uid === uid)
    assert(au.get.get.clientId.get === agencyId)

    val acu = balance.listClientPassports(agencyId)
    assert(acu.isSuccess)
    assert(acu.get.size === 1)
    assert(acu.get.head.clientId.get === agencyId)
    assert(acu.get.head.uid === uid)

    info("create bad association")
    intercept[AlreadyAssociatedWithAnotherException] {
      balance.createUserClientAssociation(clientId, uid).get
    }
    intercept[ClientNotFoundException] {
      balance.createUserClientAssociation(0, uid).get
    }
    intercept[AlreadyAssociatedException] {
      balance.createUserClientAssociation(agencyId, uid).get
    }

    info("association for subclient should fail")
    assert(balance.createUserClientAssociation(clientId, uid).isFailure)

    info("remove association")
    assert(balance.removeUserClientAssociation(agencyId, uid).isSuccess)
    val acu2 = balance.listClientPassports(agencyId)
    assert(acu2.isSuccess)
    assert(acu2.get.size === 0)

    info("remove association 2")
    intercept[NoAssociatedException] {
      balance.removeUserClientAssociation(clientId, uid).get
    }

    info("association for subclient should pass")
    assert(balance.createUserClientAssociation(clientId, uid).isSuccess)
    assert(balance.removeUserClientAssociation(clientId, uid).isSuccess)

  }

  Scenario("Orders for agency") {
    val balance = new XmlRpcBalance(Recorder(s"$baseScenariosDir/agency-orders"))

    info("create agency, client")
    val agencyId =
      balance.createClient(ClientRequest(None, ClientProperties(ClientTypes.IndividualPerson, isAgency = true))).get
    val clientId = balance.createClient(ClientRequest(None, ClientProperties(ClientTypes.UnincorporatedBusiness))).get

    val orderId3 = getUniqOrderNumber + 2
    val orderId4 = orderId3 + 1
    val orderId5 = orderId3 + 2

    info("create orders")

    val orderResults = balance
      .createOrUpdateOrdersBatch(
        Order(OrderId(serviceId, orderId3), agencyId, productId),
        Order(OrderId(serviceId, orderId4), clientId, productId, agencyId = Some(agencyId)),
        Order(OrderId(serviceId, orderId5), clientId, productId)
      )
      .get
    assert(orderResults.size == 3)
    assert(orderResults.filter(_.code != 0).isEmpty)

    info("check orders")
    val orders = balance.getOrdersInfo(
      Seq(
        OrderRequest(OrderId(serviceId, orderId3)),
        OrderRequest(OrderId(serviceId, orderId4)),
        OrderRequest(OrderId(serviceId, orderId5))
      )
    )
    assert(orders.isSuccess)
    assert(orders.get.size === 3)
    assert(orders.get.filter(_.serviceId != serviceId).isEmpty)
    assert(orders.get.filter(_.productId.get != productId).isEmpty)
    assert(orders.get.filter(_.completionQty.get != 0).isEmpty)
    assert(orders.get.filter(_.consumeQty.get != 0).isEmpty)

    info("we can't get agencyId, clienId for order, solve it by request payment fails")
    assert(
      balance.createRequest(agencyId, PaymentRequest(OrderId(serviceId, orderId3), qty = BigDecimal(0.11))).isSuccess
    )
    assert(
      balance.createRequest(clientId, PaymentRequest(OrderId(serviceId, orderId3), qty = BigDecimal(0.11))).isFailure
    )

    assert(
      balance.createRequest(agencyId, PaymentRequest(OrderId(serviceId, orderId4), qty = BigDecimal(0.22))).isSuccess
    )
    // direct client can't pay for agency-client campaign
    assert(
      balance.createRequest(clientId, PaymentRequest(OrderId(serviceId, orderId4), qty = BigDecimal(0.22))).isFailure
    )

    assert(
      balance.createRequest(agencyId, PaymentRequest(OrderId(serviceId, orderId5), qty = BigDecimal(0.33))).isFailure
    )
    assert(
      balance.createRequest(clientId, PaymentRequest(OrderId(serviceId, orderId5), qty = BigDecimal(0.33))).isSuccess
    )

    info("create order with bad product name")
    val orderResults2 = balance.createOrUpdateOrdersBatch(
      Order(OrderId(serviceId, orderId4), clientId, -1L, agencyId = Some(agencyId))
    )
    assert(orderResults2.isSuccess)
    assert(orderResults2.get.size === 1)
    assert(orderResults2.get.head.code !== 0)

    info("edit order")
    val orderResults3 = balance.createOrUpdateOrdersBatch(
      Order(
        OrderId(serviceId, orderId4),
        clientId,
        productId,
        agencyId = Some(agencyId),
        text = Some("text"),
        actText = Some("act text"),
        managerUid = Some("37161071"),
        startTime = Some(orderDatePattern.parseDateTime("20250101"))
      )
    )
    assert(orderResults3.isSuccess)
    assert(orderResults3.get.size === 1)
    assert(orderResults3.get.head.code === 0)

    // only equivalent clients are permitted
    val orderResults4 = balance.createOrUpdateOrdersBatch(
      Order(OrderId(serviceId, orderId4), clientId, productId)
    )
    assert(orderResults4.isSuccess)
    assert(orderResults4.get.size === 1)
    assert(orderResults4.get.head.code !== 0)

    info("send spendings")

    val uc = balance.updateCampaigns(
      CampaignSpending(
        OrderId(serviceId, orderId4),
        campaignSpendingDatePattern.parseDateTime("20140924163719"),
        stop = false,
        bucks = Some(BigDecimal(4.4))
      )
    )
    assert(uc.isSuccess)
    assert(uc.get.size === 1)
    assert(uc.get.head.orderId.serviceId === serviceId)
    assert(uc.get.head.orderId.serviceOrderId === orderId4)
    assert(uc.get.head.result === 1)

    // there need sleep in real connection (minutes) for completionQty
    //      Thread.sleep(300000)

    val order23 = balance.getOrdersInfo(OrderRequest(OrderId(serviceId, orderId4)))
    assert(order23.isSuccess)
    assert(order23.get.size === 1)
    assert(order23.get.head.serviceId === serviceId)
    assert(order23.get.head.serviceOrderId === orderId4)
    //    assert(order23.get.head.completionQty.get > 0)

  }

  Scenario("Optional functions") {
    val balance = new XmlRpcBalance(Recorder(s"$baseScenariosDir/optional"))

    info("create client")
    balance.createClient(ClientRequest(None, ClientProperties(ClientTypes.UnincorporatedBusiness))).get

    assert(
      balance
        .updateNotificationUrl(NotificationUrlChangeRequest(serviceId, "http://mbi1ft.yandex.ru:34860/xmlrpc"))
        .isSuccess
    )
    // why???
    assert(balance.updateNotificationUrl(NotificationUrlChangeRequest(serviceId, "ht")).isSuccess)
    assert(
      balance
        .updateNotificationUrl(NotificationUrlChangeRequest(serviceId, "http://mbi1ft.yandex.ru:34860/xmlrpc"))
        .isSuccess
    )
  }

  Scenario("Order id") {
    println(getUniqOrderNumber)
    println(getUniqOrderNumber)
    Thread.sleep(1000L)
    println(getUniqOrderNumber)
  }

}

object ScenarioWithStubsSpec {

  val baseScenariosDir = "billing/billing/scala-balance-client/src/test/resources/scenario"
  val balanceRealUrl = "http://127.0.0.1:8002/xmlrpc"

  // due to GetOrdersInfo limits use int range longs
  def getUniqOrderNumber = 1000020

  //    System.currentTimeMillis() & 0xffffff
}
