package ru.yandex.vertis.telepony.util

import java.util

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.logging.SimpleLogging

/**
  * @author evans
  */
class ArrayOpsSpec extends SpecBase with ScalaCheckPropertyChecks with SimpleLogging {

  implicit val ValueGen: Gen[Int] = Gen.chooseNum(0, 50)

  implicit val SortedArrayGen: Gen[Array[Int]] = for {
    length <- Gen.chooseNum(0, 10)
    arr <- Gen.listOfN(length, ValueGen)
  } yield arr.toArray.sorted

  def comparator(el: Int)(candidate: Int): Int =
    if (candidate < el) -1 else if (candidate == el) 0 else 1

  implicit val generatorDrivenConfig2: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 100000)

  "ArrayOps" should {
    "binary search for all int arrays" in {
      forAll(SortedArrayGen, ValueGen) { (arr: Array[Int], el: Int) =>
        val expected = arr.find(_ == el)
        val actual = ArrayOps.binarySearchBy(arr)(comparator(el))
        actual shouldEqual expected
      }
    }
    "binary search for index for all int arrays" in {
      forAll(SortedArrayGen, ValueGen) { (arr: Array[Int], el: Int) =>
        val expected = util.Arrays.binarySearch(arr, el)
        val actual = ArrayOps.binarySearchIndexBy(arr)(comparator(el))
        if (expected >= 0) {
          arr(actual) shouldEqual arr(expected)
        } else {
          actual shouldEqual expected
        }
      }
    }
    "relaxed binary search for index for all int arrays" in {
      forAll(SortedArrayGen, ValueGen) { (arr: Array[Int], el: Int) =>
        whenever(arr.nonEmpty) {
          ArrayOps.relaxedBinarySearchBy(arr)(comparator(el))
        }
      }
    }
  }
}
