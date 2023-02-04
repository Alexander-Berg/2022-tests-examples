package ru.yandex.vertis.billing.balance.xmlrpc

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.balance.model.{OperatorId, Person, PersonRequest}
import ru.yandex.vertis.billing.balance.xmlrpc.XmlRpcBalanceSpec.MockXmlRpcBalance
import ru.yandex.vertis.billing.balance.xmlrpc.model.{MethodCallValue, Value}
import ru.yandex.vertis.billing.balance.xmlrpc.parse.Parsers

import scala.xml.Elem

/**
  * @author alex-kovalenko
  */
class XmlRpcBalanceSpec extends AnyWordSpec with Matchers with BeforeAndAfterEach {

  implicit val operatorUid: OperatorId = "284743192"

  val clientId = 22975103

  val lname = "Иванов"
  val fname = "Иван"
  val mname = "Иванович"
  val phone = "+70001234567"
  val email = "email@example.com"

  val name = "test_ur"
  val longName = "Test create ur person"
  val postCode = "190000"
  val postAddress = "Пискаревский проспект"
  val legalAddress = "Пискаревский проспект 2/2"
  val inn = "7800000000"
  val kpp = "12345678"
  val bik = "044030723"
  val ogrn = "1157847288147"
  val account = "40817810503000160000"

  val balance = new MockXmlRpcBalance

  val personPhId = 5691832
  val personUrId = 5697846

  val individualPersonProps = Person.Individual(fname, lname, mname, phone, email)

  val juridicalPersonProps =
    Person.Juridical(
      name,
      longName,
      phone,
      email,
      postCode,
      Some(postAddress),
      legalAddress,
      inn,
      Some(kpp),
      bik,
      ogrn,
      account
    )

  override protected def beforeEach(): Unit = {
    balance.reset()
    super.beforeEach()
  }

  "XmlRpcBalance" should {
    "create person" when {
      "asked for ph" in {
        val rq = PersonRequest(None, clientId, individualPersonProps)
        balance.expectFrom("/balance/xmlrpc_rq_CreatePerson_ph.xml")
        balance.respondFrom("/balance/xmlrpc_rs_CreatePerson_ph.xml")
        val response = balance.createPerson(rq).get
        response shouldBe personPhId
      }
      "asked for ur" in {
        val rq = PersonRequest(None, clientId, juridicalPersonProps)
        balance.expectFrom("/balance/xmlrpc_rq_CreatePerson_ur.xml")
        balance.respondFrom("/balance/xmlrpc_rs_CreatePerson_ur.xml")
        val response = balance.createPerson(rq).get
        response shouldBe personUrId
      }
    }

    "get client persons" in {
      balance.expectFrom("/balance/xmlrpc_rq_GetClientPersons.xml")
      balance.respondFrom("/balance/xmlrpc_rs_GetClientPersons.xml")

      val persons = balance.getClientPersons(clientId).get
      persons should ((have size 2 and contain)
        .allOf(Person(personPhId, clientId, individualPersonProps), Person(personUrId, clientId, juridicalPersonProps)))
    }

    "update person" in {
      val rq = PersonRequest(Some(personPhId), clientId, individualPersonProps)
      balance.expectFrom("/balance/xmlrpc_rq_UpdatePerson_ph.xml")
      balance.respondFrom("/balance/xmlrpc_rs_CreatePerson_ph.xml")

      val response = balance.createPerson(rq).get
      response shouldBe personPhId
    }
  }
}

object XmlRpcBalanceSpec {

  class MockXmlRpcBalance extends XmlRpcBalance("") {
    var request: Option[Elem] = None
    var response: Option[Elem] = None

    def expectFrom(name: String): Unit = {
      request = Some(scala.xml.XML.load(getClass.getResourceAsStream(name)))
    }

    def respondFrom(name: String): Unit = {
      response = Some(scala.xml.XML.load(getClass.getResourceAsStream(name)))
    }

    def reset(): Unit = {
      request = None
      response = None
    }

    override def execute(v: MethodCallValue): Value = {
      println(s"executed $v")
      import Matchers.convertToAnyShouldWrapper
      request.map(Parsers.asMethodCall).foreach(v shouldBe _)

      Parsers.asMethodResponse(response.get)
    }
  }
}

class SomeSpec extends AnyWordSpec with Matchers
