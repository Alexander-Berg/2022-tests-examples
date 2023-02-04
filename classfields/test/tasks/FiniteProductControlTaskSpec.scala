package ru.yandex.vertis.billing.tasks

import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.FiniteProductDao
import ru.yandex.vertis.billing.dao.FiniteProductDao.FiniteProductRecord
import ru.yandex.vertis.billing.dao.gens.ExpiredFiniteProductRecordGen
import ru.yandex.vertis.billing.model_core.CampaignId
import ru.yandex.vertis.billing.model_core.gens.{CampaignHeaderGen, Producer}
import ru.yandex.vertis.billing.service.{ArchiveService, CampaignService}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.util.Success

/**
  * Spec on [[FiniteProductControlTask]]
  *
  * @author ruslansd
  */
class FiniteProductControlTaskSpec extends AnyWordSpec with Matchers with BeforeAndAfterEach with MockitoSupport {

  override protected def beforeEach(): Unit = {
    Mockito.clearInvocations[Any](campaignMock, archive)
    super.beforeEach()
  }

  private val campaignMock = {
    val m = mock[CampaignService]

    when(m.get(?[CampaignId])(?))
      .thenReturn(Success(CampaignHeaderGen.next))

    when(m.update(?, ?, ?)(?, ?))
      .thenReturn(Success(CampaignHeaderGen.next))
    m
  }

  private val archive = {
    val m = mock[ArchiveService]

    when(m.archiveCampaign(?, ?, ?)(?))
      .thenReturn(Success(()))
    m
  }

  private def finiteProductDao(records: Iterable[FiniteProductRecord]) = {
    val m = mock[FiniteProductDao]

    when(m.get(?))
      .thenReturn(Success(records))

    when(m.delete(?))
      .thenReturn(Success(()))
    m
  }

  private def task(records: Iterable[FiniteProductRecord]) =
    new FiniteProductControlTask(
      finiteProductDao(records),
      archive,
      campaignMock
    )

  "FiniteProductControlTask" should {

    "correctly work on empty set" in {
      task(Iterable.empty).execute() shouldBe Success(())
    }

    "correctly work on non empty set" in {
      val count = 100
      val records = ExpiredFiniteProductRecordGen.next(count)

      val archiveCount = records.count(_.action.`type` == FiniteProductDao.Actions.Archive)
      val setProductCount = records.count(_.action.`type` == FiniteProductDao.Actions.SetProduct)

      task(records).execute()

      Mockito.verify(campaignMock, Mockito.times(count)).get(?[CampaignId])(?)
      Mockito.verify(campaignMock, Mockito.times(setProductCount)).update(?, ?, ?)(?, ?)
      Mockito.verify(archive, Mockito.times(archiveCount)).archiveCampaign(?, ?, ?)(?)
    }
  }

}
