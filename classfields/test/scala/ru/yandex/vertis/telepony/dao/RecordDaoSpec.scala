package ru.yandex.vertis.telepony.dao

import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.model.{OperatorAccounts, Operators, RecordMeta}
import ru.yandex.vertis.telepony.util.Page
import ru.yandex.vertis.telepony.util.Range.Full
import ru.yandex.vertis.telepony.{DatabaseSpec, SpecBase}

import scala.concurrent.duration._

/**
  * @author evans
  */
trait RecordDaoSpec extends SpecBase with BeforeAndAfterEach with DatabaseSpec {

  def dao: RecordDao

  override protected def beforeEach(): Unit = {
    dao.clear().databaseValue.futureValue
    super.beforeEach()
  }

  private val sampleUrl = "http://vk.com"

  private val sampleRecord =
    RecordMeta("1", OperatorAccounts.MttShared, sampleUrl, None, None, DateTime.now, customS3Prefix = Some("prefix/"))

  "RecordDao" should {
    "create" in {
      dao.createIfNotExists(sampleRecord).databaseValue.futureValue
    }
    "multiple create" in {
      dao.createIfNotExists(sampleRecord).databaseValue.futureValue
      dao.createIfNotExists(sampleRecord).databaseValue.futureValue
      dao.listAll(Full).databaseValue.futureValue.size should ===(1)
    }
    "get v1" in {
      val record = sampleRecord
      dao.createIfNotExists(record).databaseValue.futureValue
      dao.get(record.id).databaseValue.futureValue should ===(Some(record))
    }
    "get v2" in {
      val record =
        RecordMeta(
          "1",
          OperatorAccounts.MttShared,
          sampleUrl,
          None,
          Some("124"),
          DateTime.now,
          customS3Prefix = Some("prefix/")
        )
      dao.createIfNotExists(record).databaseValue.futureValue
      dao.get(record.id).databaseValue.futureValue should ===(Some(record))
    }
    "update" in {
      val record = sampleRecord
      dao.createIfNotExists(record).databaseValue.futureValue
      val record2 = record.copy(groupId = Some("124"))
      dao.update(record2).databaseValue.futureValue
      dao.get(record.id).databaseValue.futureValue should ===(Some(record2))
    }
    "list by empty" in {
      val record = sampleRecord
      val record2 =
        RecordMeta(
          "2",
          OperatorAccounts.MttShared,
          "http://vk2com",
          None,
          None,
          DateTime.now,
          customS3Prefix = Some("prefix/")
        )
      dao.createIfNotExists(record).databaseValue.futureValue
      dao.createIfNotExists(record2).databaseValue.futureValue
      dao.listAll(Full).databaseValue.futureValue.size should ===(2)
    }

    "list for load only not after specified time" in {
      val r1 = sampleRecord.copy(id = "1", time = DateTime.now().minusHours(4))
      val r2 = sampleRecord.copy(id = "2", time = DateTime.now().minusHours(2))

      dao.createIfNotExists(r1).databaseValue.futureValue
      dao.createIfNotExists(r2).databaseValue.futureValue

      val records =
        dao.listForLoad(sampleRecord.operator, DateTime.now.minusHours(3), 24.hours, 1.hour).databaseValue.futureValue
      records should ===(List(r2))
    }

    "list for load only not downloaded record" in {
      val r1 = sampleRecord.copy(id = "1", status = true)
      val r2 = sampleRecord.copy(id = "2", status = false)

      dao.createIfNotExists(r1).databaseValue.futureValue
      dao.createIfNotExists(r2).databaseValue.futureValue

      val records =
        dao.listForLoad(sampleRecord.operator, DateTime.now.minusDays(1), 24.hours, 1.hour).databaseValue.futureValue
      records should ===(List(r2))
    }

    "list for load only of specified operator" in {
      val r1 = sampleRecord.copy(id = "1", account = OperatorAccounts.BeelineShared)
      val r2 = sampleRecord.copy(id = "2", account = OperatorAccounts.MttShared)

      dao.createIfNotExists(r1).databaseValue.futureValue
      dao.createIfNotExists(r2).databaseValue.futureValue

      val records =
        dao.listForLoad(Operators.Mtt, DateTime.now.minusDays(1), 24.hours, 1.hour).databaseValue.futureValue
      records should ===(List(r2))
    }

    "list for load with filter by last upload time" in {
      val r1 = sampleRecord.copy(
        id = "1",
        time = DateTime.now().minusHours(1),
        lastUploadTime = Some(DateTime.now())
      )
      val r2 = sampleRecord.copy(
        id = "2",
        time = DateTime.now().minusHours(3),
        lastUploadTime = None
      )
      val r3 = sampleRecord.copy(
        id = "3",
        time = DateTime.now().minusHours(3),
        lastUploadTime = Some(DateTime.now().minusHours(8))
      )
      val r4 = sampleRecord.copy(
        id = "4",
        time = DateTime.now().minusHours(3),
        lastUploadTime = Some(DateTime.now().minusHours(4))
      )

      dao.createIfNotExists(r1).databaseValue.futureValue
      dao.createIfNotExists(r2).databaseValue.futureValue
      dao.createIfNotExists(r3).databaseValue.futureValue
      dao.createIfNotExists(r4).databaseValue.futureValue

      val records =
        dao.listForLoad(Operators.Mtt, DateTime.now.minusDays(1), 6.hours, 2.hours).databaseValue.futureValue
      records should contain theSameElementsAs List(r1, r2, r3)
    }

    "delete" in {
      val record = sampleRecord
      dao.createIfNotExists(record).databaseValue.futureValue
      dao.delete(record.id).databaseValue.futureValue
      dao.get(record.id).databaseValue.futureValue should not be defined
    }
    "provide valid slice" in {
      dao
        .createIfNotExists(
          RecordMeta("1", OperatorAccounts.MttShared, "", None, None, DateTime.now, customS3Prefix = None)
        )
        .databaseValue
        .futureValue
      dao
        .createIfNotExists(
          RecordMeta("2", OperatorAccounts.MttShared, "", None, None, DateTime.now, customS3Prefix = None)
        )
        .databaseValue
        .futureValue
      val page = Page(0, 1)
      val slice = dao.listAll(page).databaseValue.futureValue
      slice.total should ===(2)
      slice.values.size should ===(1)
      slice.slice should ===(page)
    }
    "Insert empty custom prefix" in {
      val first = sampleRecord.copy(id = "customFirst", customS3Prefix = None)
      dao.createIfNotExists(first).databaseValue.futureValue
      dao.get(first.id).databaseValue.futureValue should ===(Some(first))

      val second = sampleRecord.copy(id = "customSecond", customS3Prefix = Some(""))
      dao.createIfNotExists(second).databaseValue.futureValue
      dao.get(second.id).databaseValue.futureValue should ===(Some(second))
    }
  }
}
