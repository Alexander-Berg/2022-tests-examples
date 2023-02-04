package ru.yandex.vertis.vsquality.hobo.util

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{Assertion, BeforeAndAfter}
import org.specs2.matcher.ThrownExpectationsCreation
import org.specs2.mock.Mockito
import ru.yandex.vertis.scalatest.matcher.SmartEqualMatcher

import scala.concurrent.Future
import scala.reflect.ClassTag

/**
  * Base trait for every Spec
  *
  * @author semkagtn
  */
trait SpecBase
  extends AnyWordSpec
  with BeforeAndAfter
  with Matchers
  with ScalaFutures
  with Mockito
  with ThrownExpectationsCreation {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(15, Seconds), interval = Span(50, Millis))

  def smartEqual[A](right: A): SmartEqualMatcher[A] = SmartEqualMatcher.smartEqual(right)

  implicit class TestFuture[T](val future: Future[T]) {

    def shouldCompleteWithException[A <: Throwable: ClassTag]: Assertion =
      whenReady(future.failed) { e =>
        e shouldBe a[A]
      }
  }

}
