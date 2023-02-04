package ru.yandex.vos2.util

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers._
import org.scalatestplus.junit.JUnitRunner

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 26.09.16
  */
@RunWith(classOf[JUnitRunner])
class DumpUtilsTest extends AnyFunSuite {

  import DumpUtils.RichAny
  private case class TestClass(a: String, b: Int, c: Seq[Any], m: Map[String, Int], s: Option[Int])

  test("dump string") {
    "abc".valueTreeString shouldBe "abc"
  }

  test("dump number") {
    1.valueTreeString shouldBe "1"
  }

  test("dump date") {
    val date = DateTime.now
    date.valueTreeString shouldBe date.toString
  }

  test("dump collection") {
    val seq = Seq(1, 2, 3)
    seq.valueTreeString shouldBe
      """- 1
        |- 2
        |- 3""".stripMargin
    Seq.empty.valueTreeString shouldBe "<empty>"
  }

  test("dump map") {
    val map = Map(1 -> "a", 2 -> "b")
    map.valueTreeString shouldBe
      """- 1: a
        |- 2: b""".stripMargin
    Map.empty.valueTreeString shouldBe "<empty>"
  }

  test("dump option") {
    Some("ab").valueTreeString shouldBe "ab"
    None.valueTreeString shouldBe "<none>"
  }

  test("dump case class") {
    val obj = TestClass("aaa", 2, Seq(3, "551f"), Map("4d" -> 4), None)
    obj.valueTreeString shouldBe
      """- a: aaa
        |- b: 2
        |- c:
        || - 3
        || - 551f
        |- m: - 4d: 4
        |- s: <none>""".stripMargin
  }
}
