package ru.yandex.vertis.moderation.util

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class HandleExceptionIteratorSpec extends SpecBase {

  var i = 3
  val iterator =
    new Iterator[Int] {
      override def hasNext: Boolean = 1000 / i > 0
      override def next(): Int = {
        i = i - 1
        i
      }
    }

  private var catchedException: Option[Throwable] = None
  private val handleExceptionIterator =
    HandleExceptionIterator(iterator) { t =>
      catchedException = Some(t)
    }

  "HandleExceptionIterator" should {

    "abcdefg" in {
      intercept[ArithmeticException] {
        handleExceptionIterator.toList
      }
      catchedException match {
        case Some(_: ArithmeticException) => info("Done")
        case other                        => fail(s"Unexpected $other")
      }
    }
  }
}
