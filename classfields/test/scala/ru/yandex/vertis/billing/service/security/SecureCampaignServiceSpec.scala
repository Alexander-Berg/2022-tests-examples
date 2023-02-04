package ru.yandex.vertis.billing.service.security

import org.mockito.Mockito.verify
import org.scalatest.Assertions
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.{EmptyTransactionContext, TransactionContext}
import ru.yandex.vertis.billing.model_core.FixPrice.unitsToFixPrice
import ru.yandex.vertis.billing.model_core.gens.Producer
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.security.{
  agency,
  client,
  expectDeny,
  securityProviderMock,
  uid2operatorContext,
  AgencyUid,
  ClientUid,
  CustomerNotFoundException,
  OtherClientUid,
  OtherUid,
  SecurityManager,
  SuperUserUid
}
import ru.yandex.vertis.billing.service.CampaignService
import ru.yandex.vertis.billing.util.Page
import ru.yandex.vertis.mockito.MockitoSupport

/**
  * Specs on [[SecureCampaignService]] wrapper
  *
  * @author ruslansd
  */
class SecureCampaignServiceSpec extends AnyWordSpec with Matchers with MockitoSupport {

  val service = mock[CampaignService]

  val security = new SecurityManager {
    val security = securityProviderMock
  }

  val secureService = new SecureCampaignService(service, security)

  val tc: TransactionContext = EmptyTransactionContext

  "SecureCampaignService" should {

    "update agency customer only by SuperUser and agency" in {
      val campaignId = "id"
      val patch = CampaignService.Patch()
      val clientId = client.id
      val agencyId = agency.id
      val customerId = new CustomerId(clientId, Some(agencyId))

      secureService.update(customerId, campaignId, patch)(SuperUserUid, tc)
      verify(service).update(customerId, campaignId, patch)(SuperUserUid, tc)

      secureService.update(customerId, campaignId, patch)(AgencyUid, tc)
      verify(service).update(customerId, campaignId, patch)(AgencyUid, tc)

      expectDeny {
        secureService.update(customerId, campaignId, patch)(ClientUid, tc).get
      }
    }

    "update direct customer only by SuperUser and client" in {
      val campaignId = "id"
      val patch = CampaignService.Patch()
      val clientId = client.id
      val customerId = new CustomerId(clientId, None)

      secureService.update(customerId, campaignId, patch)(SuperUserUid, tc)
      verify(service).update(customerId, campaignId, patch)(SuperUserUid, tc)

      secureService.update(customerId, campaignId, patch)(ClientUid, tc)
      verify(service).update(customerId, campaignId, patch)(ClientUid, tc)

      expectDeny {
        secureService.update(customerId, campaignId, patch)(AgencyUid, tc).get
      }
    }

    "getOffers agency customer only by SuperUser and agency" in {
      val campaignId = "id"
      val slice = Page(0, 10)
      val clientId = client.id
      val agencyId = agency.id
      val customerId = new CustomerId(clientId, Some(agencyId))

      secureService.getOffers(customerId, campaignId, slice)(SuperUserUid)
      verify(service).getOffers(customerId, campaignId, slice)(SuperUserUid)

      secureService.getOffers(customerId, campaignId, slice)(AgencyUid)
      verify(service).getOffers(customerId, campaignId, slice)(AgencyUid)

      secureService.getOffers(customerId, campaignId, slice)(ClientUid)
      verify(service).getOffers(customerId, campaignId, slice)(ClientUid)
    }

    "getOffers direct customer only by SuperUser and client" in {
      val campaignId = "id"
      val slice = Page(0, 10)
      val clientId = client.id
      val customerId = new CustomerId(clientId, None)

      secureService.getOffers(customerId, campaignId, slice)(SuperUserUid)
      verify(service).getOffers(customerId, campaignId, slice)(SuperUserUid)

      secureService.getOffers(customerId, campaignId, slice)(ClientUid)
      verify(service).getOffers(customerId, campaignId, slice)(ClientUid)

      expectDeny {
        secureService.getOffers(customerId, campaignId, slice)(AgencyUid).get
      }
    }

    "find agency customer only by SuperUser, agency and client" in {
      val offers = gens.OfferIdGen.next(1)
      val clientId = client.id
      val agencyId = agency.id
      val customerId = new CustomerId(clientId, Some(agencyId))

      secureService.getOffers(customerId, offers)(SuperUserUid)
      verify(service).getOffers(customerId, offers)(SuperUserUid)

      secureService.getOffers(customerId, offers)(AgencyUid)
      verify(service).getOffers(customerId, offers)(AgencyUid)

      secureService.getOffers(customerId, offers)(ClientUid)
      verify(service).getOffers(customerId, offers)(ClientUid)

      Assertions.intercept[CustomerNotFoundException] {
        secureService.getOffers(customerId, offers)(OtherUid).get
      }
    }

    "find direct customer only by SuperUser and client" in {
      val offers = gens.OfferIdGen.next(1)
      val clientId = client.id
      val customerId = new CustomerId(clientId, None)

      secureService.getOffers(customerId, offers)(SuperUserUid)
      verify(service).getOffers(customerId, offers)(SuperUserUid)

      secureService.getOffers(customerId, offers)(ClientUid)
      verify(service).getOffers(customerId, offers)(ClientUid)

      expectDeny {
        secureService.getOffers(customerId, offers)(AgencyUid).get
      }
    }

    "delete agency customer only by SuperUser and agency" in {
      val campaignId = "id"
      val clientId = client.id
      val agencyId = agency.id
      val customerId = new CustomerId(clientId, Some(agencyId))

      secureService.delete(customerId, campaignId)(SuperUserUid)
      verify(service).delete(customerId, campaignId)(SuperUserUid)

      secureService.delete(customerId, campaignId)(AgencyUid)
      verify(service).delete(customerId, campaignId)(AgencyUid)

      expectDeny {
        secureService.delete(customerId, campaignId)(ClientUid).get
      }
    }

    "delete direct customer only by SuperUser and client" in {
      val campaignId = "id"
      val clientId = client.id
      val customerId = new CustomerId(clientId, None)

      secureService.delete(customerId, campaignId)(SuperUserUid)
      verify(service).delete(customerId, campaignId)(SuperUserUid)

      secureService.delete(customerId, campaignId)(ClientUid)
      verify(service).delete(customerId, campaignId)(ClientUid)

      expectDeny {
        secureService.delete(customerId, campaignId)(AgencyUid).get
      }
    }

    import CampaignService.Filter

    "list agency customer campaigns by SuperUser, agency and client" in {
      val slice = Page(0, 10)
      val clientId = client.id
      val agencyId = agency.id
      val customerId = new CustomerId(clientId, Some(agencyId))

      val filter = Filter.ForCustomer(customerId)

      secureService.list(filter, slice)(SuperUserUid)
      verify(service).list(filter, slice)(SuperUserUid)

      secureService.list(filter, slice)(AgencyUid)
      verify(service).list(filter, slice)(AgencyUid)

      secureService.list(filter, slice)(ClientUid)
      verify(service).list(filter, slice)(ClientUid)

      expectDeny {
        secureService.list(filter, slice)(OtherClientUid).get
      }
    }

    "list customers for client only by SuperUser and client" in {
      val slice = Page(0, 10)
      val clientId = client.id

      val filter = Filter.ForClient(clientId)

      secureService.list(filter, slice)(SuperUserUid)
      verify(service).list(filter, slice)(SuperUserUid)

      secureService.list(filter, slice)(ClientUid)
      verify(service).list(filter, slice)(ClientUid)

      expectDeny {
        secureService.list(filter, slice)(AgencyUid).get
      }
    }

    "list direct customer only by SuperUser and client" in {
      val slice = Page(0, 10)
      val clientId = client.id
      val customerId = new CustomerId(clientId, None)

      val filter = Filter.ForCustomer(customerId)

      secureService.list(filter, slice)(SuperUserUid)
      verify(service).list(filter, slice)(SuperUserUid)

      secureService.list(filter, slice)(ClientUid)
      verify(service).list(filter, slice)(ClientUid)

      expectDeny {
        secureService.list(filter, slice)(AgencyUid).get
      }
    }

    "get agency customer only by SuperUser, agency" in {
      val campaignId = "id"
      val clientId = client.id
      val agencyId = agency.id
      val customerId = new CustomerId(clientId, Some(agencyId))

      secureService.get(customerId, campaignId)(SuperUserUid)
      verify(service).get(customerId, campaignId)(SuperUserUid)

      secureService.get(customerId, campaignId)(AgencyUid)
      verify(service).get(customerId, campaignId)(AgencyUid)

      secureService.get(customerId, campaignId)(ClientUid)
      verify(service).get(customerId, campaignId)(ClientUid)

      expectDeny {
        secureService.get(customerId, campaignId)(OtherClientUid).get
      }
    }

    "get direct customer only by SuperUser and client" in {
      val campaignId = "id"
      val clientId = client.id
      val customerId = new CustomerId(clientId, None)

      secureService.get(customerId, campaignId)(SuperUserUid)
      verify(service).get(customerId, campaignId)(SuperUserUid)

      secureService.get(customerId, campaignId)(ClientUid)
      verify(service).get(customerId, campaignId)(ClientUid)

      expectDeny {
        secureService.get(customerId, campaignId)(AgencyUid).get
      }
    }

    val product = Product(Raising(CostPerMille(100L)))
    val source = CampaignService.Source(
      None,
      10,
      product,
      CampaignSettings.Default.copy(isEnabled = false),
      None,
      List(PartnerOfferId("partner", "offer"))
    )

    "create agency customer only by SuperUser and agency" in {
      val clientId = client.id
      val agencyId = agency.id
      val customerId = new CustomerId(clientId, Some(agencyId))

      secureService.create(customerId, source)(SuperUserUid, tc)
      verify(service).create(customerId, source)(SuperUserUid, tc)

      secureService.create(customerId, source)(AgencyUid, tc)
      verify(service).create(customerId, source)(AgencyUid, tc)

      expectDeny {
        secureService.create(customerId, source)(ClientUid, tc).get
      }
    }

    "create direct customer only by SuperUser and client" in {
      val clientId = client.id
      val customerId = new CustomerId(clientId, None)

      secureService.create(customerId, source)(SuperUserUid, tc)
      verify(service).create(customerId, source)(SuperUserUid, tc)

      secureService.create(customerId, source)(ClientUid, tc)
      verify(service).create(customerId, source)(ClientUid, tc)

      expectDeny {
        secureService.create(customerId, source)(AgencyUid, tc).get
      }
    }
  }
}
