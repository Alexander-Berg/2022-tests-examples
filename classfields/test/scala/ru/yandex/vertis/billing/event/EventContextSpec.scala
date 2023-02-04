package ru.yandex.vertis.billing.event

import org.joda.time.{DateTime, DateTimeZone, LocalDate, LocalTime}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.events.Dsl
import ru.yandex.vertis.billing.microcore_model.Properties
import ru.yandex.vertis.billing.model_core.event.EventContext
import ru.yandex.vertis.protobuf.kv.Converter

import scala.jdk.CollectionConverters._

/**
  * Specs on [[Dsl]]
  *
  * @author alesavin
  */
class EventContextSpec extends AnyWordSpec with Matchers {

  "Dsl.eventContext" should {
    "create valid context" in {
      val context = Dsl.eventContext(
        Dsl.timetable(
          Seq(
            Dsl.localDateInterval(
              LocalDate.parse("2016-09-19"),
              LocalDate.parse("2016-09-23"),
              Seq(
                Dsl.localTimeInterval(LocalTime.parse("09:00"), LocalTime.parse("13:00")),
                Dsl.localTimeInterval(LocalTime.parse("14:00"), LocalTime.parse("18:00"))
              ).asJavaCollection
            ),
            Dsl.localDateInterval(
              LocalDate.parse("2016-09-24"),
              LocalDate.parse("2016-09-24"),
              Seq(
                Dsl.localTimeInterval(LocalTime.parse("09:00"), LocalTime.parse("15:00"))
              ).asJavaCollection
            )
          ).asJavaCollection,
          DateTimeZone.forID("Europe/Moscow")
        )
      )

      val kv = Converter.toKeyValue(context, Some(Properties.BILLING_EVENT_CONTEXT)).get
      val record = EventRecord("test", List.empty, kv)

      Extractor.getEventContext(record) match {
        case EventContext(Some(wp)) =>
          assert(wp.contains(DateTime.parse("2016-09-19T09:00:00.001+03:00")))
          assert(wp.contains(DateTime.parse("2016-09-19T18:00:00.000+03:00")))
          assert(!wp.contains(DateTime.parse("2016-09-19T18:00:01.000+03:00")))
          assert(!wp.contains(DateTime.parse("2016-09-21T13:30:01.000+03:00")))
          assert(!wp.contains(DateTime.parse("2016-09-22T13:30:01.000+03:00")))
          assert(wp.contains(DateTime.parse("2016-09-24T13:30:01.000+03:00")))
          assert(wp.contains(DateTime.parse("2016-09-24T15:00:00.000+03:00")))
          assert(!wp.contains(DateTime.parse("2016-09-24T16:00:00.000+03:00")))
        case other => fail(s"Unexpected $other")
      }
    }
  }
}
