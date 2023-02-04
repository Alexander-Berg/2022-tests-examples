package ru.yandex.complaints.dao

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}

/**
  * Spec for [[ValidateMetaDao]]
  *
  * @author potseluev
  */
@RunWith(classOf[JUnitRunner])
class ValidateMetaDaoSpec
  extends WordSpec
    with Matchers {

  "ValidateMetaDao.validateAutoruAuthor" should {

    case class TestCase(authorId: AuthorId,
                        isCorrect: Boolean)

    val testCases = Seq(
      TestCase("auto_ru_0", isCorrect = false),
      TestCase("a_-273", isCorrect = false),
      TestCase("dealer_0", isCorrect = false),
      TestCase("dealer_undefined", isCorrect = false),
      TestCase("auto_ru_01", isCorrect = false),
      TestCase("auto_ru_10", isCorrect = true),
      TestCase("dealer_5", isCorrect = true)
    )

    testCases.foreach { case TestCase(authorId, isCorrect) =>
      s"${if (isCorrect) "accept" else "reject"} $authorId".in {
        ValidateMetaDao.validateAutoruAuthor(authorId).isSuccess shouldBe isCorrect
      }
    }
  }

}
