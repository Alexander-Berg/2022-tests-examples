package ru.yandex.auto.util

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MetroUtilsSpec extends FlatSpec {

  "removeMetroSuffix" should "not modify data without (метро) and extra spaces" in {
    val t1 = "bmn"
    assert(t1 == MetroUtils.removeMetroSuffix(t1))
  }

  "removeMetroSuffix" should "trim spaces" in {
    val t2 = "bmn  "
    val r2 = "bmn"
    assert(r2 == MetroUtils.removeMetroSuffix(t2))
  }

  "removeMetroSuffix" should "modify data with (метро)" in {
    val t3 = "Павшино (метро)"
    val r3 = "Павшино"
    assert(r3 == MetroUtils.removeMetroSuffix(t3))
  }

  "removeMetroSuffix" should "be case-insensitive" in {
    val t4 = "Павшино (Метро)"
    val r4 = "Павшино"
    assert(r4 == MetroUtils.removeMetroSuffix(t4))
  }
}
