package ru.yandex.vertis.scalamock

import org.scalamock.handlers.CallHandler

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

package object util {

  implicit class RichFutureCallHandler[R](val handler: CallHandler[Future[R]]) extends AnyVal {

    def returningF(value: R): handler.Derived =
      handler.returning(Future.successful(value))

    def throwingF(e: Throwable): handler.Derived =
      handler.returning(Future.failed(e))
  }

  implicit class RichTryCallHandler[R](val handler: CallHandler[Try[R]]) extends AnyVal {

    def returningT(value: R): handler.Derived =
      handler.returning(Success(value))

    def throwingT(e: Throwable): handler.Derived =
      handler.returning(Failure(e))
  }
}
