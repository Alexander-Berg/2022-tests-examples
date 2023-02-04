package ru.yandex.vos2.notifications

import org.joda.time.DateTime
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.ApiOfferModel.Category

import scala.concurrent.duration._
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.matchers.should.Matchers

@RunWith(classOf[JUnitRunner])
class CallParamsTest extends AnyFunSuite with Matchers {
  test("convert CallParams") {
    Category.CARS.toString shouldBe "CARS"
    Category.valueOf("CARS") shouldBe Category.CARS
    val date = new DateTime()
    val paramsAsLust = List("1", "60", date.getMillis.toString, "+7646464646464")
    val res = CallParams.unapply(paramsAsLust)
    res match {
      case Some(p: CallParams) =>
        p.callerId shouldBe "1"
        p.duration shouldBe 1.minute
        p.startTime shouldBe date
        p.outgoingPhoneNumber shouldBe "+7646464646464"
      case _ => fail("conversion was failed")
    }
  }
}
