package ru.yandex.vertis.telepony.json

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.telepony.json.ReactiveJsonProtocol._
import ru.yandex.vertis.telepony.model.{Action, Event, EventModelGenerators}
import spray.json._

/**
  *
  * @author zvez
  */
class ReactiveJsonProtocolSpec extends AnyWordSpec with ScalaCheckPropertyChecks with Matchers {

  "VoxEventJsonProtocol" should {
    "format and parse events" in {
      forAll(EventModelGenerators.VoxEventsGen) { event =>
        val json = event.toJson
        val str = json.compactPrint
        val parsedJson = str.parseJson
        val parsedEvent = parsedJson.convertTo[Event]
        parsedEvent shouldBe event
      }
    }

    "format and parse actions" in {
      forAll(EventModelGenerators.ActionGen) { action =>
        val json = action.toJson
        val str = json.compactPrint
        val parsedJson = str.parseJson
        val parsedAction = parsedJson.convertTo[Action]
        parsedAction shouldBe action
      }
    }
  }

}
