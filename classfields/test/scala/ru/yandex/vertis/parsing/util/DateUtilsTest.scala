package ru.yandex.vertis.parsing.util

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
  * Тест для утилитного класса DateUtils
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class DateUtilsTest extends FunSuite {
  test("str2date") {
    val d = DateUtils.jodaParse("2018-02-10T01:10:46.034+03:00")
    assert(d.getYear == 2018)
    assert(d.getMonthOfYear == 2)
    assert(d.getDayOfMonth == 10)
  }
}
