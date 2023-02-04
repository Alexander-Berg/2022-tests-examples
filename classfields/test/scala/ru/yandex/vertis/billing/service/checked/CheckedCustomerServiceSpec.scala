package ru.yandex.vertis.billing.service.checked

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.balance.model.{Balance, ClientId}
import ru.yandex.vertis.billing.dao.impl.jdbc.{JdbcCustomerDao, JdbcSpecTemplate}
import ru.yandex.vertis.billing.model_core.Partner
import ru.yandex.vertis.billing.model_core.gens._
import ru.yandex.vertis.billing.service.ResourceService
import ru.yandex.vertis.billing.service.checking.CheckedCustomerService
import ru.yandex.vertis.billing.service.checking.CheckedCustomerService.{
  AgencyPartnerConsistencyPolicy,
  AllResourceConsistencyPolicy,
  ResourceConsistencyPolicy
}
import ru.yandex.vertis.billing.service.impl.{CustomerServiceImpl, SimpleResourceService}
import ru.yandex.vertis.billing.util.AutomatedContext
import ru.yandex.vertis.mockito.MockitoSupport

import scala.util.Success

/**
  * Spec on [[ru.yandex.vertis.billing.service.checking.CheckedCustomerService]].
  *
  * @author ruslansd
  */
class CheckedCustomerServiceSpec extends AnyWordSpec with Matchers with JdbcSpecTemplate with MockitoSupport {

  implicit private val rc = AutomatedContext("CheckedCustomerServiceSpec")
  private val dao = new JdbcCustomerDao(billingDatabase)
  private val balance = mock[Balance]

  stub(balance.getClientsById _) { case clientId =>
    Success(Some(ClientGen.next.copy(id = clientId)))

  }

  stub(balance.getClientsByIdBatch(_: Iterable[ClientId])) { case clientIds =>
    Success {
      clientIds.map { id =>
        ClientGen.next.copy(id = id)
      }
    }
  }

  private def service(rPolicy: ResourceConsistencyPolicy) =
    new CustomerServiceImpl(dao, SimpleResourceService, balance) with CheckedCustomerService {
      override def resourcesService: ResourceService = SimpleResourceService

      override def policy: ResourceConsistencyPolicy = rPolicy
    }

  "CheckedCustomerService" should {
    "allow create customer with uniq resource" in {
      val s = service(AllResourceConsistencyPolicy)
      val customerId = CustomerIdGen.next
      val resource = Partner("test_1")
      (s.create(customerId, resource) should be).a(Symbol("Success"))
    }

    "disallow create customer with resource duplicate resource" in {
      val s = service(AllResourceConsistencyPolicy)
      val customerId = CustomerIdGen.next
      val customer2Id = CustomerIdGen.next
      val resource = Partner("test_2")
      (s.create(customerId, resource) should be).a(Symbol("Success"))
      intercept[IllegalArgumentException] {
        s.create(customer2Id, resource).get
      }
    }

    "allow create with resource duplicate only for agency customer" in {
      val s = service(AgencyPartnerConsistencyPolicy)
      val agencyCustomer1 = CustomerIdGen.suchThat(_.agencyId.isDefined).next
      val agencyCustomer2 = CustomerIdGen.suchThat(_.agencyId.isDefined).next

      val resource = Partner("test_3")
      (s.create(agencyCustomer1, resource) should be).a(Symbol("Success"))
      (s.create(agencyCustomer2, resource) should be).a(Symbol("Success"))
    }

    "disallow create with resource duplicate only for agency customer (AllResourceConsistencyPolicy)" in {
      val s = service(AllResourceConsistencyPolicy)
      val agencyCustomer1 = CustomerIdGen.suchThat(_.agencyId.isDefined).next
      val agencyCustomer2 = CustomerIdGen.suchThat(_.agencyId.isDefined).next

      val resource = Partner("test_4")
      (s.create(agencyCustomer1, resource) should be).a(Symbol("Success"))
      intercept[IllegalArgumentException] {
        s.create(agencyCustomer2, resource).get
      }
    }

  }

}
