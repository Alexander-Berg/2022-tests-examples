package ru.yandex.vertis.moderation.util

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.util.CollectionUtil.RichIterable

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class CollectionUtilSpec extends SpecBase {

  "frequentElements" should {

    case class Params(k: Int, src: Seq[String], expectedResult: Set[String])

    val params: Seq[Params] =
      Seq(
        Params(
          k = 1,
          src = Seq.empty,
          expectedResult = Set.empty
        ),
        Params(
          k = 0,
          src =
            Seq(
              "a",
              "a",
              "b"
            ),
          expectedResult = Set("a", "b")
        ),
        Params(
          k = 1,
          src =
            Seq(
              "a",
              "a",
              "b"
            ),
          expectedResult = Set("a", "b")
        ),
        Params(
          k = 2,
          src =
            Seq(
              "a",
              "b",
              "c"
            ),
          expectedResult = Set.empty
        ),
        Params(
          k = 2,
          src =
            Seq(
              "a",
              "a",
              "c"
            ),
          expectedResult = Set("a")
        )
      )
    params.foreach { case Params(k, sets, expectedResult) =>
      s"return correct result for k=$k, sets=$sets" in {
        val actualResult = sets.frequentElements(k)
        actualResult shouldBe expectedResult
      }
    }
  }

  "merge maps" should {
    case class Params(first: Map[String, Int], second: Map[String, Int], expectedMerged: Map[String, Int])

    val params =
      Seq(
        Params(
          first = Map.empty,
          second = Map.empty,
          expectedMerged = Map.empty
        ),
        Params(
          first = Map("a" -> 1, "b" -> 2, "x" -> -1),
          second = Map("a" -> 10, "c" -> 20, "x" -> 1),
          expectedMerged = Map("a" -> 11, "b" -> 2, "c" -> 20, "x" -> 0)
        )
      )

    params.foreach { case Params(first, second, expectedResult) =>
      s"return correct result for first=$first, second = $second" in {
        val actualResult = CollectionUtil.merge(first, second)(_ + _)
        actualResult shouldBe expectedResult
      }
    }
  }
}
