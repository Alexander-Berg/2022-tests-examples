package ru.yandex.vertis.billing.service.impl

import org.mockito.ArgumentCaptor
import org.mockito.Mockito.{times, verify, verifyNoInteractions}
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.gens.CampaignHeaderGen
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.service.CampaignService
import ru.yandex.vertis.billing.service.CampaignService.Patch
import ru.yandex.vertis.billing.util.{DateTimeUtils, OperatorContext, RequestContext}
import ru.yandex.vertis.generators.ProducerProvider._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => ~~}

import scala.util.{Failure, Success}

class CampaignInactiveSinceUpdaterSpec extends AnyWordSpec with OneInstancePerTest with Matchers with MockitoSupport {

  implicit private val rc: RequestContext = OperatorContext("test", Uid(0L))

  private val campaignServiceMock = mock[CampaignService]
  private val campaignInactiveSinceUpdater = new CampaignInactiveSinceUpdater(campaignServiceMock)

  private val noSpends = Spendings(Some(Spent.Daily(0)), Some(Spent.Weekly(0)), Some(Spent.Monthly(0)))

  "CampaignInactiveSinceUpdater" should {

    "clean inactiveSince field when campaign switched to active" in {
      // given
      val campaign = CampaignHeaderGen.next
        .copy(
          status = Some(CampaignStatus.Active(noSpends)),
          inactiveSince = Some(DateTimeUtils.now())
        )

      when(campaignServiceMock.update(?, ?, ?)(?, ?))
        .thenReturn(Success(campaign))

      // when
      campaignInactiveSinceUpdater.update(Seq(campaign))

      // then
      verify(campaignServiceMock, times(1))
        .update(~~(campaign.customer.id), ~~(campaign.id), ~~(Patch(inactiveChange = Some(Update(None)))))
    }

    "set inactiveSince field when campaign switched to inactive" in {
      // given
      val campaign = CampaignHeaderGen.next
        .copy(
          status = Some(CampaignStatus.Inactive(InactiveReasons.NoEnoughFunds)),
          inactiveSince = None
        )

      when(campaignServiceMock.update(?, ?, ?)(?, ?))
        .thenReturn(Success(campaign))

      // when
      campaignInactiveSinceUpdater.update(Seq(campaign))

      // then
      val patchCaptor: ArgumentCaptor[Patch] = ArgumentCaptor.forClass(classOf[Patch])
      verify(campaignServiceMock, times(1))
        .update(~~(campaign.customer.id), ~~(campaign.id), patchCaptor.capture())
      patchCaptor.getValue.inactiveChange match {
        case Some(Update(Some(_))) =>
        case unexpected =>
          fail(s"Unexpected inactive change: actual=$unexpected")
      }
    }

    "do nothing when campaign did not change activeness status" in {
      // given
      val campaigns = Seq(
        CampaignHeaderGen.next
          .copy(
            status = Some(CampaignStatus.Active(noSpends)),
            inactiveSince = None
          ),
        CampaignHeaderGen.next
          .copy(
            status = Some(CampaignStatus.Inactive(InactiveReasons.NoEnoughFunds)),
            inactiveSince = Some(DateTimeUtils.now())
          )
      )

      when(campaignServiceMock.update(?, ?, ?)(?, ?))
        .thenReturn(Failure(new RuntimeException))

      // when
      campaignInactiveSinceUpdater.update(campaigns)

      // then
      verifyNoInteractions(campaignServiceMock)
    }

  }

}
