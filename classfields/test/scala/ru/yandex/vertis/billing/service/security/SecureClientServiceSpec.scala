package ru.yandex.vertis.billing.service.security

import org.mockito.Mockito.{reset, verify}
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.gens.{AgencyGen, ClientGen, Producer}
import ru.yandex.vertis.billing.security._
import ru.yandex.vertis.billing.service.ClientService
import ru.yandex.vertis.billing.util.Page
import ru.yandex.vertis.mockito.MockitoSupport

/**
  * Specs on [[SecureClientService]] wrapper.
  *
  * @author dimas
  */
class SecureClientServiceSpec extends AnyWordSpec with Matchers with BeforeAndAfter with MockitoSupport {

  val service = mock[ClientService]

  val security = new SecurityManager {
    val security = securityProviderMock
  }

  val secureService = new SecureClientService(service, security)

  before {
    reset(service)
  }

  "SecureClientService" should {
    "delegate join call for SuperUser" in {
      val clientId = 42

      secureService.join(clientId)(SuperUserUid)
      verify(service).join(clientId, direct = false)(SuperUserUid)
    }

    "throw AccessDenyException on join call for other users" in {
      val clientId = 42

      expectDeny {
        secureService.join(clientId)(ClientUid).get
      }
      expectDeny {
        secureService.join(clientId)(AgencyUid).get
      }
    }

    val slice = Page(0, 10)

    "find by Query.All only for SuperUser" in {
      val query = ClientService.Query.All
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
      val query = ClientService.Query.ByType(isAgency = false)
      secureService.find(query, slice)(SuperUserUid)
      verify(service).find(query, slice)(SuperUserUid)

      expectDeny {
        secureService.find(query, slice)(AgencyUid).get
      }
      expectDeny {
        secureService.find(query, slice)(ClientUid).get
      }
    }

    "find by Query.ForAgency for SuperUser" in {
      val query = ClientService.Query.ForAgency(42)
      secureService.find(query, slice)(SuperUserUid)
      verify(service).find(query, slice)(SuperUserUid)

      expectDeny {
        secureService.find(query, slice)(AgencyUid).get
      }
      expectDeny {
        secureService.find(query, slice)(ClientUid).get
      }
    }

    "find by Query.ForAgency for this agency" in {
      val query = ClientService.Query.ForAgency(agency.id)
      secureService.find(query, slice)(SuperUserUid)
      verify(service).find(query, slice)(SuperUserUid)

      secureService.find(query, slice)(AgencyUid)
      verify(service).find(query, slice)(AgencyUid)

      expectDeny {
        secureService.find(query, slice)(ClientUid).get
      }
    }

    "create agency only by SuperUser" in {
      val properties = AgencyGen.next.properties
      secureService.create(properties)(SuperUserUid)
      verify(service).create(properties)(SuperUserUid)

      expectDeny {
        secureService.create(properties)(AgencyUid).get
      }

      expectDeny {
        secureService.create(properties)(ClientUid).get
      }
    }

    "create agency client by SuperUser and agency" in {
      val properties = ClientGen.next.properties.copy(agencyId = Some(agency.id), isAgency = false)
      secureService.create(properties)(SuperUserUid)
      verify(service).create(properties)(SuperUserUid)

      secureService.create(properties)(AgencyUid)
      verify(service).create(properties)(AgencyUid)

      expectDeny {
        secureService.create(properties)(ClientUid).get
      }
    }
  }
}
