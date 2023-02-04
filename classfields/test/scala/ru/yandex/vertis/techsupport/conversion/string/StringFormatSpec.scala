package ru.yandex.vertis.vsquality.techsupport.conversion.string

import org.scalacheck.Arbitrary
import ru.yandex.vertis.vsquality.techsupport.conversion.string.StringFormatSpec.{
  NegativeTestCase,
  PositiveTestCase,
  TestCase
}
import ru.yandex.vertis.vsquality.techsupport.model.UserId
import org.scalatestplus.scalacheck.Checkers.check
import org.scalacheck.Prop
import org.scalatest.Assertion
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase

import scala.reflect.ClassTag

/**
  * @author potseluev
  */
class StringFormatSpec extends SpecBase {

  import StringFormatInstances._
  import ru.yandex.vertis.vsquality.techsupport.Arbitraries._

  private val testCases: Seq[TestCase[_]] =
    Seq(
      PositiveTestCase[UserId.Client.Autoru]()
    ) ++ Seq("21342", "user:-123", "").map(NegativeTestCase[UserId.Client.Autoru])

  "StringFormat" should {
    testCases.foreach { test =>
      test.description in {
        test match {
          case positive: PositiveTestCase[_] => checkPositive(positive)
          case negative: NegativeTestCase[_] => checkNegative(negative)
        }
      }
    }
  }

  private def checkPositive[T](test: PositiveTestCase[T]): Assertion = {
    import test._
    check(Prop.forAll { x: T =>
      format.deserialize(format.serialize(x)) == Right(x)
    })
  }

  private def checkNegative[T](test: NegativeTestCase[T]): Assertion = {
    test.format.deserialize(test.incorrectString).isLeft shouldBe true
  }
}

object StringFormatSpec {

  sealed trait TestCase[T] {
    def description: String

    def format: StringFormat[T]
  }

  case class PositiveTestCase[T]()(implicit val format: StringFormat[T], val arb: Arbitrary[T], classTag: ClassTag[T])
    extends TestCase[T] {

    def description: String =
      s"convert $classTag"
  }

  case class NegativeTestCase[T](incorrectString: String)(implicit val format: StringFormat[T], classTag: ClassTag[T])
    extends TestCase[T] {

    override def description: String =
      s"fail convert $incorrectString to $classTag"
  }

}
