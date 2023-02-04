package ru.yandex.vertis.baker.util

import org.scalatest.funsuite.AnyFunSuite

class DurationUtilTest extends AnyFunSuite {
  test("seconds from") {
    assert(DurationUtil.secondsFrom(0, 1000000) == 0.001)
  }
}
