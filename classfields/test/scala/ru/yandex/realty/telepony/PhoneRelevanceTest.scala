package ru.yandex.realty.telepony

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 07.09.17
  */
@RunWith(classOf[JUnitRunner])
class PhoneRelevanceTest extends FlatSpec with Matchers with PhoneRelevance {
  import PhoneRelevance._
  import TeleponyClient.Call

  "PhoneRelevance" should "correct build relevance in empty case" in {
    calculateRelevance(Nil) should be(0)
  }

  it should "correct up for 3 days" in {
    val c = Seq(
      Call(30, 20, "result", "proxy1", "target1", time = DateTime.now().minusMinutes(5).toString)
    )
    calculateRelevance(c) should be(TopBonus)

    val c2 = Seq(
      Call(30, 19, "result", "proxy1", "target1", time = DateTime.now().minusMinutes(5).toString)
    )
    calculateRelevance(c2) should be(0)

    val c3 = Seq(
      Call(30, 25, "result", "proxy1", "target1", time = DateTime.now().minusDays(4).toString)
    )
    calculateRelevance(c3) should be(0)

    val c4 = Seq(
      Call(30, 25, "result", "proxy1", "target1", time = DateTime.now().minusHours(2).toString),
      Call(30, 10, "result", "proxy1", "target1", time = DateTime.now().minusHours(1).toString)
    )
    calculateRelevance(c4) should be(TopBonus)

    val c5 = Seq(
      Call(30, 25, "result", "proxy1", "target1", time = DateTime.now().minusHours(2).toString),
      Call(30, 0, "result", "proxy1", "target1", time = DateTime.now().minusHours(1).toString),
      Call(30, 10, "result", "proxy1", "target1", time = DateTime.now().minusMinutes(10).toString)
    )
    calculateRelevance(c5) should be(0)

    val c6 = Seq(
      Call(30, 25, "result", "proxy1", "target1", time = DateTime.now().minusDays(2).toString),
      Call(30, 10, "result", "proxy1", "target1", time = DateTime.now().minusMinutes(10).toString)
    )
    calculateRelevance(c6) should be(TopBonus)

  }

  it should "correct up for 1 day" in {
    val c = Seq(
      Call(30, 20, "result", "proxy1", "target1", time = DateTime.now().minusMinutes(5).toString),
      Call(30, 21, "result", "proxy1", "target1", time = DateTime.now().minusHours(23).toString),
      Call(30, 22, "result", "proxy1", "target1", time = DateTime.now().minusHours(14).toString),
      Call(30, 0, "result", "proxy1", "target1", time = DateTime.now().minusHours(14).toString),
      Call(30, 0, "result", "proxy1", "target1", time = DateTime.now().minusMinutes(4).toString),
      Call(30, 0, "result", "proxy1", "target1", time = DateTime.now().minusMinutes(3).toString)
    )
    calculateRelevance(c) should be(TopBonus)

    val c2 = Seq(
      Call(30, 20, "result", "proxy1", "target1", time = DateTime.now().minusMinutes(5).toString),
      Call(30, 21, "result", "proxy1", "target1", time = DateTime.now().minusHours(23).toString),
      Call(30, 22, "result", "proxy1", "target1", time = DateTime.now().minusHours(14).toString),
      Call(30, 0, "result", "proxy1", "target1", time = DateTime.now().minusMinutes(2).toString),
      Call(30, 0, "result", "proxy1", "target1", time = DateTime.now().minusMinutes(4).toString),
      Call(30, 0, "result", "proxy1", "target1", time = DateTime.now().minusMinutes(3).toString),
      Call(30, 2, "result", "proxy1", "target1", time = DateTime.now().minusMinutes(1).toString)
    )
    calculateRelevance(c2) should be(0)
  }

  it should "correct down" in {
    val c1 = Seq(
      Call(30, 2, "result", "proxy1", "target1", time = DateTime.now().minusMinutes(10).toString),
      Call(30, 0, "result", "proxy1", "target1", time = DateTime.now().minusDays(2).toString),
      Call(30, 0, "result", "proxy1", "target1", time = DateTime.now().minusDays(4).toString),
      Call(30, 0, "result", "proxy1", "target1", time = DateTime.now().minusDays(3).toString)
    )
    calculateRelevance(c1) should be(0)

    val c2 = Seq(
      Call(30, 20, "result", "proxy1", "target1", time = DateTime.now().minusMinutes(5).toString),
      Call(30, 21, "result", "proxy1", "target1", time = DateTime.now().minusHours(23).toString),
      Call(30, 22, "result", "proxy1", "target1", time = DateTime.now().minusHours(14).toString),
      Call(30, 2, "result", "proxy1", "target1", time = DateTime.now().minusMinutes(10).toString),
      Call(30, 0, "result", "proxy1", "target1", time = DateTime.now().minusMinutes(2).toString),
      Call(30, 0, "result", "proxy1", "target1", time = DateTime.now().minusMinutes(4).toString),
      Call(30, 0, "result", "proxy1", "target1", time = DateTime.now().minusMinutes(3).toString)
    )
    calculateRelevance(c2) should be(DownBonus)

  }
}
