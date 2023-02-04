package ru.yandex.vertis.billing.banker.service.tskv

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
  * @author ruslansd
  */
class TskvLoggingSpec extends AnyWordSpec with Matchers {

  import TskvLogging.escape

  "TskvLogging" should {
    "escape result" in {
      escape("Hello\tworld!\n\r") shouldBe "Hello world! "
      escape("Hello\t\t\t\tworld!\r") shouldBe "Hello world! "

      escape("Hello world!") shouldBe "Hello world!"
      escape("Hello    world!") shouldBe "Hello    world!"
    }

    "log causes" in {
      val cause3 = new Exception("cause3")
      val cause2 = new RuntimeException("cause2", cause3)
      val cause1 = new IllegalArgumentException("cause1", cause2)
      val root = new IllegalStateException("root", cause1)

      val serialized = TskvLogging.fail(root)
      info(serialized)

      def toString(e: Throwable): String =
        s"[${e.getClass.getSimpleName}: ${e.getMessage}]"
      val sep = " caused by "
      val expected = toString(root) + sep + toString(cause1) + sep + toString(cause2)
      serialized shouldBe s"Failure($expected)"
    }

    "handle nulls when log causes" in {
      val ex = new NumberFormatException

      val serialized = TskvLogging.fail(ex)
      serialized shouldBe "Failure([NumberFormatException: null])"
    }
  }
}
