package ru.yandex.vertis.util.collection

import org.scalatest.{FunSuite, OptionValues}
import org.scalatest.Matchers._

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 17.02.17
  */
class CollectionsTest extends FunSuite with OptionValues {

  test("toMultiMap") {
    Seq(1 → "a", 1 → "b", 2 → "c").toMultiMap shouldBe Map(
      1 → List("a", "b"),
      2 → List("c")
    )
  }

  test("distinctBy") {
    Seq("A", "B", "AB", "c").distinctBy(_.head) shouldBe Seq("A", "B", "c")
    Seq("A", "B", "c").distinctBy(_.head) shouldBe Seq("A", "B", "c")
  }

  test("optMin") {
    Seq.empty[Int].optMin shouldBe None
    Seq(1).optMin.value shouldBe 1
    Seq(1, 0, 5).optMin.value shouldBe 0
  }

  test("optMax") {
    Seq.empty[Int].optMax shouldBe None
    Seq(1).optMax.value shouldBe 1
    Seq(1, 0, 5).optMax.value shouldBe 5
  }

  test("Vector.getOrElse") {
    Vector(1, 5, 8).getOrElse(1, -1) shouldBe 5
    Vector(1, 5, 8).getOrElse(3, -1) shouldBe -1
  }

  test("intersperse") {
    Vector(1, 4).intersperse(Vector(2, 3)) shouldBe Vector(1, 2, 4, 3)
    Vector(1, 3, 5, 8).intersperse(Vector(4, 2)) shouldBe Vector(1, 4, 3, 2, 5, 8)
    Vector(1, 3).intersperse(Vector(4, 2, 6, 7)) shouldBe Vector(1, 4, 3, 2, 6, 7)
  }

  test("groupAndSortBy") {
    Vector(1, 8, 3, 5, 0, 2, 4, 6).groupAndSortBy(_ % 3) shouldBe Vector(
      0 -> Iterable(3, 0, 6),
      1 -> Iterable(1, 4),
      2 -> Iterable(8, 5, 2)
    )
  }

  test("sumBy") {
    Vector(1 -> 2, 3 -> 4).sumBy(_._1) shouldBe 4
    Vector(1 -> 2, 3 -> 4).sumBy(_._2) shouldBe 6
    Vector.empty[(Int, Int)].sumBy(_._1) shouldBe 0
  }

  test("groupByValueItem") {
    Map().groupByValueItem shouldBe Map()
    Map(1 -> Set(2, 3), 4 -> Set(2)).groupByValueItem shouldBe Map(2 -> Set(1, 4), 3 -> Set(1))
  }
}
