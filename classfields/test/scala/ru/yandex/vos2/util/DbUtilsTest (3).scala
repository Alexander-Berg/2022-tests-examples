package ru.yandex.vos2.util

import org.scalatest.funsuite.AnyFunSuite

/**
  * Created by andrey on 2/3/17.
  */
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DbUtilsTest extends AnyFunSuite {
  test("nonempty inSection") {
    assert(DbUtils.inSection("test", Iterable(1, 2, 3)) == "test in (?, ?, ?)")
    intercept[RuntimeException](DbUtils.inSection("test", Iterable.empty))
    intercept[RuntimeException](DbUtils.inSection("test", Iterable.empty, allowEmpty = false))
    assert(DbUtils.inSection("test", Iterable.empty, allowEmpty = true) == "1 = 1")
  }

  test("notInSection") {
    assert(DbUtils.notInSection("test", Iterable(1, 2, 3)) == "test not in (?, ?, ?)")
    intercept[RuntimeException](DbUtils.notInSection("test", Iterable.empty))
    intercept[RuntimeException](DbUtils.notInSection("test", Iterable.empty, allowEmpty = false))
    assert(DbUtils.notInSection("test", Iterable.empty, allowEmpty = true) == "1 = 1")
  }
}
