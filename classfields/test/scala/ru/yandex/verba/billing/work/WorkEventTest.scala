package ru.yandex.verba.billing.work

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.verba.billing.model.WorkEvent
import spray.json._
import ru.yandex.verba.core.util.VerbaJsonProtocol._

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 12.12.13 14:48
  */
class WorkEventTest extends AnyFreeSpec with Matchers {
  "WorkEvent" - {
    "should be serializable" in {
      val event = WorkEvent("test service", 1.3)

      println(event.toJson.prettyPrint)
      event.toJson.prettyPrint.parseJson.convertTo[WorkEvent] shouldEqual event
    }
  }
}
