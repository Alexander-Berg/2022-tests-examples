package ru.yandex.vertis.chat

import org.mockito.stubbing.OngoingStubbing
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.exceptions.TestFailedException
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpec}

import scala.concurrent.Future

/**
  * Base for Specs.
  *
  * @author dimas
  */
trait SpecBase extends WordSpec with Matchers with BeforeAndAfterAll with BeforeAndAfter with ScalaFutures {

  override val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected = true

  implicit override def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(20, Seconds))

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

  implicit class RichOngoingStub[T](stub: org.mockito.stubbing.OngoingStubbing[Future[T]]) {
    def thenReturnF(result: T): OngoingStubbing[Future[T]] = stub.thenReturn(Future.successful(result))

    def thenThrowF(t: Throwable): OngoingStubbing[Future[T]] = stub.thenReturn(Future.failed(t))
  }
}
