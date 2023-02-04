package ru.yandex.vertis.telepony

import org.scalactic.{TraversableEqualityConstraints, TypeCheckedTripleEquals}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterEach, Inside, OptionValues}
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.{Await, Future}
import scala.reflect.ClassTag
import scala.concurrent.duration.DurationInt

/**
  * Base for all specs for avoid 'extends' same things in each spec.
  *
  * @author dimas
  */
trait SpecBase
  extends AnyWordSpecLike
  with Matchers
  with TypeCheckedTripleEquals
  with TraversableEqualityConstraints
  with ScalaFutures
  with OptionValues
  with Inside
  with Eventually
  with BeforeAndAfterEach {
  implicit override def patienceConfig: PatienceConfig = DefaultPatienceConfig

  private val DefaultPatienceConfig =
    PatienceConfig(timeout = Span(15, Seconds), interval = Span(50, Milliseconds))

  implicit class TestExtensions(val obj: AnyRef) {

    def viewedAs[T: ClassTag]: T = {
      obj shouldBe a[T]
      obj.asInstanceOf[T]
    }
  }

  implicit class FutureExtensions[T](f: Future[T]) {

    def await: T =
      Await.result(f, 5.seconds)
  }
}
