package ru.yandex.realty.rent.chat.service.analytics.offerchat.startdateresponses

import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.db.mysql.api._
import ru.yandex.realty.ops.DaoMetrics
import ru.yandex.realty.rent.chat.{BaseDbSpec, CleanSchemaBeforeAll}

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class StartDateResponseDaoSpec extends FlatSpec with Matchers with BaseDbSpec with CleanSchemaBeforeAll {

  import StartDateResponseDaoSpec._

  behavior of classOf[MysqlStartDateResponseDao].getName

  private val dao = new MysqlStartDateResponseDao(masterSlaveDatabase, DaoMetrics.stub())

  private def findRecord(roomId: String): Option[StartDateResponseRecord] = {
    masterSlaveDatabase.master.db
      .run(StartDateResponseTable.Table.filter(_.roomId === roomId).result.headOption)
      .futureValue
  }

  it should "create records" in {
    TestRecords.foreach { r =>
      assume(findRecord(r.roomId).isEmpty)
    }

    whenReady(Future.traverse(TestRecords)(dao.set)) { _ =>
      TestRecords.foreach { expected =>
        val actual = findRecord(expected.roomId).get
        actual.configVersion shouldBe expected.configVersion
        actual.presetIndex shouldBe expected.presetIndex
        actual.responseText shouldBe (if (expected.roomId == "10000") "A".repeat(1024) else expected.responseText)
      }
    }
  }

  it should "aggregate records" in {
    whenReady(dao.aggregate()) { records =>
      records.toSet shouldBe Set(
        StartDateAggregateRecord(0, Some(0), "0", 2),
        StartDateAggregateRecord(0, Some(1), "1", 1),
        StartDateAggregateRecord(1, Some(0), "0", 1),
        StartDateAggregateRecord(0, None, "0", 1),
        StartDateAggregateRecord(0, Some(0), "A".repeat(1024), 1)
      )
    }
  }

  it should "clear record" in {
    assume(findRecord("1").isDefined)
    whenReady(dao.clear("1")) { _ =>
      findRecord("1") shouldBe None
    }
  }

}

object StartDateResponseDaoSpec {
  private val TestRecords = Seq(
    StartDateResponseRecord("1", 0, Some(0), "0"),
    StartDateResponseRecord("2", 0, Some(0), "0"),
    StartDateResponseRecord("3", 0, Some(1), "1"),
    StartDateResponseRecord("4", 1, Some(0), "0"),
    StartDateResponseRecord("5", 0, None, "0"),
    StartDateResponseRecord("10000", 0, Some(0), "A".repeat(10000))
  )
}
