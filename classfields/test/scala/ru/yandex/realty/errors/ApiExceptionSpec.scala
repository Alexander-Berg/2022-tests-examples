package ru.yandex.realty.errors

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase

import scala.util.Random

@RunWith(classOf[JUnitRunner])
class ApiExceptionSpec extends SpecBase {

  "ApiException" should {
    "Decoded.unapply" in {
      import ApiException.Decoded.unapply

      val errors = List(
        new NotFoundApiException("test message"),
        new InvalidParamsApiException("Flat [12345] : status ok"),
        new ForbiddenApiException("Произошла ошибка, извините!"),
        new ConflictApiException("тест - test")
      )

      errors.foreach { error =>
        unapply(error.toStatusRuntimeException).map(_.getMessage) shouldBe Some(error.getMessage)
      }

      unapply(new Exception("тест - test")) shouldBe None

      val rand = new Random(13)
      for (_ <- 0 to 1000) {
        val str = rand.nextString(rand.nextInt(1000))
        val e = new ConflictApiException(str)
        unapply(e.toStatusRuntimeException).map(_.getMessage) shouldBe Some(e.getMessage)
      }
    }
  }
}
