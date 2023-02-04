package ru.yandex.realty.event

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures
import ru.yandex.realty.AsyncSpecBase

/**
  * Created by Sergey Kozlov <slider5@yandex-team.ru> on 03.05.2018
  */
@RunWith(classOf[JUnitRunner])
trait VertisEventSenderTest extends AsyncSpecBase {

  protected val eventSender: VertisEventSender

  "EventSender" should {
    "apply" in {
      ModelGen.CreateOfferEventGen.sample.map { event =>
        eventSender(event).futureValue
      }
    }
  }
}
