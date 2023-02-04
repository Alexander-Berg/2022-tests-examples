package ru.yandex.vertis.billing.async

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.exceptions.TestFailedException
import org.scalatest.time.{Seconds, Span}
import ru.yandex.vertis.billing.SpecBase

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Try}
import concurrent.duration.DurationInt

/**
  * Base for all specs with futures for avoid 'extends' same things in each spec.
  *
  * @author dimas
  */
trait AsyncSpecBase extends SpecBase with ScalaFutures {

  implicit protected def ec: ExecutionContext = concurrent.ExecutionContext.Implicits.global

  /**
    * Default value for futures [[PatienceConfig]].
    */
  private val DefaultPatienceConfig =
    PatienceConfig(Span(60, Seconds), Span(1, Seconds))

  implicit override def patienceConfig: PatienceConfig =
    DefaultPatienceConfig

  implicit class ExtendedFutureConcept[A](f: Future[A]) {

    def toTry: Try[A] =
      Try(f.futureValue).recoverWith { case e: TestFailedException =>
        Failure(e.getCause)
      }

    def await: A =
      Await.result(f, 5.seconds)
  }
}
