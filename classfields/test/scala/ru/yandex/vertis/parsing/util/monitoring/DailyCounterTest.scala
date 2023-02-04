package ru.yandex.vertis.parsing.util.monitoring

import java.io.File

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.parsing.util.{DateUtils, FileUtils}
import ru.yandex.vertis.parsing.util.DateUtils.RichDateTime

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class DailyCounterTest extends FunSuite {
  test("inc") {
    new File("/tmp/test_current_value.txt").delete()
    val d = new DailyCounter("test", "test")("l1", "l2")
    d.inc("a1", "a2")(5)
    d.inc("b1", "b2")(6)
    assert(
      d.gaugeData == Seq(
        GaugeData(Seq("a1", "a2"), 5),
        GaugeData(Seq("b1", "b2"), 6)
      )
    )
    d.inc("b1", "b2")(6)
    assert(
      d.gaugeData == Seq(
        GaugeData(Seq("a1", "a2"), 5),
        GaugeData(Seq("b1", "b2"), 12)
      )
    )
  }

  test("reset") {
    val date = DateUtils.currentDayStart
    FileUtils.save("/tmp/test_current_value.txt") { write =>
      write(date.minusDays(1).formatDateAs("yyyy-MM-dd"))
      write("a1,a2 45")
    }
    val d = new DailyCounter("test", "test")("l1", "l2")
    assert(
      d.gaugeData == Seq(
        GaugeData(Seq("a1", "a2"), 0)
      )
    )
    d.inc("a1", "a2")()
    assert(
      d.gaugeData == Seq(
        GaugeData(Seq("a1", "a2"), 1)
      )
    )
  }

  test("load from file") {
    val date = DateUtils.currentDayStart
    FileUtils.save("/tmp/test_current_value.txt") { write =>
      write(date.formatDateAs("yyyy-MM-dd"))
      write("a1,a2 45")
    }
    val d = new DailyCounter("test", "test")("l1", "l2")
    assert(
      d.gaugeData == Seq(
        GaugeData(Seq("a1", "a2"), 45)
      )
    )
  }
}
