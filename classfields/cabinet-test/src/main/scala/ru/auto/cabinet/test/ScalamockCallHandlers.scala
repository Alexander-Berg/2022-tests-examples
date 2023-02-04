package ru.auto.cabinet.test

import org.scalamock.handlers.CallHandler

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
}
