package ru.yandex.vertis.billing.dao

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{Assertion, BeforeAndAfterEach, TryValues}
import ru.yandex.vertis.billing.dao.gens.{archiveRecordGen, ArchiveRecordGenParams}
import ru.yandex.vertis.billing.model_core.Epoch
import ru.yandex.vertis.billing.model_core.gens.{CampaignHeaderGen, CampaignIdGen, CustomerIdGen, Producer}
import ru.yandex.vertis.billing.service.ArchiveService.Filter.{
  CampaignUpdatedSinceBatchOrdered,
  ForCustomer,
  ForCustomerCampaign
}
import ru.yandex.vertis.billing.service.ArchiveService._
import ru.yandex.vertis.billing.util.clean.CleanableDao

import scala.collection.mutable
import scala.util.Success

/**
  * Spec on [[ArchiveDao]]
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
trait ArchiveDaoSpec extends AnyWordSpec with Matchers with TryValues with BeforeAndAfterEach {

  def archiveDao: ArchiveDao with CleanableDao

  override def beforeEach(): Unit = {
    super.beforeEach()
    archiveDao.clean().get
  }

  private def checkCampaignUpdatedSinceBatchOrdered(
      sortedSource: Seq[ArchiveRecord],
      epoch: Epoch,
      archiveRecordId: Option[ArchiveRecordId],
      batchSize: Int): Assertion = {
    val expected = sortedSource.take(batchSize)
    archiveDao.get(CampaignUpdatedSinceBatchOrdered(epoch, archiveRecordId, batchSize)) match {
      case Success(actualRecords) if actualRecords.isEmpty =>
        expected.isEmpty shouldBe true
      case Success(actualRecords) =>
        (actualRecords.toSeq should contain).theSameElementsInOrderAs(expected)
        val last = actualRecords.last
        val epoch = last.epoch.get
        val id = Some(last.id)
        val rest = sortedSource.drop(batchSize)
        checkCampaignUpdatedSinceBatchOrdered(rest, epoch, id, batchSize)
      case other =>
        fail(s"Unexpected $other")
    }
  }

  "ArchiveDao" should {
    "upsert" when {
      def testUpsert(rt: RecordType): Unit = {
        s"got $rt records" in {
          val count = 10
          val records = archiveRecordGen(ArchiveRecordGenParams().withRecordType(rt)).next(count)
          archiveDao.upsert(records) match {
            case Success(()) =>
            case other => fail(s"Unexpected $other")
          }
        }
      }

      RecordTypes.values.foreach(testUpsert)

      "got different records" in {
        val recordsCount = 50
        val records = archiveRecordGen().next(recordsCount)
        archiveDao.upsert(records) match {
          case Success(()) =>
          case other => fail(s"Unexpected $other")
        }
      }

      "got empty records" in {
        archiveDao.upsert(Iterable()) match {
          case Success(()) =>
          case other => fail(s"Unexpected $other")
        }
      }

      "got records with the same id" in {
        val campaign = CampaignHeaderGen.next
        val record1 = ArchiveRecord(
          id = "upsert_same",
          customerId = campaign.customer.id,
          comment = "comment 1",
          payload = CampaignPayload(campaign),
          epoch = None
        )
        val filter = ForCustomerCampaign(RecordTypes.Campaign, campaign.customer.id, campaign.id)

        archiveDao.upsert(Iterable(record1))
        val got1 = archiveDao.get(filter) match {
          case Success(records) if records.size == 1 =>
            records.head
          case other => fail(s"Unexpected $other")
        }

        Thread.sleep(100)
        archiveDao.upsert(Iterable(record1.copy(comment = "comment 2"))) match {
          case Success(()) =>
          case other => fail(s"Unexpected $other")
        }

        val got2 = archiveDao.get(filter) match {
          case Success(records) if records.size == 1 =>
            records.head
          case other => fail(s"Unexpected $other")
        }

        (got1.epoch, got2.epoch) match {
          case (Some(e1), Some(e2)) if e2 > e1 =>
          case other => fail(s"Unexpected $other")
        }
      }
    }

    "correctly get records" when {
      "got filter for customer" in {
        val count = 10
        val gen = for {
          customerId <- CustomerIdGen
          params = ArchiveRecordGenParams().withCustomerId(customerId).withoutEpoch.withoutCampaignEpoch
          record <- archiveRecordGen(params)
        } yield record
        val records = gen.next(count)
        archiveDao.upsert(records).get
        records.groupBy(_.recordType).foreach { case (rt, rs) =>
          val record = rs.head
          archiveDao.get(ForCustomer(rt, record.customerId)) match {
            case Success(actualResult) =>
              val actualWithoutEpoch = actualResult.map(_.copy(epoch = None)).toList
              val expectedResult = List(record)
              actualWithoutEpoch should contain theSameElementsAs expectedResult
            case other => fail(s"Unexpected $other for type $rt, expected: $record")
          }
        }
      }

      "got filter for customer and campaign" in {
        val count = 5
        val gen = for {
          campaignId <- CampaignIdGen
          params = ArchiveRecordGenParams().withCampaignId(campaignId).withoutEpoch.withoutCampaignEpoch
          record <- archiveRecordGen(params)
        } yield (campaignId, record)
        val campaignIdWithRecord = gen.next(count)
        val records = campaignIdWithRecord.map(_._2)

        archiveDao.upsert(records).get

        campaignIdWithRecord.foreach { case (campaignId, record) =>
          archiveDao.get(ForCustomerCampaign(record.recordType, record.customerId, campaignId)) match {
            case Success(actual) =>
              val actualWithoutEpoch = actual.map(_.copy(epoch = None)).toSeq
              val expected = Seq(record)
              (actualWithoutEpoch should contain).theSameElementsInOrderAs(expected)
            case other =>
              fail(s"Unexpected $other for type ${record.recordType}, expected: $record")
          }
        }
      }
      "got CampaignUpdatedSinceBatchOrdered" in {
        val campaignIdsCount = 10
        val recordsPerType = 50
        val campaignIds = CampaignIdGen.next(campaignIdsCount).toSeq
        val records = campaignIds.flatMap { campaignId =>
          val params = ArchiveRecordGenParams()
            .withRecordType(RecordTypes.Campaign)
            .withCampaignId(campaignId)
            .withoutEpoch
            .withoutCampaignEpoch
          val campaignRawRecords = archiveRecordGen(params).next
          val otherRecordTypes = RecordTypes.values.filter(_ != RecordTypes.Campaign)
          val otherRawRecords = otherRecordTypes.unsorted.flatMap { recordType =>
            archiveRecordGen(ArchiveRecordGenParams().withRecordType(recordType).withCampaignId(campaignId))
              .next(recordsPerType)
          }
          campaignRawRecords +: otherRawRecords.toSeq
        }

        archiveDao.upsert(records).get

        val campaignRecords = records.collect { case r @ ArchiveRecord(_, _, _, _, CampaignPayload(_)) =>
          r
        }
        val actualCampaignRecords = archiveDao.get(CampaignUpdatedSinceBatchOrdered(0L, None, records.size + 1)).get
        val actualWithoutEpoch = actualCampaignRecords.map(_.copy(epoch = None))
        actualWithoutEpoch should contain theSameElementsAs campaignRecords
        val startEpoch = 0L
        val batchSize = 5
        checkCampaignUpdatedSinceBatchOrdered(actualCampaignRecords.toSeq, startEpoch, None, batchSize)
      }
    }
  }
}
