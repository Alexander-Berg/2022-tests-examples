package ru.yandex.realty

import org.scalactic.source.Position
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.exceptions.TestFailedException
import org.scalatest.time.{Millis, Seconds, Span}
import ru.yandex.realty.application.ng.ExecutionContextProvider
import ru.yandex.vertis.util.concurrent.Threads

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * Base for all specs with futures for avoid 'extends' same things in each spec.
  *
  * @author dimas
  */
trait AsyncSpecBase extends SpecBase with ScalaFutures with ExecutionContextProvider {

  private val DefaultTestPoolSize = 16

  implicit override lazy val ec: ExecutionContext =
    Threads.newForkJoinPoolExecutionContext(DefaultTestPoolSize, s"${getClass.getSimpleName}-ec")

  /**
    * Unwrap cause exception from [[TestFailedException]]
    *
    * It is useful when you need to test [[Future]]'s failure
    */
  def interceptCause[T <: AnyRef](f: => Any)(implicit classTag: ClassTag[T], pos: Position): T = {
    intercept {
      try {
        f
      } catch {
        case e: TestFailedException if e.getCause != null =>
          throw e.getCause
      }
    }
  }

  /**
    * Default value for futures [[PatienceConfig]].
    */
  private val DefaultPatienceConfig =
    PatienceConfig(Span(5, Seconds), Span(15, Millis))

  implicit override def patienceConfig: PatienceConfig =
    DefaultPatienceConfig
}
