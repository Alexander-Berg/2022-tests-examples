package ru.yandex.realty.clients.balance

import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FlatSpec, Ignore, Matchers}
import ru.yandex.realty.http.TvmContext
import ru.yandex.realty.tvm.TvmLibraryApi
import ru.yandex.realty.tvm.impl.NoOpTvmLibraryApi
import ru.yandex.realty.tracing.Traced

/**
  * For manual use only!
  *
  * This test uses real Balance's testing so it may be unstable.
  */
@Ignore
@RunWith(classOf[JUnitRunner])
class DefaultBalanceClientTestManual extends FlatSpec with Matchers with ScalaFutures {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val traced: Traced = Traced.empty
  implicit val config: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(1000, Millis)), interval = scaled(Span(10000, Millis)))

  private val client = new DefaultBalanceClient(
    "https://balance-xmlrpc-tvm-ts.paysys.yandex.net:8004/xmlrpctvm",
    NoOpTvmLibraryApi,
    TvmContext("balance.xmlrpc")
  )

  behavior.of("Balance")

  it should "correct find info by uid" in {
    val res = client.getPassportByUid("485576718")
    res.futureValue.login.get should be("reklama-prostor")
  }

  it should "correct find balance info" in {
    val res = client.getClientCreditLimits(clientId = 32511881, productId = 507211)
    res.futureValue.head.contractId should be("278656")
  }

  it should "correct create request in balance" in {
    val res =
      client.createRequest(uid = "485576718", clientId = 60653856, serviceId = 82, orderId = 1534, quantity = 50)
    val requestId = res.futureValue.requestId
    requestId should not be 0
  }

  it should "find correct persons" in {
    val res = client.getClientPersons(clientId = 32511881)
    val persons = res.futureValue.head
    persons.clientId should be(32511881)
    persons.isOk should be(true)
  }

  it should "correct work with request choices for contract" in {
    val res = client.getContract(clientId = 7506343, None)
    res.futureValue.get.id should be(525617) // contractId
  }

  it should "correct create invoice without credit" in {
    val invoice = client.createInvoice(
      uid = "485576718",
      requestId = 695131609,
      personId = 6640230,
      contractIdOpt = Some(1002582),
      credit = false
    )
    invoice.futureValue should be > 0
  }

  it should "correct create invoice with credit" in {
    val invoice = client.createInvoice(
      uid = "485576718",
      requestId = 695131609,
      personId = 6640230,
      contractIdOpt = Some(1002582),
      credit = true
    )
    invoice.futureValue should be > 0
  }

  it should "correct insert new person" in {
    val res = client.upsertUrPerson(
      UrPersonToBalance(
        uid = "485576718",
        clientId = 32511881,
        personId = None,
        name = "name",
        longname = "longname",
        phone = "+79110047689",
        email = "email@domain.com",
        legaladdress = "legaladdress",
        inn = "6449013711",
        kpp = "644901001",
        account = Some("435345345345345345"),
        signerPersonName = Some("Вася Пупкин"),
        postcode = "190068",
        postaddress = "postaddress",
        representative = Some("representative")
      )
    )
    res.futureValue
  }

  it should "correct update person" in {
    val res = client.getClientPersons(clientId = 32511881)
    val person = res.futureValue.head
    client.upsertUrPerson(
      UrPersonToBalance(
        uid = "485576718",
        clientId = 32511881,
        personId = Some(person.parsedId.get),
        name = "name",
        longname = "longname2",
        phone = "+79110047689",
        email = "email@domain.com",
        legaladdress = "legaladdress",
        inn = "6449013711",
        kpp = "644901001",
        account = Some("435345345345345345"),
        signerPersonName = Some("Вася Пупкин"),
        postcode = "190068",
        postaddress = "postaddress",
        representative = Some("representative")
      )
    )
    res.futureValue
  }

  it should "correct process credit details for valid client" in {
    val res = client.getContractCreditsDetailed(contractId = 278656)

    val d = res.futureValue
    d.present should be(true)
  }

  it should "correct process credit details with error" in {
    val res = client.getContractCreditsDetailed(contractId = 278655)

    val d = res.futureValue
    d.present should be(false)
  }

  it should "get invoice for id" in {
    val res = client.getInvoice(485576718, 76524559)
    val invoice = res.futureValue
    invoice.id shouldBe 76524559
    invoice.clientId shouldBe 82402328
    invoice.personId shouldBe 7007128
    invoice.cancelled shouldBe false
  }
}
