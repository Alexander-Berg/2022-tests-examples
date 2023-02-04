package ru.yandex.vertis.billing.service.impl

import org.mockito.ArgumentCaptor
import org.mockito.Mockito.verify
import org.scalatest.TryValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.LimitDao.ByCampaign
import ru.yandex.vertis.billing.dao.gens.archiveRecordGen
import ru.yandex.vertis.billing.dao.{ArchiveDao, BindingDao, CampaignDao, LimitDao}
import ru.yandex.vertis.billing.model_core.gens.{
  BindingPointGen,
  CampaignHeaderGen,
  CustomerIdGen,
  LimitSettingGen,
  Producer
}
import ru.yandex.vertis.billing.model_core.{Binding, BindingFilter, CustomerId, Uid}
import ru.yandex.vertis.billing.service.ArchiveService._
import ru.yandex.vertis.billing.service.impl.ArchiveServiceImplSpec.ArchiveSetup
import ru.yandex.vertis.billing.service.{ArchiveService, CampaignService}
import ru.yandex.vertis.billing.util.{DateTimeUtils, OperatorContext}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.collection.{mutable, Iterable}
import scala.util.{Failure, Success}

/**
  * Specs on [[ArchiveService]]
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
class ArchiveServiceImplSpec extends AnyWordSpec with Matchers with TryValues with MockitoSupport {

  import ArchiveServiceImplSpec.rc

  "ArchiveService" should {
    "archive campaign" in new ArchiveSetup {
      val recordsCaptor: ArgumentCaptor[Iterable[ArchiveRecord]] =
        ArgumentCaptor.forClass(classOf[Iterable[ArchiveRecord]])

      val campaign = {
        val c = CampaignHeaderGen.next.copy(status = None)
        c.copy(id = s"archive:${c.id}", settings = c.settings.copy(isEnabled = false))
      }
      val campaignId = campaign.id
      val customerId: CustomerId = campaign.customer.id
      val limits = LimitSettingGen.next(5).toList
      val bindings = BindingPointGen
        .next(5)
        .toList
        .map(Binding(_, DateTimeUtils.now().withMillisOfSecond(0)))
      when(campaignDao.get(CampaignService.Filter.ForCampaignCustomer(customerId, campaignId)))
        .thenReturn(Success(Iterable(campaign)))
      when(limitDao.get(campaignId)).thenReturn(Success(limits))
      when(bindingDao.get(BindingFilter.ForCampaign(campaignId))).thenReturn(Success(bindings))

      archiveService.archiveCampaign(customerId, campaignId, "comment") match {
        case Success(()) =>
        case other => fail(s"Unexpected $other")
      }

      verify(campaignDao).delete(customerId, campaignId)
      verify(limitDao).delete(ByCampaign(campaignId))
      verify(bindingDao).remove(BindingFilter.ForCampaign(campaignId))

      verify(archiveDao).upsert(recordsCaptor.capture())

      val records = recordsCaptor.getValue
      records.size shouldBe 11
      records.foreach { r =>
        r.customerId shouldBe customerId
        r.comment shouldBe "comment"
      }

      val archivedCampaigns = records.collect { case ArchiveRecord(_, _, _, _, CampaignPayload(c)) =>
        c
      }
      archivedCampaigns.toSet shouldBe Set(campaign)

      val archivedLimits = records.collect { case ArchiveRecord(_, _, _, _, LimitPayload(`campaignId`, l)) =>
        l
      }
      archivedLimits should contain theSameElementsAs limits

      val archivedBinding = records.collect { case ArchiveRecord(_, _, _, _, BindingPayload(b)) =>
        b
      }
      archivedBinding should contain theSameElementsAs bindings
    }

    "not archive enabled campaign" in new ArchiveSetup {
      val campaign = {
        val c = CampaignHeaderGen.next
        c.copy(status = None, settings = c.settings.copy(isEnabled = true))
      }

      when(campaignDao.get(?)(?)).thenReturn(Success(Iterable(campaign)))
      when(limitDao.get(?)).thenReturn(Success(Seq.empty))
      when(bindingDao.get(?)).thenReturn(Success(Iterable.empty))

      archiveService.archiveCampaign(campaign.customer.id, campaign.id, "c") match {
        case Failure(_: IllegalArgumentException) =>
        case other => fail(s"Unexpected $other")
      }
    }

    "get records" in new ArchiveSetup {
      val recordsCount = 10
      val records = archiveRecordGen().next(recordsCount)
      val filter = Filter.ForCustomer(RecordTypes.Campaign, CustomerIdGen.next)

      when(archiveDao.get(filter)).thenReturn(Success(records))

      archiveService.get(filter) match {
        case Success(rs) =>
          rs should contain theSameElementsAs records
        case other => fail(s"Unexpected $other")
      }
    }
  }
}

object ArchiveServiceImplSpec {

  implicit private val rc: OperatorContext = OperatorContext("test", Uid(1))

  trait ArchiveSetup extends MockitoSupport {

    protected val archiveDao = {
      val m = mock[ArchiveDao]
      when(m.upsert(?)).thenReturn(Success(()))
      m
    }

    protected val campaignDao = {
      val m = mock[CampaignDao]
      when(m.delete(?, ?)).thenReturn(Success(()))
      m
    }

    protected val limitDao = {
      val m = mock[LimitDao]
      when(m.delete(?)).thenReturn(Success(()))
      m
    }

    protected val bindingDao = {
      val m = mock[BindingDao]
      when(m.remove(?)).thenReturn(Success(()))
      m
    }

    protected val archiveService: ArchiveService =
      new ArchiveServiceImpl(campaignDao, Some(limitDao), Some(bindingDao), archiveDao)

  }

}
