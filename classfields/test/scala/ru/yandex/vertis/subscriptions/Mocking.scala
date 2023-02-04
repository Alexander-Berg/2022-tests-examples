package ru.yandex.vertis.subscriptions

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import org.scalamock.util.Defaultable

import scala.concurrent.Future

/**
  *
  * @author zvez
  */
trait Mocking extends MockFactory {

  implicit class CallHandlerFutureHelper[R: Defaultable](val ch: CallHandler[Future[R]]) {
    def returnsF(value: R): ch.Derived = ch.returns(Future.successful(value))
    def failsF(ex: Exception): ch.Derived = ch.returns(Future.failed(ex))
  }

}
