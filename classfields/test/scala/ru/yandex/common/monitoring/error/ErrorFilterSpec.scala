package ru.yandex.common.monitoring.error

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}

/**
  * [[ErrorFilter]] specs.
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
class ErrorFilterSpec
  extends WordSpec
    with Matchers {

  "ErrorFilter.Block" should {
    val block = ErrorFilter.Block({ case _: IllegalArgumentException => true })
    "block error" in {
      block.filter(new IllegalArgumentException) should be(false)
    }
    "pass error" in {
      block.filter(new NoSuchElementException) should be(true)
    }
  }

  "ErrorFilter.Pass" should {
    val pass = ErrorFilter.Pass({ case _: IllegalArgumentException => true })
    "pass error" in {
      pass.filter(new IllegalArgumentException) should be(true)
    }
    "block error" in {
      pass.filter(new NoSuchElementException) should be(false)
    }
  }

}
