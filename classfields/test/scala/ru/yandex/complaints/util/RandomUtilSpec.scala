package ru.yandex.complaints.util

import org.scalatest.{Matchers, WordSpec}

/**
 * Runnable spec for [[RandomUtil]]
 *
 * @author frenki
 * created on 22.03.2018.
 */
class RandomUtilSpec extends WordSpec with Matchers {

    "RandomUtil" should {
        val n = 10

        "return not negative long" in {
            RandomUtil.nextLongAbs should be >= 0L
        }

        "return String with length n" in {
            RandomUtil.nextSymbols(n) should have length 10
        }

        "return number from 0 to 9" in {
            RandomUtil.nextDigit should (be > 0 and be < 10)
        }

        "return String with length n contains only digits" in {
            val nextDigit = RandomUtil.nextDigits(n)
            nextDigit should have length 10
            nextDigit should fullyMatch regex "(\\d+)"
        }
    }
}
