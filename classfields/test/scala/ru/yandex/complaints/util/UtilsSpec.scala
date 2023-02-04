package ru.yandex.complaints.util

import java.io.IOException

import org.scalatest.{Matchers, WordSpec}

import scala.util.Failure

/**
  *  Runnable spec for [[Utils]]
  *
  * @author frenki
  * created on 22.03.2018.
  */
class UtilsSpec extends WordSpec with Matchers {
    "Utils" should {
        val nullValue = null
        val definedValue = 123

        "check value for not being null" in {
            Utils.notNull(nullValue) should be(false)
            Utils.notNull(definedValue) should be(true)
        }

        "check value for being null" in {
            Utils.isNull(nullValue) should be(true)
            Utils.isNull(definedValue) should be(false)
        }

        val emptyArray = Array()

        "make String from first n elements of array" in {
            val fullArray = Array(1,2,3)

            Utils.toString(emptyArray, 1) should equal ("(List())")
            Utils.toString(fullArray, fullArray.length - 1).toCharArray should contain allOf ('1','2')
            Utils.toString(fullArray, fullArray.length + 1).toCharArray should contain allOf ('1','2','3')
        }

        "throw an Exception on negative elements count" in {
            val thrown = the [IllegalArgumentException] thrownBy Utils.toString(emptyArray, -2)
            thrown.getMessage should equal ("Incorrect -2. Must be >0")
        }
    }
}
