package ru.yandex.vertis.subscriptions

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.exceptions.TestFailedException
import org.scalatest.{Matchers, WordSpec}

/**
  * Base traits blend for specs.
  *
  * @author dimas
  */
trait SpecBase extends WordSpec with Matchers with ScalaFutures {

  /**
    * Rethrows exception cause
    * if an non-fatal exception is thrown during given action.
    */
  //scalastyle:off
  def cause[A](action: => A): A = {
    try {
      action
    } catch {
      case e: TestFailedException if e.getCause != null =>
        throw e.getCause
    }
  }
  //scalastyle:on

}
