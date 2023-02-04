package ru.yandex.auto.recalls.core.dao

import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.recalls.RecallsApiModel.{CardEventType, Source}
import ru.yandex.auto.recalls.core.testkit.RecallsPgDatabaseContainer
import ru.yandex.auto.vin.decoder.model.VinCode
import auto.carfax.common.storages.pg.PostgresProfile.api._
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture

import scala.concurrent.ExecutionContext.Implicits.global

class RecallsEventDaoTest extends AnyFunSuite with RecallsPgDatabaseContainer with BeforeAndAfter {

  val dao = new RecallsEventQueueDao

  before {
    val query = RecallsEventQueueTable.events.delete
    database.runWrite(query)
  }

  test("CRUD") {
    val vin = VinCode("X7LBSRBYNBH480080")

    val insert = for {
      _ <- dao.insert(123, vin, "user:1", Source.AUTORU, CardEventType.CREATED)
      _ <- dao.insert(1234, vin, "yandex:1", Source.NAVIGATOR, CardEventType.CREATED)
      _ <- dao.insert(1235, vin, "user:2", Source.AUTORU, CardEventType.CREATED)
      _ <- dao.insert(123, vin, "yandex:1", Source.AUTORU, CardEventType.DELETED)
    } yield ()

    database.runAll(insert).await

    assert(database.runAll(dao.tableSize).await == 4)

    val batch = database.runAll(dao.poll(buckets = Set(0), bucketsSize = 2, batchSize = 2)).await
    assert(batch.size == 1)

    database.runAll(dao.delete(batch.map(_.id).toSet)).await
    assert(database.runAll(dao.tableSize).await == 3)
  }
}
