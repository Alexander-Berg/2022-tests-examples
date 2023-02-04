package ru.auto.salesman.dao

import org.scalactic.Equality
import ru.auto.salesman.dao.impl.jdbc.JdbcLogBufferEntryDao
import ru.auto.salesman.model.broker.MessageId
import ru.auto.salesman.model.log.buffer.{
  LogBufferEntry,
  LogBufferEntryId,
  LogBufferEntryPayload,
  LogBufferEntryType
}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.template.SalesmanJdbcSpecTemplate
import doobie.implicits._
import org.joda.time.DateTime
import ru.auto.salesman.dao.impl.jdbc.database.doobie.Transactor._
import zio.ZIO

class LogBufferEntryDaoSpec extends BaseSpec with SalesmanJdbcSpecTemplate {
  import LogBufferEntryDaoSpec._

  def dao = new JdbcLogBufferEntryDao(transactor)

  "create and read record" in {
    val testDao = dao
    val expected = genRecords(1)

    val res = (for {
      _ <- clean()
      _ <- createRecords(expected, testDao)
      res <- testDao.read(1)
    } yield res).success.value

    res should contain theSameElementsAs expected
  }

  "get first 2 records if there is more records in table" in {
    val testDao = dao
    val records = genRecords(3)
    val res = (for {
      _ <- clean()
      _ <- createRecords(records, testDao)
      res <- testDao.read(2)
    } yield res).success.value

    val expected = records.take(2)
    res should contain theSameElementsAs expected
  }

  "get all records in table if limit more than records in table" in {
    val testDao = dao
    val records = genRecords(3)
    val res = (for {
      _ <- clean()
      _ <- createRecords(records, testDao)
      res <- testDao.read(10)
    } yield res).success.value

    res should contain theSameElementsAs records
  }

  "delete specific records" in {
    val testDao = dao
    val records = genRecords(3)
    val toDelete = records.take(2).map(_.entryId)
    val res = (for {
      _ <- clean()
      _ <- createRecords(records, testDao)
      _ <- testDao.delete(toDelete)
      res <- testDao.read(1)
    } yield res).success.value

    val expected = records.takeRight(1)
    res should contain theSameElementsAs expected
  }

  "return empty on empty table" in {
    val testDao = dao
    val res = (for {
      _ <- clean()
      res <- testDao.read(1)
    } yield res).success.value

    res shouldBe empty
  }

  "do nothing on empty list for delete" in {
    val testDao = dao
    val expected = genRecords(1)

    val res = (for {
      _ <- clean()
      _ <- createRecords(expected, testDao)
      _ <- testDao.delete(List.empty)
      res <- testDao.read(1)
    } yield res).success.value

    res should contain theSameElementsAs expected
  }

  "dont fail on delete of not existed record" in {
    val testDao = dao
    (for {
      _ <- clean()
      res <- testDao.delete(List(LogBufferEntryId(1)))
    } yield res).success
  }

  private def clean() =
    sql"DELETE FROM tskv_log_bufer".update.run.transact(transactor)

}

object LogBufferEntryDaoSpec {
  private val testTime = DateTime.parse("2021-08-25")

  private def genRecord(id: Int) = LogBufferEntry(
    LogBufferEntryId(id),
    LogBufferEntryPayload(MessageId(s"id_$id"), s"data_$id".getBytes),
    LogBufferEntryType.SalesmanTasksLog,
    testTime
  )

  private def genRecords(count: Int): List[LogBufferEntry] =
    (1 to count).map(genRecord).toList

  private def createRecords(
      records: List[LogBufferEntry],
      dao: LogBufferEntryDao
  ) =
    ZIO.foreach_(records)(r => dao.create(r.entryPayload, r.entryType, r.created))

  // Это пришлось написать, чтобы обойти косяк в сравнении Array, внутри контейнеров и объектов
  // Подробности https://github.com/scalatest/scalatest/issues/491
  implicit val logBufferEntryEq: Equality[LogBufferEntry] =
    new Equality[LogBufferEntry] {

      override def areEqual(a: LogBufferEntry, b: Any): Boolean =
        b match {
          case entry: LogBufferEntry =>
            a.entryId == entry.entryId &&
              a.entryType == entry.entryType &&
              a.entryPayload.messageId == entry.entryPayload.messageId &&
              a.created == entry.created &&
              a.entryPayload.protoBytes
                .sameElements(entry.entryPayload.protoBytes)
          case _ => false
        }

    }

}
