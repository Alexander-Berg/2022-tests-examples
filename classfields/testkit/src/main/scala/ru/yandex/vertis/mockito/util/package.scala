package ru.yandex.vertis.mockito

import org.mockito.stubbing.OngoingStubbing

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

package object util {

  implicit class RichFutureOngoingStub[T](val stub: OngoingStubbing[Future[T]]) extends AnyVal {

    def thenReturnF(result: T): OngoingStubbing[Future[T]] =
      stub.thenReturn(Future.successful(result))

    def thenThrowF(t: Throwable): OngoingStubbing[Future[T]] =
      stub.thenReturn(Future.failed(t))
  }

  implicit class RichTryOngoingStub[T](val stub: OngoingStubbing[Try[T]]) extends AnyVal {

    def thenReturnT(result: T): OngoingStubbing[Try[T]] =
      stub.thenReturn(Success(result))

    def thenThrowT(t: Throwable): OngoingStubbing[Try[T]] =
      stub.thenReturn(Failure(t))
  }
}
