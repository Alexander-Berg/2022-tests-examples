package ru.yandex.vertis.util

import org.scalatest.{Matchers, WordSpec}

/**
  * Specs on [[CaseClassToString]]
  *
  * @author semkagtn
  */
class CaseClassToStringSpec
  extends WordSpec
    with Matchers {

  case class EmptyCaseClass() {
    override def toString: String = CaseClassToString[EmptyCaseClass]
  }

  case class SimpleCaseClass(field1: String, field2: Int) {
    override def toString: String = CaseClassToString[SimpleCaseClass]
  }

  case class WeirdCaseClass(`type`: String, `some-weird-name`: Int) {
    override def toString: String = CaseClassToString[WeirdCaseClass]
  }

  "toString" should {

    "correctly works for empty case class" in {
      val actualResult = EmptyCaseClass().toString
      val expectedResult = "EmptyCaseClass"
      actualResult should be (expectedResult)
    }

    "correctly works for simple case class" in {
      val actualResult = SimpleCaseClass(field1 = "x", field2 = 1).toString
      val expectedResult = "SimpleCaseClass(field1=x,field2=1)"
      actualResult should be (expectedResult)
    }

    "correctly works for weird case class" in {
      val actualResult = WeirdCaseClass(`type` = "t", `some-weird-name` = 2).toString
      val expectedResult = "WeirdCaseClass(type=t,some-weird-name=2)"
      actualResult should be (expectedResult)
    }
  }
}
