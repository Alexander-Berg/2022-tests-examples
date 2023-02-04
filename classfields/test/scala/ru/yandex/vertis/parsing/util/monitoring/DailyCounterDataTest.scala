package ru.yandex.vertis.parsing.util.monitoring

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.parsing.util.DateUtils
import ru.yandex.vertis.parsing.util.DateUtils.RichDateTime

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class DailyCounterDataTest extends FunSuite {
  test("serialize") {
    val date = DateUtils.currentDayStart
    val dateStr = date.formatDateAs("yyyy-MM-dd")
    val d = initDailyCounterData(date)
    assert(
      d.serialize ==
        s"""$dateStr
         |a1,a2 1
         |b1,b2 2
         |c1,c2 3
         |""".stripMargin
    )
  }

  test("gaugeData") {
    val d = initDailyCounterData()
    val values = d.gaugeData
    assert(
      values == Seq(
        GaugeData(Seq("a1", "a2"), 1),
        GaugeData(Seq("b1", "b2"), 2),
        GaugeData(Seq("c1", "c2"), 3)
      )
    )
  }

  test("reset") {
    val d: DailyCounterData = initDailyCounterData(DateUtils.currentDayStart.minusDays(1)).reset
    assert(d.date == DateUtils.currentDayStart)
    val values = d.gaugeData
    assert(
      values == Seq(
        GaugeData(Seq("a1", "a2"), 0),
        GaugeData(Seq("b1", "b2"), 0),
        GaugeData(Seq("c1", "c2"), 0)
      )
    )
  }

  test("inc") {
    val d = DailyCounterData.default(Seq("l1"))
    assert(d.gaugeData == Seq.empty)
    val d2 = d.inc("a")(5)
    assert(d2.gaugeData == Seq(GaugeData(Seq("a"), 5)))
    val d3 = d2.inc("b")(6)
    assert(
      d3.gaugeData == Seq(
        GaugeData(Seq("a"), 5),
        GaugeData(Seq("b"), 6)
      )
    )
    val d4 = d3.inc("a")(5)
    assert(
      d4.gaugeData == Seq(
        GaugeData(Seq("a"), 10),
        GaugeData(Seq("b"), 6)
      )
    )
  }

  test("load") {
    val lines =
      """2018-04-06
        |a,b,c 5
        |
        |d,e,f 6
        |
        |
      """.stripMargin.split("\n")
    val d = DailyCounterData.load(Seq("l1", "l2", "l3"), lines)
    assert(
      d == DailyCounterData(
        new DateTime(2018, 4, 6, 0, 0, 0),
        Seq("l1", "l2", "l3"),
        Map(
          Seq("a", "b", "c") -> 5,
          Seq("d", "e", "f") -> 6
        )
      )
    )
  }

  test("same length") {
    val lines =
      """2018-04-06
        |a,b,c 5
        |
        |d,e 6
        |
        |
      """.stripMargin.split("\n")
    val labelNames = Seq("l1", "l2")
    assert(DailyCounterData.load(labelNames, lines) == DailyCounterData.default(labelNames))
  }

  test("no spaces") {
    val lines =
      """2018-04-06
        |a,b,c 5
        |
        |d,rt e 6
        |
        |
      """.stripMargin.split("\n")
    val labelNames = Seq("l1", "l2")
    assert(DailyCounterData.load(labelNames, lines) == DailyCounterData.default(labelNames))
    intercept[RuntimeException] {
      DailyCounterData(DateUtils.currentDayStart, labelNames, Map(Seq("aaa eee") -> 1))
    }
  }

  test("load from wrong str") {
    val lines = Seq("2018-04-10", "pewpew")
    val d = DailyCounterData.load(Seq("l1", "l2"), lines)
    assert(d == DailyCounterData.default(Seq("l1", "l2")))
  }

  test("load from empty str") {
    val d = DailyCounterData.load(Seq("l1", "l2"), "     ")
    assert(d == DailyCounterData.default(Seq("l1", "l2")))
  }

  private def initDailyCounterData(date: DateTime = DateUtils.currentDayStart) = {
    val d = DailyCounterData
      .default(Seq("l1", "l2"))
      .copy(date = date)
      .inc("a1", "a2")(1)
      .inc("b1", "b2")(2)
      .inc("c1", "c2")(3)
    d
  }
}
