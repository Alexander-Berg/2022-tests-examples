package ru.yandex.vertis.billing.service.security

import org.mockito.Mockito.verify
import org.scalatest.Assertions
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import ru.yandex.vertis.billing.model_core.CustomerId
import ru.yandex.vertis.billing.security.{
  agency,
  client,
  expectDeny,
  securityProviderMock,
  uid2operatorContext,
  AgencyUid,
  ClientUid,
  CustomerNotFoundException,
  OtherUid,
  SecurityManager,
  SuperUserUid
}
import ru.yandex.vertis.billing.service.ArchiveService
import ru.yandex.vertis.billing.service.ArchiveService.RecordTypes
import ru.yandex.vertis.mockito.MockitoSupport

import scala.util.Success

/**
  * Specs on [[SecureArchiveService]]
  *
  * @author alex-kovalenko
  */
class SecureArchiveServiceSpec extends AnyWordSpec with Matchers with MockitoSupport {

  val comment = "-"
  val campaignId = "id"

  val service = {
    val m = MockitoSugar.mock[ArchiveService]
    when(m.archiveCampaign(?, ?, ?)(?)).thenReturn(Success(()))
    when(m.get(?, ?)(?)).thenReturn(Success(Iterable.empty))
    m
  }

  val security = new SecurityManager {
    val security = securityProviderMock
  }

  val secureService = new SecureArchiveService(service, security)

  "SecureArchiveService" should {
    "archive agency customer campaign only by SuperUser and agency" in {
      val customerId = CustomerId(client.id, Some(agency.id))

      secureService.archiveCampaign(customerId, campaignId, comment)(SuperUserUid)
      verify(service).archiveCampaign(customerId, campaignId, comment)(SuperUserUid)

      secureService.archiveCampaign(customerId, campaignId, comment)(AgencyUid)
      verify(service).archiveCampaign(customerId, campaignId, comment)(AgencyUid)

      expectDeny {
        secureService.archiveCampaign(customerId, campaignId, comment)(ClientUid).get
      }

      Assertions.intercept[CustomerNotFoundException] {
        secureService.archiveCampaign(customerId, campaignId, comment)(OtherUid).get
      }
    }

    "archive direct customer campaign only by SuperUser and client" in {
      val customerId = CustomerId(client.id, None)

      secureService.archiveCampaign(customerId, campaignId, comment)(SuperUserUid)
      verify(service).archiveCampaign(customerId, campaignId, comment)(SuperUserUid)

      secureService.archiveCampaign(customerId, campaignId, comment)(ClientUid)
      verify(service).archiveCampaign(customerId, campaignId, comment)(ClientUid)

      expectDeny {
        secureService.archiveCampaign(customerId, campaignId, comment)(AgencyUid).get
      }

      Assertions.intercept[CustomerNotFoundException] {
        secureService.archiveCampaign(customerId, campaignId, comment)(OtherUid).get
      }
    }

    "get agency customer records only by SuperUser, agency and client" in {
      val customerId = CustomerId(client.id, Some(agency.id))
      val filter = ArchiveService.Filter.ForCustomer(RecordTypes.Campaign, customerId)

      secureService.get(filter, readFromMaster = false)(SuperUserUid)
      verify(service).get(filter, readFromMaster = false)(SuperUserUid)

      secureService.get(filter, readFromMaster = false)(AgencyUid)
      verify(service).get(filter, readFromMaster = false)(AgencyUid)

      secureService.get(filter, readFromMaster = false)(ClientUid)
      verify(service).get(filter, readFromMaster = false)(ClientUid)

      Assertions.intercept[CustomerNotFoundException] {
        secureService.get(filter)(OtherUid).get
      }
    }

    "get direct customer records only by SuperUser and agency" in {
      val customerId = CustomerId(client.id, None)
      val filter = ArchiveService.Filter.ForCustomer(RecordTypes.Campaign, customerId)

      secureService.get(filter, readFromMaster = false)(SuperUserUid)
      verify(service).get(filter, readFromMaster = false)(SuperUserUid)

      secureService.get(filter, readFromMaster = false)(ClientUid)
      verify(service).get(filter, readFromMaster = false)(ClientUid)

      expectDeny {
        secureService.get(filter)(AgencyUid).get
      }

      Assertions.intercept[CustomerNotFoundException] {
        secureService.get(filter)(OtherUid).get
      }
    }
  }

}
