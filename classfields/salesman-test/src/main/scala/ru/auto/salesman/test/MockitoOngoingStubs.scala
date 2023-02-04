package ru.auto.salesman.test

import org.mockito.stubbing.OngoingStubbing
import zio.ZIO

trait MockitoOngoingStubs {

  implicit class RichZIOOngoingStub[R, E, A](
      private val stub: OngoingStubbing[ZIO[R, E, A]]
  ) {

    def thenReturnZ(value: A): OngoingStubbing[ZIO[R, E, A]] =
      stub.thenReturn(ZIO.succeed(value))

    def thenThrowZ(e: E)(
        implicit ev: E <:< Throwable
    ): OngoingStubbing[ZIO[R, E, A]] =
      stub.thenReturn(ZIO.fail(e))
  }
}
