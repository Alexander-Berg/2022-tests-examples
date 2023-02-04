package ru.yandex.vertis.billing.service.checking

import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.CampaignDao.DuplicationPolicy
import ru.yandex.vertis.billing.dao.{BindingDao, CampaignCallDao, CampaignDao}
import ru.yandex.vertis.billing.model_core.gens._
import ru.yandex.vertis.billing.model_core.{AttachRule, CampaignSettings, PartnerRef, Uid, Update}
import ru.yandex.vertis.billing.service.CampaignService
import ru.yandex.vertis.billing.service.impl.CampaignServiceImpl
import ru.yandex.vertis.billing.util.OperatorContext
import ru.yandex.vertis.mockito.MockitoSupport

import scala.util.Success

/**
  * Specs on [[CheckedCampaignService]]
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
class CheckedCampaignServiceSpec extends AnyWordSpec with Matchers with OneInstancePerTest with MockitoSupport {

  implicit val operatorContext = OperatorContext("test", Uid(0L))

  val owner = CustomerIdGen.next

  val firstCampaignId = "campaign_1"
  val firstAttachRule = AttachRule.Resources(Set(PartnerRef("capa_partner_1")))

  val firstCampaign = CampaignHeaderGen.next.copy(
    id = firstCampaignId,
    settings = CampaignSettings.Default.copy(
      attachRule = Some(firstAttachRule)
    )
  )

  val secondCampaignId = "campaign_2"
  val secondAttachRule = AttachRule.Resources(Set(PartnerRef("capa_partner_2")))

  val secondCampaign = CampaignHeaderGen.next.copy(
    id = secondCampaignId,
    settings = CampaignSettings.Default.copy(
      attachRule = Some(secondAttachRule)
    )
  )

  val otherAttachRule = AttachRule.Resources(Set(PartnerRef("capa_partner_other")))

  val campaigns = Iterable(firstCampaign, secondCampaign)

  val campaignDao = {
    val m = mock[CampaignDao]

    when(m.get(?)(?))
      .thenReturn(Success(campaigns))

    when(m.create(?, ?, ?)(?))
      .thenReturn(Success(CampaignHeaderGen.next))
    when(m.update(?, ?, ?, ?)(?))
      .thenReturn(Success(CampaignHeaderGen.next))

    m
  }

  val campaignService =
    new CampaignServiceImpl(campaignDao, mock[BindingDao], mock[CampaignCallDao], DuplicationPolicy.AllowDuplicates)
      with CheckedCampaignService {
      override def checker: UserInputChecker = Transparent
    }

  "CampaignService.create" should {
    "not check attach rules if empty in source" in {
      val source = CampaignService.Source(
        None,
        OrderIdGen.next,
        ProductGen.next,
        CampaignSettings.Default.copy(attachRule = None),
        None,
        Iterable()
      )
      campaignService.create(owner, source) match {
        case Success(_) =>
        case other => fail(s"Unpredicted $other")
      }
//      verify(campaignDao).withCustomerLock(any())(any())
    }

    "create campaign if attach rule check passed" in {
      val source = CampaignService.Source(
        None,
        OrderIdGen.next,
        ProductGen.next,
        CampaignSettings.Default.copy(attachRule = Some(otherAttachRule)),
        None,
        Iterable()
      )
      campaignService.create(owner, source) match {
        case Success(_) =>
        case other => fail(s"Unpredicted $other")
      }
//      verify(campaignDao).withCustomerLock(any())(any())
    }
  }

  "CampaignService.update" should {
    val mainPatch = CampaignService.Patch(product = Some(ProductGen.next))

    "not check attach rules if empty in patch" in {
      val patch =
        mainPatch.copy(attachRule = None)
      campaignService.update(owner, firstCampaignId, patch) match {
        case Success(_) =>
        case other => fail(s"Unpredicted $other")
      }
//      verify(campaignDao).withCustomerLock(any())(any())
    }

    "not check attach rules if delete in patch" in {
      val patch =
        mainPatch.copy(attachRule = Some(Update(None)))
      campaignService.update(owner, firstCampaignId, patch) match {
        case Success(_) =>
        case other => fail(s"Unpredicted $other")
      }
//      verify(campaignDao).withCustomerLock(any())(any())
    }

    "update campaign if attach rules check passes" in {
      val campaignId = firstCampaignId
      val patch =
        mainPatch.copy(attachRule = Some(Update(Some(otherAttachRule))))
      campaignService.update(owner, campaignId, patch) match {
        case Success(_) =>
        case other => fail(s"Unpredicted $other")
      }
//      verify(campaignDao).withCustomerLock(any())(any())
    }

    "update campaign if patch has the same attach rules as its source" in {
      val campaignId = firstCampaignId
      val patch = mainPatch.copy(attachRule = Some(Update(Some(firstAttachRule))))
      campaignService.update(owner, campaignId, patch) match {
        case Success(_) =>
        case other => fail(s"Unpredicted $other")
      }
//      verify(campaignDao).withCustomerLock(any())(any())
    }
  }

}
