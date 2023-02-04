package ru.yandex.vertis.billing.reader

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.impl.jdbc.{JdbcEventDivisionDao, JdbcSpecTemplate}
import ru.yandex.vertis.billing.event.EventRecord
import ru.yandex.vertis.billing.model_core.SupportedDivisions
import ru.yandex.vertis.billing.model_core.gens.{PayloadGen, Producer}
import ru.yandex.vertis.billing.util.DateTimeInterval
import ru.yandex.vertis.billing.util.TestingHelpers.RichEventRecord

import scala.util.Success

/**
  * Spec on [[DivisionEventRecordsReader]]
  *
  * @author ruslansd
  */
class DivisionEventRecordsReaderSpec extends AnyWordSpec with Matchers with JdbcSpecTemplate {

  private val eventDao =
    new JdbcEventDivisionDao(eventStorageDualDatabase, SupportedDivisions.AutoRuIndexing.identity)

  private val reader = new DivisionEventRecordsReader(eventDao)

  private val Payloads = PayloadGen.next(1000).toList

  private val Interval = DateTimeInterval.currentDay

  private val EventRecords = Payloads.filter(p => Interval.contains(p.timestamp)).map(_.toEventRecord.get)

  "DivisionEventRecordsReader" should {

    "read events empty set events" in {
      val events = collection.mutable.ListBuffer[EventRecord]()
      reader.read(Interval)(accumulate(events)) match {
        case Success(_) =>
          events.size should be(0)
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "correctly read event records" in {
      eventDao.write(Payloads) match {
        case Success(_) =>
        case other => fail(s"Unexpected $other")
      }

      val events = collection.mutable.ListBuffer[EventRecord]()
      reader.read(Interval)(accumulate(events)) match {
        case Success(_) =>
          events.map(_.withoutEpoch) should contain theSameElementsAs EventRecords
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "double read changes nothing" in {
      val events = collection.mutable.ListBuffer[EventRecord]()
      reader.read(Interval)(accumulate(events)) match {
        case Success(_) =>
          events.map(_.withoutEpoch) should contain theSameElementsAs EventRecords
        case other =>
          fail(s"Unexpected $other")
      }
    }
  }

}
