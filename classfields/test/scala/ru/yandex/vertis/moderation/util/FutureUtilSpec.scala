package ru.yandex.vertis.moderation.util

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class FutureUtilSpec extends SpecBase {

  case object SingletonException extends RuntimeException

  case class TestCase(description: String, inputSeq: Seq[Int], f: Int => Future[String])

  val testCases: Seq[TestCase] =
    Seq(
      TestCase(
        description = "empty seq",
        inputSeq = Seq.empty,
        f = x => Future.successful(x.toString)
      ),
      TestCase(
        description = "all success",
        inputSeq = Seq(1, 2),
        f = x => Future.successful(x.toString)
      ),
      TestCase(
        description = "one fail",
        inputSeq = Seq(1, 2),
        f = x => if (x == 1) Future.failed(SingletonException) else Future.successful(x.toString)
      )
    )

  "waitingTraverse" should {

    testCases.foreach { case TestCase(description, inputSeq, f) =>
      description in {
        val actualResult = FutureUtil.toTry(FutureUtil.waitingTraverse(inputSeq)(f))
        val expectedResult = FutureUtil.toTry(Future.traverse(inputSeq)(f))
        actualResult shouldBe expectedResult
      }
    }
  }
}
