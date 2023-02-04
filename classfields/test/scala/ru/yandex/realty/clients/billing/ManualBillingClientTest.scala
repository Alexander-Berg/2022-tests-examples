package ru.yandex.realty.clients.billing

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FlatSpec, Ignore, Matchers}
import ru.yandex.realty.http.{HttpEndpoint, RemoteHttpService, TestHttpClient}
import ru.yandex.realty.model.offer.OfferCampaignType
import ru.yandex.realty.tracing.Traced

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 15.02.18
  */
@Ignore
@RunWith(classOf[JUnitRunner])
class ManualBillingClientTest extends FlatSpec with Matchers with ScalaFutures with TestHttpClient {

  implicit val traced: Traced = Traced.empty
  implicit val config: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(1000, Millis)), interval = scaled(Span(3000, Millis)))

  private val httpService =
    new RemoteHttpService("billing", HttpEndpoint.apply("back-rt-01-sas.test.vertis.yandex.net", 34100), testClient)

  val client = new BillingClientImpl(httpService, BillingServices.RealtyCommercial)

  "Billing" should "correct find client orders" in {
    val res = client.getOrder(clientId = 60653856, uid = "485576718")
    res.futureValue.get.id shouldBe 1534
  }

  it should "correct create new order for client" in {
    val user = client.getUser(uid = "485576718")
    val c = user.futureValue.get.customers.head.client
    val res = client.createOrder(clientId = 1111, uid = "485576718", text = "Яндекс.Недвижимость")
    res.futureValue.id shouldBe >(0)
  }

  it should "correct find info about user in billing" in {
    val res = client.getUser(uid = "485576718")
    res.futureValue.get.role.length shouldBe >(0)
  }

  it should "correct return none for unknown user" in {
    val res = client.getUser(uid = "45")
    res.futureValue shouldBe None
  }

  it should "correct create new client with min params" in {
    val res = client.createClient(uid = "485576718", CreateClientBody(name = "Test client", email = None, phone = None))
    res.futureValue.client.id shouldBe >(0L)
  }

  it should "correct create new client with full params" in {
    val res = client.createClient(
      uid = "485576718",
      CreateClientBody(name = "Test client", email = Some("test@ya.ru"), phone = Some("+79110047689"))
    )
    res.futureValue.client.id shouldBe >(0L)
  }

  it should "correct assign client to user" in {
    val res = client.assignUserToClient(uid = "485576718", clientId = 60653856)
    res.futureValue
  }

  it should "correct find customer" in {
    val res = client.getCustomer(uid = "485576718", clientId = 32511881)
    res.futureValue.get.client.id shouldBe 32511881
  }

//  it should "correct create new customer with resource" in {
//    val c = client.createClient(uid = "485576718", name = "Test client", email = None, phone = None)
//    val clientId = c.futureValue.client.id
//    val res = client.createCustomer(uid = "485576718", clientId = clientId)
//    res.futureValue
//  }

  it should "correct update customer" in {
    var res = client.updateCustomer(
      uid = "485576718",
      clientId = 60653856,
      forAdd = Some(Resource(capaPartnerId = Some("777"))),
      forDelete = None
    )
    res.futureValue.resourceRefs.map(_.capaPartnerId.get).contains("777") shouldBe true
    res = client.updateCustomer(
      uid = "485576718",
      clientId = 60653856,
      forDelete = Some(Resource(capaPartnerId = Some("777"))),
      forAdd = None
    )
    res.futureValue.resourceRefs.map(_.capaPartnerId.get).contains("777") shouldBe false
  }

  it should "correct find campaigns" in {
    val res = client.getCampaigns(uid = "485576718", clientId = 32511881)
    val c = res.futureValue.values.map(_.`type`)
    c should contain(OfferCampaignType.PLACEMENT)
    c should contain(OfferCampaignType.RAISE)
    c should contain(OfferCampaignType.PREMIUM)
    c should contain(OfferCampaignType.FEED_PLACEMENT)
    c should contain(OfferCampaignType.PROMOTION)
    c should not contain OfferCampaignType.UNKNOWN
  }

  it should "correct create new campaign" in {
    val res = client.getOrder(clientId = 32511881, uid = "485576718")
    val orderId = res.futureValue.get.id
    val c = client.createCampaign(
      uid = "485576718",
      clientId = 32511881,
      orderId = orderId,
      campaign = OfferCampaignType.RAISE
    )
    c.futureValue
  }

  it should "correct find client delegates" in {
    val res = client.getClientDelegates(clientId = 32511881, operatorUid = "485576718")
    val c = res.futureValue
    c.size shouldBe 1
    val cd = c.head
    cd.uid shouldBe 485576718
    cd.login shouldBe "reklama-prostor"
    cd.name shouldBe Some("Pupkin Vasily")
  }
}
