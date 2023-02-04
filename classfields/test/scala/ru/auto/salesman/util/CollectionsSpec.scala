package ru.auto.salesman.util

import org.scalacheck.Gen
import scala.util.Try

import ru.auto.salesman.test.BaseSpec

class CollectionsSpec extends BaseSpec {
  import Collections.RichList
  "Collections" should {

    "get min in minByOption" in {
      val li: List[Int] = List(2, 5, 8, 4)
      li.minByOption(identity) shouldBe Some(2)
    }

    "get max in maxByOption" in {
      val li: List[Int] = List(2, 5, 1, 8)
      li.maxByOption(identity) shouldBe Some(8)
    }

    "get None in minByOption" in {
      val li: List[Int] = List()
      li.minByOption(identity) shouldBe None
    }

    "get None in maxByOption" in {
      val li: List[Int] = List()
      li.maxByOption(identity) shouldBe None
    }

    val mockedFunction = mockFunction[List[Int], Try[Unit]]

    "call function on non empty list" in {
      forAll(Gen.nonEmptyListOf(Gen.posNum[Int])) { nonEmptyList =>
        mockedFunction.expects(*).returningT(())
        nonEmptyList.onNonEmpty(mockedFunction).success.value shouldBe (())
      }
    }

    "don't call function on empty list" in {
      val emptyList: List[Int] = List()
      mockedFunction.expects(*).never
      emptyList.onNonEmpty(mockedFunction).success.value shouldBe (())
    }
  }
}
