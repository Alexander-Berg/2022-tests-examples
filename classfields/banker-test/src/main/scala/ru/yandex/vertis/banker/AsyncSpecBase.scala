package ru.yandex.vertis.banker

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.exceptions.TestFailedException
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Try}

/**
  * @author ruslansd
  */
trait AsyncSpecBase extends ScalaFutures {

  /**
    * Default value for futures [[PatienceConfig]].
    */
  private val DefaultPatienceConfig =
    PatienceConfig(Span(120, Seconds), Span(10, Millis))

  implicit override def patienceConfig: PatienceConfig =
    DefaultPatienceConfig

  implicit def ec: ExecutionContext = concurrent.ExecutionContext.Implicits.global

  implicit class ExtendedFutureConcept[A](f: Future[A]) {

    def toTry: Try[A] =
      Try(f.futureValue).recoverWith { case e: TestFailedException =>
        Failure(e.getCause)
      }

    def await: A =
      Await.result(f, 5.seconds)
  }

}
