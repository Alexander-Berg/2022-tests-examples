package ru.yandex.vos2.util

import Dates._
import org.joda.time.LocalDate
import org.scalatest.funsuite.AnyFunSuite

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 24/09/2019
  */
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DatesTest extends AnyFunSuite {
  test("LocalDate") {
    val date = new LocalDate(1911, 7, 9)
    val ms = date.toMillis
    assert(ms.toLocalDate == date)
  }
}
