package ru.yandex.vertis.billing.balance.xmlrpc.model

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.billing.balance.model.{ClientProperties, ClientRequest, ClientTypes}

/**
  * Created by IntelliJ IDEA.
  * User: alesavin
  * Date: 17.09.14
  * Time: 16:09
  */
class ModelSpec extends AnyFlatSpec with Matchers {

  "DateTime" should "serialize" in {
    println(DateTimeValue(DateTime.now(DateTimeZone.forID("Europe/Moscow"))).asXml)
  }

  "ClientRequest" should "check fields" in {
    intercept[IllegalArgumentException] {
      ClientRequest(null, ClientProperties(clientType = ClientTypes.IndividualPerson))
    }
    ClientRequest(None, ClientProperties(clientType = ClientTypes.IndividualPerson))
    ClientRequest(Some(1), ClientProperties(clientType = ClientTypes.IndividualPerson))

    intercept[IllegalArgumentException] {
      ClientRequest(None, ClientProperties(clientType = ClientTypes.IndividualPerson, name = null))
    }
    intercept[IllegalArgumentException] {
      ClientRequest(None, ClientProperties(clientType = ClientTypes.IndividualPerson, name = Some(null)))
    }
    ClientRequest(None, ClientProperties(clientType = ClientTypes.IndividualPerson, name = None))
    ClientRequest(None, ClientProperties(clientType = ClientTypes.IndividualPerson, name = Some("")))

    intercept[IllegalArgumentException] {
      ClientRequest(None, ClientProperties(clientType = ClientTypes.IndividualPerson, email = null))
    }
    intercept[IllegalArgumentException] {
      ClientRequest(None, ClientProperties(clientType = ClientTypes.IndividualPerson, email = Some(null)))
    }
    ClientRequest(None, ClientProperties(clientType = ClientTypes.IndividualPerson, email = None))
    ClientRequest(None, ClientProperties(clientType = ClientTypes.IndividualPerson, email = Some("")))
  }
}
