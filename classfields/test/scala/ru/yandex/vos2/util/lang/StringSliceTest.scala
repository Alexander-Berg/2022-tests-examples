package ru.yandex.vos2.util.lang

import org.scalatest.FunSuite

/**
  * Created by andrey on 5/23/17.
  */
class StringSliceTest extends FunSuite {
  private val slit = StringSlice.splitLast2('_')

  test("splitLast2") {
    check("create_date_asc", "create_date", "asc")
    check("create_date_asc_test", "create_date_asc", "test")
    check("create_date", "create", "date")
    checkNone("create")
  }

  private def check(str: String, need1: String, need2: String): Unit = {
    str match {
      case slit(p1, p2) =>
        assert(p1 == need1)
        assert(p2 == need2)
      case _ =>
        fail()
    }
  }

  private def checkNone(str: String): Unit = {
    str match {
      case slit(p1, p2) =>
        fail()
      case _ =>
        assert(true)
    }
  }
}
