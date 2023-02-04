package ru.yandex.realty.persistence.cassandra

import org.scalatest.Assertions
import org.scalatest.concurrent.Waiters

import scala.concurrent.Future
import scala.util.{Failure, Success}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by abulychev on 18.07.16.
  */
object FailingHelper {
  implicit class Failing[A](val f: Future[A]) extends Assertions with Waiters {

    def failing[T <: Throwable](implicit m: Manifest[T]): T = {
      val w = new Waiter
      f.onComplete {
        case Failure(e) => w(throw e); w.dismiss()
        case Success(_) => w.dismiss()
      }
      intercept[T] {
        w.await
      }
    }
  }
}
