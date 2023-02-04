package ru.yandex.vertis.scalatest.matcher

import java.util.Objects

import org.scalatest.{Matchers, WordSpec}

/**
  * Specs on [[Differ]]
  *
  * @author semkagtn
  */
class DifferSpec
  extends WordSpec
    with Matchers {

  import DifferSpec._

  "differ" should {

    val params: Seq[(Any, Any, Set[Diff])] = Seq(
      // primitive values
      (0, 0, Set.empty),
      (0, 1, Set(Diff.Changed("/", 0, 1))),
      ("a", "a", Set.empty),
      ("a", "b", Set(Diff.Changed("/", "a", "b"))),
      ("a", null, Set(Diff.Changed("/", "a", null))),
      (null, "b", Set(Diff.Changed("/", null, "b"))),
      ((), (), Set.empty),
      (Set.empty, Set.empty, Set.empty),
      (Set(1), Set(1), Set.empty),
      (Set(1), Set(2), Set(Diff.Changed("/", Set(1), Set(2)))),

      // array
      (Array.empty, Array.empty, Set.empty),
      (Array(0), Array(0), Set.empty),
      (Array(1), Array.empty, Set(Diff.Added("/0", 1))),
      (Array.empty, Array(1), Set(Diff.Removed("/0", 1))),
      (Array(2), Array(3), Set(Diff.Changed("/0", 2, 3))),
      (Array(1, 2, 3), Array(1, 2), Set(Diff.Added("/2", 3))),
      (Array(1, 2), Array(1, 2, 3), Set(Diff.Removed("/2", 3))),
      (Array(1, 2), Array(1, 3), Set(Diff.Changed("/1", 2, 3))),

      // seq
      (Seq.empty, Seq.empty, Set.empty),
      (Seq(0), Seq(0), Set.empty),
      (Seq(1), Seq.empty, Set(Diff.Added("/0", 1))),
      (Seq.empty, Seq(1), Set(Diff.Removed("/0", 1))),
      (Seq(2), Seq(3), Set(Diff.Changed("/0", 2, 3))),
      (Seq(1, 2, 3), Seq(1, 2), Set(Diff.Added("/2", 3))),
      (Seq(1, 2), Seq(1, 2, 3), Set(Diff.Removed("/2", 3))),
      (Seq(1, 2), Seq(1, 3), Set(Diff.Changed("/1", 2, 3))),
      (Seq(0), null, Set(Diff.Changed("/", Seq(0), null))),
      (null, Seq(0), Set(Diff.Changed("/", null, Seq(0)))),
      (Seq.empty, null, Set(Diff.Changed("/", Seq.empty, null))),

      // map
      (Map.empty, Map.empty, Set.empty),
      (Map("x" -> 1), Map("x" -> 1), Set.empty),
      (Map("x" -> 1), Map.empty, Set(Diff.Added("/x", 1))),
      (Map.empty, Map("y" -> 2), Set(Diff.Removed("/y", 2))),
      (Map("x" -> 1), Map("x" -> 2), Set(Diff.Changed("/x", 1, 2))),
      (Map("x" -> 1), Map("y" -> 2), Set(Diff.Added("/x", 1), Diff.Removed("/y", 2))),
      (Map("x" -> 1), null, Set(Diff.Changed("/", Map("x" -> 1), null))),
      (null, Map("x" -> 1), Set(Diff.Changed("/", null, Map("x" -> 1)))),
      (Map("x" -> 1), Map("x" -> null), Set(Diff.Changed("/x", 1, null))),
      (Map.empty, null, Set(Diff.Changed("/", Map.empty, null))),

      // case-class
      (Point(1, 1), Point(1, 1), Set.empty),
      (Point(1, 1), Point(1, 2), Set(Diff.Changed("/y", 1, 2))),
      (Point(1, 1), Point(2, 3), Set(Diff.Changed("/x", 1, 2), Diff.Changed("/y", 1, 3))),
      (Point(1, 1), null, Set(Diff.Changed("/", Point(1, 1), null))),
      (null, Point(1, 1), Set(Diff.Changed("/", null, Point(1, 1)))),

      // in-depth
      (Array(Array(1)), Array(Array(2)), Set(Diff.Changed("/0/0", 1, 2))),
      (Map("x" -> Seq(1)), Map("y" -> Seq(2)), Set(Diff.Added("/x/0", 1), Diff.Removed("/y/0", 2))),
      (circle(1, 2, 3), circle(1, 0, 0), Set(Diff.Changed("/c/y", 2, 0), Diff.Changed("/r", 3, 0))),

      // case objects
      (Enum1, Enum1, Set.empty),
      (Enum1, Enum2, Set(Diff.Changed("/", Enum1, Enum2))),
      (Enum1, null, Set(Diff.Changed("/", Enum1, null))),
      (null, Enum2, Set(Diff.Changed("/", null, Enum2))),

      // sealed trait
      (Just(1), Just(1), Set.empty),
      (Just(1), Just(2), Set(Diff.Changed("/value", 1, 2))),
      (Just(1), Nothing, Set(Diff.Added("/value", 1))),
      (Nothing, Just(1), Set(Diff.Removed("/value", 1)))
    )
    params.foreach { case (actual, expected, expectedDiff) =>
      s"works with actual=${actual.asStr} and expected=${expected.asStr}" in {
        val differ = new Differ[Any](Seq.empty)
        val actualDiff = differ.diffs(actual, expected).toSet
        actualDiff shouldBe expectedDiff
      }
    }

    "correctly ignore one field" in {
      val actual = circle(0, 0, 0)
      val expected = circle(1, 2, 3)
      val differ = new Differ[Circle](Iterable("/c/y".r))

      val actualDiff = differ.diffs(actual, expected).toSet
      val expectedDiff = Set(Diff.Changed("/c/x", 0, 1), Diff.Changed("/r", 0, 3))
      actualDiff shouldBe expectedDiff
    }

    "correctly ignore two fields" in {
      val actual = circle(0, 0, 0)
      val expected = circle(1, 2, 3)
      val differ = new Differ[Circle](Iterable("/c/y".r, "/r".r))

      val actualDiff = differ.diffs(actual, expected).toSet
      val expectedDiff = Set(Diff.Changed("/c/x", 0, 1))
      actualDiff shouldBe expectedDiff
    }

    "correctly ignore several fields by one regex" in {
      val actual = circle(0, 0, 0)
      val expected = circle(1, 2, 3)
      val differ = new Differ[Circle](Iterable("/c/.+".r))

      val actualDiff = differ.diffs(actual, expected).toSet
      val expectedDiff = Set(Diff.Changed("/r", 0, 3))
      actualDiff shouldBe expectedDiff
    }
  }
}

object DifferSpec {

  case class Point(x: Int, y: Int)
  case class Circle(c: Point, r: Int)
  def circle(x: Int, y: Int, r: Int): Circle = Circle(Point(x, y), r)

  sealed trait Enum
  case object Enum1 extends Enum
  case object Enum2 extends Enum

  sealed trait Maybe
  case class Just(value: Int) extends Maybe
  case object Nothing extends Maybe

  implicit class RichAny(val any: Any) extends AnyVal {

    def asStr: String = any match {
      case array: Array[_] => array.mkString("Array(", ",", ")")
      case string: String => "\"" + string + "\""
      case _ => Objects.toString(any)
    }
  }
}
