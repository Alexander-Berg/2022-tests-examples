package ru.yandex.vertis.billing.security

import org.scalatest.Assertions
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.balance.model.Balance
import ru.yandex.vertis.billing.model_core.{CustomerId, User}
import ru.yandex.vertis.billing.util.OperatorContext
import ru.yandex.vertis.mockito.MockitoSupport.{?, mock, when}

import scala.util.Success

/**
  * Specs on [[SecurityProvider]]
  *
  * @author dimas
  */
class SecurityProviderSpec extends AnyWordSpec with Matchers {

  val superContext = OperatorContext("test", SuperUserUid)
  val superAutoRuContext = OperatorContext("test", SuperUserAutoRuUid)
  val autoRuContext = OperatorContext("test", AutoRuUser)
  val ClientContext = OperatorContext("test", ClientUid)

  import GrantModes.{ExclusiveRW, Read => R, ReadWrite => RW}

  private def testerForUid(user: User, context: OperatorContext)(grant: Grant, expectedWithGrant: Boolean) = {
    securityProviderMock.get(user)(context).get.contains(grant) shouldBe expectedWithGrant
    securityProviderMock.get(user, Some(grant))(context).get.contains(grant) shouldBe expectedWithGrant
  }

  private def testerForAutoRuUid(user: User, context: OperatorContext)(grant: Grant, expectedWithGrant: Boolean) = {
    securityProviderMock.get(user)(context).get.contains(grant) shouldBe false
    securityProviderMock.get(user, Some(grant))(context).get.contains(grant) shouldBe expectedWithGrant
  }

  "SecurityProvider" should {
    "provide SecurityContext for SuperUser with Grant.All" in {
      val context = securityProviderMock.get(SuperUserUid)(SuperUserUid).get
      context.contains(Grant.All) should be(true)
    }

    "provide SecurityContext for RegularUser without Grant.All" in {
      val context = securityProviderMock.get(ClientUid)(ClientUid).get
      context.contains(Grant.All) should be(false)
    }

    "provide SecurityContext for SuperUserAutoRuUid with Grant.All" in {
      securityProviderMock.get(SuperUserAutoRuUid, Some(Grant.All))(superAutoRuContext) match {
        case Success(context) =>
          context.contains(Grant.All) should be(true)
        case other => fail(s"Unexpected $other")
      }
    }

    "provide SecurityContext for direct client user with grants on user, client and customer" in {
      val tester = testerForUid(ClientUid, ClientUid)(_, _)

      tester(Grant.OnUser(ClientUid, R), true)
      tester(Grant.OnUser(ClientUid, RW), true)
      tester(Grant.OnUser(AgencyUid, R), false)
      tester(Grant.OnUser(AgencyUid, RW), false)

      tester(Grant.OnClient(client.id, R), true)
      tester(Grant.OnClient(client.id, RW), true)
      tester(Grant.OnClient(agency.id, R), false)
      tester(Grant.OnClient(agency.id, RW), false)

      tester(Grant.OnCustomer(CustomerId(client.id, None), R), true)
      tester(Grant.OnCustomer(CustomerId(client.id, None), RW), true)
      tester(Grant.OnCustomer(CustomerId(client.id, Some(agency.id)), R), true)
      tester(Grant.OnCustomer(CustomerId(client.id, Some(agency.id)), RW), false)

    }

    "provide SecurityContext for agency client user with grants on user, client and customers" in {
      val tester = testerForUid(AgencyUid, AgencyUid)(_, _)

      tester(Grant.OnUser(AgencyUid, R), true)
      tester(Grant.OnUser(AgencyUid, RW), true)
      tester(Grant.OnUser(ClientUid, R), false)
      tester(Grant.OnUser(ClientUid, RW), false)

      tester(Grant.OnClient(agency.id, R), true)
      tester(Grant.OnClient(agency.id, RW), true)
      tester(Grant.OnClient(client.id, R), false)
      tester(Grant.OnClient(client.id, RW), false)

      tester(Grant.OnCustomer(CustomerId(client.id, Some(agency.id)), R), true)
      tester(Grant.OnCustomer(CustomerId(client.id, Some(agency.id)), RW), true)
    }

    "provide SecurityContext for direct/agency client for autoru with OnUser grants" in {
      val testerEqual = testerForUid(AutoRuUser, autoRuContext)(_, _)

      testerEqual(Grant.OnUser(AgencyUid, R), false)
      testerEqual(Grant.OnUser(AgencyUid, RW), false)
      testerEqual(Grant.OnUser(ClientUid, R), false)
      testerEqual(Grant.OnUser(ClientUid, RW), false)

      testerEqual(Grant.OnUser(AutoRuUser, R), true)
      testerEqual(Grant.OnUser(AutoRuUser, RW), true)
    }

    "provide SecurityContext for direct/agency client with OnClient/OnCustomer grants" in {
      val testerEqual = testerForAutoRuUid(AutoRuUser, autoRuContext)(_, _)

      testerEqual(Grant.OnClient(autoruClient.id, R), true)
      testerEqual(Grant.OnClient(autoruClient.id, RW), true)

      testerEqual(Grant.OnCustomer(CustomerId(autoruClient), R), true)
      testerEqual(Grant.OnCustomer(CustomerId(autoruClient), RW), true)
    }

    "throw NoSuchElementException for non-existent autoru client" in {
      def testerException(grant: Grant) =
        Assertions.intercept[CustomerNotFoundException] {
          securityProviderMock.get(AutoRuUser, Some(grant))(autoRuContext).get
        }

      testerException(Grant.OnClient(agency.id, R))
      testerException(Grant.OnClient(agency.id, RW))
      testerException(Grant.OnClient(client.id, R))
      testerException(Grant.OnClient(client.id, RW))
    }

    "provide exclusive grants only for customers (direct client)" in {
      val tester = testerForUid(ClientUid, ClientUid)(_, _)
      val superUserTester = testerForUid(SuperUserUid, SuperUserUid)(_, _)

      tester(Grant.OnUser(ClientUid, R), true)
      tester(Grant.OnUser(ClientUid, ExclusiveRW), true)
      superUserTester(Grant.OnUser(ClientUid, R), true)
      superUserTester(Grant.OnUser(ClientUid, ExclusiveRW), false)

      tester(Grant.OnClient(client.id, R), true)
      tester(Grant.OnClient(client.id, ExclusiveRW), true)
      superUserTester(Grant.OnClient(client.id, R), true)
      superUserTester(Grant.OnClient(client.id, ExclusiveRW), false)

      tester(Grant.OnCustomer(CustomerId(client.id, None), R), true)
      tester(Grant.OnCustomer(CustomerId(client.id, None), ExclusiveRW), true)
      superUserTester(Grant.OnCustomer(CustomerId(client.id, None), R), true)
      superUserTester(Grant.OnCustomer(CustomerId(client.id, None), ExclusiveRW), false)
    }

    "throw NoSuchElementException for non-existent client" in {
      val balance = mock[Balance]
      when(balance.getPassportByUid(?)(?)).thenReturn(Success(None))

      val securityProvider = new SecurityProviderImpl(rolesMock, Some(balance), Some(customersMock))

      Assertions.intercept[CustomerNotFoundException] {
        securityProvider.get(ClientUid)(ClientContext).get
      }
    }

  }

}
