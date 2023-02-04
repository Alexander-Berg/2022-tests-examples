package ru.yandex.vertis.billing.indexer

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.DefaultPropertyChecks
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.dao.impl.mds.S3CampaignStorage
import ru.yandex.vertis.billing.service.CampaignService.{EnabledCampaignsFilter, Filter}
import ru.yandex.vertis.billing.service.async.AsyncCampaignService
import ru.yandex.vertis.billing.service.impl.CampaignWithStatusAndLimitsEnricher
import ru.yandex.vertis.billing.service.{AsyncCampaignProviderImpl, CampaignService}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

/**
  * Specs on [[ru.yandex.vertis.billing.service.AsyncCampaignProvider]]
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
class AsyncCampaignProviderSpec
  extends AnyWordSpec
  with Matchers
  with MockitoSupport
  with DefaultPropertyChecks
  with AsyncSpecBase {

  private val notValuable = Iterable(
    DisabledCampaign,
    NotEnoughFundsCampaign,
    CampaignWithDailyLimitOverdrafted,
    CampaignWithWeeklyLimitOverdrafted
  )

  val campaignService = mock[AsyncCampaignService]

  val campaignEnricher = new CampaignWithStatusAndLimitsEnricher(limitService, orderDao)

  val campaignProvider =
    new AsyncCampaignProviderImpl(campaignService, AsyncCampaignProviderSpec.s3CampaignStorage, campaignEnricher)

  "AsyncCampaignProvider" should {
    "get active campaigns from campaignService as is" in {
      when(campaignService.getEnabled(?)(?))
        .thenReturn(Future.successful(Iterable(EnabledCampaign, DisabledCampaign)))
      val headers = campaignProvider.getEnabled(CampaignService.EnabledCampaignsFilter.All).futureValue
      headers.map(_.getId) should contain theSameElementsAs Iterable(EnabledCampaign.id)
    }

    "get active campaign by Id as is" in {
      when(campaignService.getIfEnabled(?)(?))
        .thenReturn(Future.successful(EnabledCampaign))
      val header = campaignProvider.getIfActive(EnabledCampaign.id).futureValue
      header.getId shouldBe EnabledCampaign.id
    }

    "fail when no active campaign by Id is present" in {
      when(campaignService.getIfEnabled(?)(?))
        .thenReturn(Future.failed(new NoSuchElementException))
      intercept[NoSuchElementException] {
        campaignProvider.getIfActive("not_exists").toTry.get
      }
    }

    "skip not valuable with status" in {
      when(campaignService.getEnabled(?)(?))
        .thenReturn(Future.successful(notValuable))

      val headers = campaignProvider.getValuable(CampaignService.EnabledCampaignsFilter.All).futureValue
      headers.size shouldBe 0
    }

    "skip not valuable without status" in {
      val notValuableWithoutStatus = notValuable.map { c =>
        c.copy(status = None)
      }
      when(campaignService.getEnabled(?)(?))
        .thenReturn(Future.successful(notValuableWithoutStatus))

      val headers = campaignProvider.getValuable(CampaignService.EnabledCampaignsFilter.All).futureValue
      headers.size shouldBe 0
    }

    "get disabled and inactive campaign" in {
      when(campaignService.get(any[Filter]())(?))
        .thenReturn(Future.successful(Iterable(EnabledCampaign, DisabledCampaign, InactiveCampaign)))

      val headers = campaignProvider.getInactive().futureValue
      headers.map(_.getId) should contain theSameElementsAs Seq("disabled", "inactive")
    }
  }

}

object AsyncCampaignProviderSpec extends MockitoSupport {

  val s3CampaignStorage = {
    val m = mock[S3CampaignStorage]
    when(m.getCampaigns(?))
      .thenReturn(Future.failed(new NoSuchElementException("artificial")))
    m
  }
}
