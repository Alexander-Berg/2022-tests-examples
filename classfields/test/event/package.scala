package ru.yandex.vertis.billing

import scala.annotation.nowarn
import scala.collection.mutable

/**
  * Utils for test statistics
  *
  * @author alesavin
  */
package object event {

  def accumulate[A](acc: => mutable.ListBuffer[A]): (A => Unit) = { e =>
    acc += e: @nowarn("msg=discarded non-Unit value")
  }
}
