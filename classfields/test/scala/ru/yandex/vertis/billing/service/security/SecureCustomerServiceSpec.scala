package ru.yandex.vertis.billing.service.security

import org.mockito.Mockito.verify
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.CustomerDao
import ru.yandex.vertis.billing.model_core.{CustomerId, Site}
import ru.yandex.vertis.billing.security._
import ru.yandex.vertis.billing.service.CustomerService
import ru.yandex.vertis.billing.util.Page
import ru.yandex.vertis.mockito.MockitoSupport

/**
  *Specs on [[SecureCustomerService]] wrapper
  *
  * @author ruslansd
  */
class SecureCustomerServiceSpec extends AnyWordSpec with Matchers with MockitoSupport {

  val service = mock[CustomerService]

  val security = new SecurityManager {
    val security = securityProviderMock
  }

  val secureService = new SecureCustomerService(service, security)

  "SecureCustomerService" should {
    "get agency client for SuperUser, agency and client" in {
      val clientId = client.id
      val agencyId = agency.id
      val customerId = new CustomerId(clientId, Some(agencyId))

      secureService.get(customerId)(SuperUserUid)
      verify(service).get(customerId)(SuperUserUid)

      secureService.get(customerId)(AgencyUid)
      verify(service).get(customerId)(AgencyUid)

      secureService.get(customerId)(ClientUid)
      verify(service).get(customerId)(ClientUid)

      expectDeny {
        secureService.get(customerId)(OtherClientUid).get
      }
    }

    "get direct client only for SuperUser and client" in {
      val clientId = client.id
      val customerId = new CustomerId(clientId, None)

      secureService.get(customerId)(SuperUserUid)
      verify(service).get(customerId)(SuperUserUid)

      secureService.get(customerId)(ClientUid)
      verify(service).get(customerId)(ClientUid)

      expectDeny {
        secureService.get(customerId)(AgencyUid).get
      }
    }

    "create agency client only for SuperUser and agency" in {
      val clientId = client.id
      val agencyId = agency.id
      val customerId = new CustomerId(clientId, Some(agencyId))
      val resource = Site("www.test.com")

      secureService.create(customerId, resource)(SuperUserUid)
      verify(service).create(customerId, resource)(SuperUserUid)

      secureService.create(customerId, resource)(AgencyUid)
      verify(service).create(customerId, resource)(AgencyUid)

      expectDeny {
        secureService.create(customerId, resource)(ClientUid).get
      }
    }

    "create direct client only for SuperUser and client" in {
      val clientId = client.id
      val customerId = new CustomerId(clientId, None)
      val resource = Site("www.test.com")

      secureService.create(customerId, resource)(SuperUserUid)
      verify(service).create(customerId, resource)(SuperUserUid)

      secureService.create(customerId, resource)(ClientUid)
      verify(service).create(customerId, resource)(ClientUid)

      expectDeny {
        secureService.create(customerId, resource)(AgencyUid).get
      }
    }

    val slice = Page(0, 10)

    "find by Query.All only for SuperUser" in {
      val query = CustomerDao.Query.All

      secureService.find(query, slice)(SuperUserUid)
      verify(service).find(query, slice)(SuperUserUid)

      expectDeny {
        secureService.find(query, slice)(AgencyUid).get
      }

      expectDeny {
        secureService.find(query, slice)(ClientUid).get
      }
    }

    "find by Query.ByType only for SuperUser" in {
      val query = CustomerDao.Query.ByType(isAgency = false)

      secureService.find(query, slice)(SuperUserUid)
      verify(service).find(query, slice)(SuperUserUid)

      expectDeny {
        secureService.find(query, slice)(AgencyUid).get
      }

      expectDeny {
        secureService.find(query, slice)(ClientUid).get
      }
    }

    "find Query.ForClient only for SuperUser and client" in {
      val clientId = client.id
      val query = CustomerDao.Query.ForClient(clientId)

      secureService.find(query, slice)(SuperUserUid)
      verify(service).find(query, slice)(SuperUserUid)

      secureService.find(query, slice)(ClientUid)
      verify(service).find(query, slice)(ClientUid)

      expectDeny {
        secureService.find(query, slice)(AgencyUid).get
      }
    }

    "find Query.ForAgency only for SuperUser and agency" in {
      val agencyId = agency.id
      val query = CustomerDao.Query.ForAgency(agencyId)

      secureService.find(query, slice)(SuperUserUid)
      verify(service).find(query, slice)(SuperUserUid)

      secureService.find(query, slice)(AgencyUid)
      verify(service).find(query, slice)(AgencyUid)

      expectDeny {
        secureService.find(query, slice)(ClientUid).get
      }
    }
  }
}
