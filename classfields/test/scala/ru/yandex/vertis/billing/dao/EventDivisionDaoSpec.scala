package ru.yandex.vertis.billing.dao

import akka.stream.scaladsl.Sink
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.async.{ActorSystemSpecBase, AsyncSpecBase}
import org.scalatest.LoneElement
import org.joda.time.DateTime
import ru.yandex.vertis.billing.dao.EventDivisionDao.{All, OnInterval, OnIntervalWithHBaseSort, WithDate}
import ru.yandex.vertis.billing.dao.EventDivisionDaoSpec._
import ru.yandex.vertis.billing.dao.impl.jdbc._
import ru.yandex.vertis.billing.model_core.{Payload, SupportedDivisions}
import ru.yandex.vertis.billing.model_core.gens.{EventGen, Producer}
import ru.yandex.vertis.billing.util.{DateTimeInterval, DateTimeUtils}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.SetParameter
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.util.Success

/**
  * Specs on [[EventDivisionDao]]
  *
  * @author alesavin
  */
class EventDivisionDaoSpec
  extends AnyWordSpec
  with Matchers
  with JdbcSpecTemplate
  with LoneElement
  with AsyncSpecBase
  with ActorSystemSpecBase {

  private val table = SupportedDivisions.AutoRuIndexing.identity

  private val eventDivisionDao = new JdbcEventDivisionDao(eventStorageDualDatabase, table)

  implicit private val byteArraySetParameter: SetParameter[Array[Byte]] =
    SetParameter[Array[Byte]] { (bytes, pp) =>
      pp.setBytes(bytes)
    }

  override protected def name: String = "EventDivisionDaoSpec"

  "EventDivisionDao" should {

    val expectedEvents = EventGen.next(100).toList
    val expectedPayloads = expectedEvents.map(_.payload).map(normalize)

    "return empty on start" in {
      eventDivisionDao.read(All) match {
        case Success(records) if records.isEmpty => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
    "write some events with correct division" in {
      eventDivisionDao.write(expectedEvents.map(_.payload)) match {
        case Success(count) if count == expectedEvents.size => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
    "return the same as writed (epoch should be defined for all records)" in {
      eventDivisionDao.read(All) match {
        case Success(records) =>
          records.forall(_.epoch.isDefined) shouldBe true
          records.map(normalize) should contain theSameElementsAs expectedPayloads
        case other => fail(s"Unexpected $other")
      }
    }
    "return payloads older specified date" in {
      val date = DateTimeUtils.now().toLocalDate
      eventDivisionDao.read(WithDate(date)) match {
        case Success(records) =>
          val expectedFiltered = expectedPayloads.filter(p => date == p.timestamp.toLocalDate)
          records.map(normalize) should contain theSameElementsAs expectedFiltered
        case other =>
          fail(s"Unexpected $other")
      }
    }
    "write events again twice and check no duplicates" in {
      eventDivisionDao.write(expectedEvents.map(_.payload)).get
      eventDivisionDao.read(All) match {
        case Success(records) =>
          val actual = records.map(normalize)
          actual should contain theSameElementsAs expectedPayloads
        case other => fail(s"Unexpected $other")
      }
    }
    "return payloads on currentDay" in {
      eventDivisionDao.read(OnInterval(DateTimeInterval.currentDay)) match {
        case Success(records) =>
          val expectedFiltered = expectedPayloads.filter(onInterval(DateTimeInterval.currentDay))
          records.map(normalize) should contain theSameElementsAs expectedFiltered
        case other => fail(s"Unexpected $other")
      }
    }
    "return payloads on currentDay with HBase sort" in {
      eventDivisionDao.read(OnIntervalWithHBaseSort(DateTimeInterval.currentDay)) match {
        case Success(records) =>
          val expectedFiltered = expectedPayloads.filter(onInterval(DateTimeInterval.currentDay))
          records.map(normalize) should contain theSameElementsAs expectedFiltered
        case other => fail(s"Unexpected $other")
      }
    }
    "read all events with stream function" in {
      val events = eventDivisionDao.stream(All).runWith(Sink.seq).futureValue
      events.map(normalize) should contain theSameElementsAs expectedPayloads
    }
    "read events on interval with stream function" in {
      val filter = OnIntervalWithHBaseSort(DateTimeInterval.currentDay)
      val events = eventDivisionDao.stream(filter).runWith(Sink.seq).futureValue
      val expectedFiltered = expectedPayloads.filter(onInterval(DateTimeInterval.currentDay))
      events.map(normalize) should contain theSameElementsAs expectedFiltered
    }
    "delete all on currentDay" in {
      eventDivisionDao.delete(OnInterval(DateTimeInterval.currentDay)).get
      eventDivisionDao.read(OnInterval(DateTimeInterval.currentDay)) match {
        case Success(records) if records.isEmpty => info("Done")
        case other => fail(s"Unexpected $other")
      }
    }
    "read payloads not on current day" in {
      eventDivisionDao.read(All) match {
        case Success(records) =>
          val expectedFiltered = expectedPayloads.filterNot(onInterval(DateTimeInterval.currentDay))
          records.map(normalize) should contain theSameElementsAs expectedFiltered
        case other => fail(s"Unexpected $other")
      }
    }
    "delete all older current day" in {
      val date = DateTimeUtils.now().toLocalDate
      eventDivisionDao.delete(WithDate(date)).get
      eventDivisionDao.read(All) match {
        case Success(records) =>
          val expectedFiltered = expectedPayloads.filter(_.timestamp.toLocalDate != date)
          records.map(normalize) should contain theSameElementsAs expectedFiltered
        case other => fail(s"Unexpected $other")
      }
    }
    "write data as string" in {
      eventStorageDatabase.runSync(sql"DELETE FROM #$table".asUpdate)
      for {
        _ <- eventDivisionDao.write(List(payload(Map("test_key" -> "test_value"))))
        dataString = eventStorageDatabase.runSync(sql"SELECT data_string FROM #$table".as[String].head)
      } yield dataString.parseJson shouldBe """{"test_key": "test_value"}""".parseJson
    }.get
    // иначе при убирании одновременно compress() и decompress() из кода тесты зелёные
    "read gzipped proto data (regress test)" in {
      val p = payload(Map("test_key" -> "test_value"))
      // gzipped proto-serialized Map("test_key" -> "test_value")
      val data =
        Array[Byte](31, -117, 8, 0, 0, 0, 0, 0, 0, 0, 19, -29, -30, 40, 73, 45, 46, -119, -49, 78, -83, 20, -30, 2, -77,
          -54, 18, 115, 74, 83, 1, 48, 87, 75, -44, 23, 0, 0, 0)
      eventStorageDatabase.runSync(sql"DELETE FROM #$table".asUpdate)
      // noinspection MakeArrayToString
      eventStorageDatabase.runSync(
        sql"""INSERT INTO #$table
              (`id`, `date`, `timestamp`, `campaign`, `uid`, `offer_id`, `page_place`, `data`)
             VALUES (${p.id}, ${p.timestamp.toLocalDate},
             ${p.timestamp}, ${p.campaign}, ${p.uid}, ${p.offerId},
             ${p.pagePlace},
             $data)""".asUpdate
      )
      eventDivisionDao.read(All).get.loneElement.copy(epoch = None) shouldBe p
    }
  }

  private def payload(data: Map[String, String]) = Payload(
    timestamp = new DateTime(100500),
    campaign = "test_campaign",
    uid = "test_uid",
    offerId = "test_offer_id",
    pagePlace = "test_page_place",
    data
  )
}

object EventDivisionDaoSpec {

  private val DefaultData = Map[String, String]()

  private def normalize(payload: Payload): Payload = {
    payload.copy(data = DefaultData, epoch = None)
  }

  private def onInterval(interval: DateTimeInterval)(payload: Payload): Boolean = {
    interval.contains(payload.timestamp)
  }

}
