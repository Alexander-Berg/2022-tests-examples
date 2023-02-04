package ru.yandex.vertis.shark.util

import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.shark.model.Tag
import zio.test.environment.TestEnvironment
import ru.yandex.vertis.shark.util.RichModel.{RichKladrId, RichMoneyRub}
import ru.yandex.vertis.zio_baker.{model => zio_baker}
import zio.test.Assertion.equalTo
import zio.test.{assert, assertTrue, DefaultRunnableSpec, ZSpec}

object RichModelSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("RichModel")(
      test("rounds up to thousand correctly") {
        val money = 15432L.taggedWith[Tag.MoneyRub]
        val result = money.roundUpTo(3)
        assert(result)(equalTo(16000L))
      },
      test("rounds up to thousand correctly for 999") {
        val money = 15999L.taggedWith[Tag.MoneyRub]
        val result = money.roundUpTo(3)
        assert(result)(equalTo(16000L))
      },
      test("rounds up dont change if not needed") {
        val money = 15000L.taggedWith[Tag.MoneyRub]
        val result = money.roundUpTo(3)
        assert(result)(equalTo(15000L))
      },
      test("rounds up dont change if not needed for 0") {
        val money = 0L.taggedWith[Tag.MoneyRub]
        val result = money.roundUpTo(3)
        assert(result)(equalTo(0L))
      },
      suite("RichKladrId")(
        kladrIsInsideSuite
      )
    )
  }

  private def kladrIsInsideSuite = {
    def check(valueStr: String, thatStr: String, expected: Boolean) = {
      val value = valueStr.taggedWith[zio_baker.Tag.KladrId]
      val that = thatStr.taggedWith[zio_baker.Tag.KladrId]
      val description = s"$value ${if (expected) "is" else "is not"} inside $that"
      test(description)(
        assertTrue(value.isInside(that) == expected)
      )
    }

    suite("isInside")(
      check("1600600000100550001", "1600600000100550001", true),
      check("1600600000100550001", "16006000001005500", true),
      check("1600600000100550001", "1600600000100", true),
      check("1600600000100550001", "1600600000200", false),
      check("1600600000100550001", "1600600000000", true),
      check("1600600000100550001", "1600100000000", false),
      check("1600600000100550001", "1600000000000", true),
      check("1600600000100550001", "1100000000000", false),
      check("16002001000014600", "1600200100000", true),
      check("16002001000014600", "1600200200000", false),
      // Make sure we don't ignore significant zeroes
      check("1101101100000110011", "1101101100000110011", true),
      check("1101101100000110011", "1101101100000110010", false),
      check("1101101100000110011", "11011011000001100", true),
      check("1101101100000110011", "11011011000001000", false),
      check("1101101100000110011", "11011011000000000", true),
      check("1101101100000110011", "11011010000000000", false),
      check("1101101100000110011", "11011000000000000", true),
      check("1101101100000110011", "11010000000000000", false),
      check("1101101100000110011", "11000000000000000", true),
      check("1101101100000110011", "10000000000000000", false)
    )
  }
}
