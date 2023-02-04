package ru.yandex.vertis.parsing.util

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
  * Created by andrey on 2/14/18.
  */
@RunWith(classOf[JUnitRunner])
class PhoneUtilsTest extends FunSuite {
  test("normalizePhone") {
    assert(PhoneUtils.normalizePhone("\n\t\t\t\t\t+7 (383) 233-60-60\t\t\t") == "73832336060")
  }
}
