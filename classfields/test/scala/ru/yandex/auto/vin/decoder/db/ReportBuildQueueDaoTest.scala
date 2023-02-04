package ru.yandex.auto.vin.decoder.db

import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.db.queue.ReportBuildQueueDao
import ru.yandex.auto.vin.decoder.db.queue.ReportBuildQueueDao.{ProcessedVinInfo, ReportBuildQueueSize}
import ru.yandex.vertis.ops.test.TestOperationalSupport
import auto.carfax.pro_auto.core.src.testkit.PgDatabaseContainer

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class ReportBuildQueueDaoTest extends AnyFunSuite with PgDatabaseContainer {

  private val dao = new ReportBuildQueueDao(database, TestOperationalSupport)

  private val buckets = 0 to 10

  test("mark as should process") {
    val vin = "123"

    Await.result(dao.markShouldProcess(vin, Some(1)), 10.seconds)
    val result = Await.result(dao.getByVin(vin), 10.seconds).get

    assert(result.vin == vin)
    assert(result.shouldProcess)
    assert(result.priority.contains(1))
  }

  test("mark batch should process") {
    val vin1 = "789"
    val vin2 = "78980"

    Await.result(dao.markBatchShouldProcess(Seq(vin1, vin2), Some(1)), 10.seconds)
    val result = Await.result(dao.getShouldProcess(10, buckets, 10), 10.seconds)

    assert(result.map(_.vin).contains(vin1))
    assert(result.map(_.vin).contains(vin2))
  }

  test("dont mark as processed when rescheduling happened") {
    val vin = "1234"

    Await.result(dao.markShouldProcess(vin, Some(1)), 10.seconds)
    Await.result(dao.getByVin(vin), 10.seconds).get
    Await.result(dao.markAsProcessed(Seq(ProcessedVinInfo(vin, System.currentTimeMillis()))), 10.seconds)
    Await.result(dao.getByVin(vin), 10.seconds).get
    val size = Await.result(dao.getQueueSize, 10.second)
    val shouldProcess = Await.result(dao.getShouldProcess(10, buckets, 10), 10.second)

    assert(size == ReportBuildQueueSize(4, 4))
    assert(shouldProcess.map(_.vin).contains(vin))
  }

  test("mark as processed") {
    val vin = "12345"
    val vin2 = "123456"
    Await.result(dao.markShouldProcess(vin, Some(1)), 10.seconds)
    Await.result(dao.markShouldProcess(vin2, Some(1)), 10.seconds)
    val row = Await.result(dao.getByVin(vin), 10.seconds).get
    val row2 = Await.result(dao.getByVin(vin2), 10.seconds).get
    Await.result(
      dao.markAsProcessed(Seq(ProcessedVinInfo(vin, row.timestamp), ProcessedVinInfo(vin2, row2.timestamp))),
      10.seconds
    )
    val shouldProcess = Await.result(dao.getShouldProcess(10, buckets, 10), 10.second)

    assert(!shouldProcess.map(_.vin).contains(vin))
    assert(!shouldProcess.map(_.vin).contains(vin2))
  }

}
