package ru.auto.salesman.test

import org.scalamock.handlers.CallHandler
import zio.{URIO, ZIO}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait ScalamockCallHandlers {

  // Can't extends AnyVal due to being inside trait :(
  // But it's more convenient, because doesn't require an import.
  // Also, handler can't be private, because it's used in methods return type.
  implicit class RichFutureCallHandler[R](val handler: CallHandler[Future[R]]) {

    def returningF(value: R): handler.Derived =
      handler.returning(Future.successful(value))

    def throwingF(e: Throwable): handler.Derived =
      handler.returning(Future.failed(e))
  }

  implicit class RichTryCallHandler[R](val handler: CallHandler[Try[R]]) {

    def returningT(value: R): handler.Derived =
      handler.returning(Success(value))

    def throwingT(e: Throwable): handler.Derived =
      handler.returning(Failure(e))
  }

  implicit class RichZIOCallHandler[R, E, A](
      val handler: CallHandler[ZIO[R, E, A]]
  ) {

    def returningZ(value: A): handler.Derived =
      handler.returning(ZIO.succeed(value))

    def throwingZ(e: E)(implicit ev: E <:< Throwable): handler.Derived =
      handler.returning(ZIO.fail(e))
  }

  // scalac can't resolve RichZIOCallHandler when E = Nothing.
  // So, need separate implicit wrapper.
  implicit class RichURIOCallHandler[R, A](
      val handler: CallHandler[URIO[R, A]]
  ) {

    def returningZ(value: A): handler.Derived =
      handler.returning(ZIO.succeed(value))
  }
}

object ScalamockCallHandlers extends ScalamockCallHandlers
