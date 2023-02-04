package ru.yandex.vertis.billing.events.model

import org.joda.time.DateTime
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.event.Extractor
import ru.yandex.vertis.billing.events.model.gens.{tuple, EventMessageGen}
import ru.yandex.vertis.billing.model_core.Payload.EventDateTimeFormatterPattern
import ru.yandex.vertis.billing.model_core.gens.Producer
import ru.yandex.vertis.billing.util.ProtoDelimitedUtil

import scala.util.Success

/**
  * Tests for [[HydraModelEventParser]]
  *
  * @author zvez
  */
class HydraModelEventParserSpec extends AnyWordSpec with Matchers {

  private val eventParser = new HydraModelEventParser

  "HydraModelEventParser" should {

    "add project, locale, component fields to payload data" in {
      val bytes = ProtoDelimitedUtil.write(EventMessageGen.next(10))
      val Success(parsed) = eventParser.parseIterable(bytes)
      for (event <- parsed) {
        val Success(payload) = event.payload.toEventRecord
        payload.values.get(Extractor.LocaleCellName) shouldEqual Some(event.division.locale)
        payload.values.get(Extractor.ProjectCellName) shouldEqual Some(event.division.project)
        payload.values.get(Extractor.ComponentCellName) shouldEqual Some(event.division.component)
        payload.values.get(Extractor.TimestampCellName) shouldEqual
          Some(EventDateTimeFormatterPattern.print(event.payload.timestamp))
      }
    }

    "not override any field if it is already in data" in {
      val locale = "en"
      val project = "test"
      val component = "test-click"
      val timestamp = EventDateTimeFormatterPattern.print(DateTime.now())

      val events = EventMessageGen.next(10).map { e =>
        val builder = e.toBuilder
        builder.addTuple(tuple(Extractor.LocaleCellName, locale))
        builder.addTuple(tuple(Extractor.ProjectCellName, project))
        builder.addTuple(tuple(Extractor.ComponentCellName, component))
        builder.addTuple(tuple(Extractor.TimestampCellName, timestamp))
        builder.build()
      }

      val Success(parsed) = eventParser.parseIterable(ProtoDelimitedUtil.write(events))
      for (event <- parsed) {
        val Success(payload) = event.payload.toEventRecord
        payload.values.get(Extractor.LocaleCellName) shouldEqual Some(locale)
        payload.values.get(Extractor.ProjectCellName) shouldEqual Some(project)
        payload.values.get(Extractor.ComponentCellName) shouldEqual Some(component)
        payload.values.get(Extractor.TimestampCellName) shouldEqual Some(timestamp)
      }
    }

  }

}
