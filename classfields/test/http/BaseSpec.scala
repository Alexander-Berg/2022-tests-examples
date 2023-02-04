package ru.yandex.vertis.baker.util.test.http

import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 18.02.17
  */
trait BaseSpec
  extends AnyWordSpecLike
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
