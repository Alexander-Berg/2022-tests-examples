package ru.yandex.vertis.hipe.clients

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpecLike}
import org.scalatest.concurrent.{Eventually, ScalaFutures}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 18.02.17
  */
trait BaseSpec
  extends WordSpecLike
  with Matchers
  with ScalaFutures
  with Eventually
  with BeforeAndAfter
  with BeforeAndAfterAll {

  override val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected = true

  implicit class AwaitableFuture[T](f: Future[T]) {
    def await: T = Await.result(f, Duration.Inf)
  }

}
