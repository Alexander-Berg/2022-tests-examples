package ru.yandex.vertis.util.exenum

import org.scalatest.{Matchers, WordSpec}
import Enums._

/**
  * Specs for [[ExEnum]] and [[ExEnumCompanion]]
  *
  * @author semkagtn
  */
class ExEnumSpec
  extends WordSpec
  with Matchers {

  "values" should {

    "return all enum values" in {
      val actualResult = TestEnum.values
      val expectedResult = Set(TestEnum.Val1, TestEnum.Val2)
      actualResult should be (expectedResult)
    }

    "return empty set for enum without values" in {
      val actualResult = EmptyEnum.values
      val expectedResult = Set.empty[EmptyEnum]
      actualResult should be (expectedResult)
    }
  }

  "withNameOpt" should {

    "return Some(value) for default name" in {
      val actualResult = TestEnum.withNameOpt("Val1")
      val expectedResult = Some(TestEnum.Val1)
      actualResult should be (expectedResult)
    }

    "return Some(value) for overriden name" in {
      val actualResult = TestEnum.withNameOpt("two")
      val expectedResult = Some(TestEnum.Val2)
      actualResult should be (expectedResult)
    }

    "return None for nonexistent name" in {
      val actualResult = TestEnum.withNameOpt("Val2")
      val expectedResult = None
      actualResult should be (expectedResult)
    }
  }

  "withName" should {

    "return value for default name" in {
      val actualResult = TestEnum.withName("Val1")
      val expectedResult = TestEnum.Val1
      actualResult should be (expectedResult)
    }

    "return value for overriden name" in {
      val actualResult = TestEnum.withName("two")
      val expectedResult = TestEnum.Val2
      actualResult should be (expectedResult)
    }

    "throw exception for nonexistent name" in {
      an [NoSuchElementException] should be thrownBy TestEnum.withName("Val2")
    }
  }

  "withIdOpt" should {

    "return Some(value) for id" in {
      val actualResult = TestEnum.withIdOpt(1)
      val expectedResult = Some(TestEnum.Val1)
      actualResult should be (expectedResult)
    }

    "return None for nonexistent id" in {
      val actualResult = TestEnum.withIdOpt(3)
      val expectedResult = None
      actualResult should be (expectedResult)
    }
  }

  "withId" should {

    "return value for id" in {
      val actualResult = TestEnum.withId(1)
      val expectedResult = TestEnum.Val1
      actualResult should be (expectedResult)
    }

    "throw exception for nonexistent id" in {
      an [NoSuchElementException] should be thrownBy TestEnum.withId(3)
    }
  }
}
