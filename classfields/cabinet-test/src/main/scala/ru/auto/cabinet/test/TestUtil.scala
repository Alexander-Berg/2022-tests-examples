package ru.auto.cabinet.test

import org.mockito.stubbing.OngoingStubbing

import scala.concurrent.Future

object TestUtil {

  implicit class RichOngoingStub[T](
      stub: org.mockito.stubbing.OngoingStubbing[Future[T]]) {

    def thenReturnF(result: T): OngoingStubbing[Future[T]] =
      stub.thenReturn(Future.successful(result))

    def thenThrowF(t: Throwable): OngoingStubbing[Future[T]] =
      stub.thenReturn(Future.failed(t))
  }
}
