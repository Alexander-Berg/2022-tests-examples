package ru.yandex.realty.componenttest.utils

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

trait ComponentTestFutureSupport {

  def awaitForReady(future: Future[_], atMost: FiniteDuration = 10.seconds): future.type =
    FutureUtils.awaitForReady(future, atMost)

  def awaitForResult[T](future: Future[T], atMost: FiniteDuration = 10.seconds): T =
    FutureUtils.awaitForResult(future, atMost)

}

object FutureUtils {

  def awaitForReady(future: Future[_], atMost: FiniteDuration = 10.seconds): future.type =
    Await.ready(future, atMost)

  def awaitForResult[T](future: Future[T], atMost: FiniteDuration = 10.seconds): T =
    Await.result(future, atMost)

}
