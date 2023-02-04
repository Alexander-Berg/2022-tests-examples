package ru.yandex.vertis.parsing.util

import org.scalatest.exceptions.TestFailedException

/**
  * TODO
  *
  * @author aborunov
  */
object TestUtils {

  def cause[A](action: => A): A = {
    try {
      action
    } catch {
      case e: TestFailedException if e.getCause != null =>
        throw e.getCause
    }
  }
}
