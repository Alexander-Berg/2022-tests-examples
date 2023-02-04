package ru.yandex.vertis.moderation.util

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase

import scala.concurrent.duration._

/**
  * @author mpoplavkov
  */
@RunWith(classOf[JUnitRunner])
class DurationUtilSpec extends SpecBase {

  private val step = 30.days

  val cases =
    Seq(
      (45.days, 60.days),
      (60.days, 60.days),
      (60.days + 1.millis, 90.days),
      (10.days, 30.days)
    )

  "increaseDurationToBecomeADividerOf" should {
    cases.foreach { case (arg, result) =>
      s"transform $arg to $result using the step $step" in {
        DurationUtil.increaseDurationToBecomeADividerOf(arg, step) shouldBe result
      }
    }
  }

}
