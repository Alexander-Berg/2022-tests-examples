package ru.yandex.vertis.billing.service

import org.mockito.Mockito.{never, times, verify}
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.dao.CampaignDao.DuplicationPolicy
import ru.yandex.vertis.billing.dao.{BindingDao, CampaignCallDao, CampaignDao}
import ru.yandex.vertis.billing.model_core.gens.{CampaignHeaderGen, CustomerHeaderGen, OfferIdGen}
import ru.yandex.vertis.billing.model_core.{Uid, Update}
import ru.yandex.vertis.billing.service.CampaignService.Patch
import ru.yandex.vertis.billing.service.impl.CampaignServiceImpl
import ru.yandex.vertis.billing.util.{OperatorContext, RequestContext}
import ru.yandex.vertis.generators.ProducerProvider._
import ru.yandex.vertis.mockito.MockitoSupport

import scala.util.Success

class HistoryCampaignServiceSpec
  extends AnyWordSpec
  with Matchers
  with MockitoSupport
  with AsyncSpecBase
  with OneInstancePerTest {

  implicit private val rc: RequestContext = OperatorContext("test", Uid(0L))

  private val campaignDao = mock[CampaignDao]
  private val bindingDao = mock[BindingDao]
  private val callDao = mock[CampaignCallDao]
  private val historyService = mock[CampaignHistoryService]

  private val campaignService =
    new CampaignServiceImpl(campaignDao, bindingDao, callDao, DuplicationPolicy.AllowDuplicates)
      with HistoryCampaignService {
      override def historyService: Option[CampaignHistoryService] = Some(HistoryCampaignServiceSpec.this.historyService)
    }

  "HistoryCampaignService" should {

    "not store history when only inactive status was updated" in {
      // given
      val customer = CustomerHeaderGen.next
      val campaign = CampaignHeaderGen.next
      val patch = Patch(
        inactiveChange = Some(Update(None))
      )

      when(campaignDao.update(?, ?, ?, ?)(?)).thenReturn(Success(campaign))

      // when
      campaignService.update(customer.id, campaign.id, patch)

      // then
      verify(historyService, never()).store(?)(?)
    }

    "store history entry when campaign was updated" in {
      // given
      val customer = CustomerHeaderGen.next
      val campaign = CampaignHeaderGen.next
      val patch = Patch(
        include = Set(OfferIdGen.next),
        inactiveChange = Some(Update(None))
      )

      when(campaignDao.update(?, ?, ?, ?)(?)).thenReturn(Success(campaign))

      // when
      campaignService.update(customer.id, campaign.id, patch)

      // then
      verify(historyService, times(1)).store(?)(?)
    }

  }

}
