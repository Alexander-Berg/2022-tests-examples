package ru.yandex.realty

import org.scalactic.source.Position
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpecLike}
import org.scalatest.exceptions.{StackDepthException, TestFailedException}

import scala.reflect.ClassTag
import scala.util.{Failure, Try}

/**
  * Base traits blend for specs.
  *
  * @author dimas
  */
trait SpecBase extends WordSpecLike with Matchers with BeforeAndAfter with BeforeAndAfterAll with MockFactory {

  /**
    * Wraps default intercept implementation with skipping of scalatest-specific exceptions
    */
  override def intercept[T <: AnyRef](f: => Any)(implicit classTag: ClassTag[T], pos: Position): T = {
    Try(f) match {
      case Failure(e: StackDepthException) =>
        throw e
      case other =>
        super.intercept(other.get)
    }
  }

  /**
    * Rethrows exception cause
    * if an non-fatal exception is thrown during given action.
    */
  //scalastyle:off
  @deprecated("For futures use AsyncSpecBase.interceptCause. For other cases you don't need for causes.")
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
