package ru.yandex.vertis.vsquality.techsupport.util

import cats.effect.{ContextShift, Effect, IO, Timer}
import org.scalacheck.Shrink
import org.scalatest._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.scalatest.matcher.SmartEqualMatcher
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions
import scala.reflect.ClassTag

trait SpecBase
  extends AnyWordSpec
  with Matchers
  with ScalaCheckPropertyChecks
  with MockitoSupport
  with BeforeAndAfter
  with BeforeAndAfterAll {
  type F[+T] = SpecBase.F[T]
  implicit val effect: Effect[F] = cats.effect.IO.ioEffect

  implicit protected val contextShift: ContextShift[F] = IO.contextShift(global)

  implicit protected val timer: Timer[IO] = IO.timer(global)

  implicit def noShrink[A]: Shrink[A] = Shrink.shrinkAny

  implicit class RichIO[T](io: IO[T]) {

    def shouldFailWith[E <: Throwable: ClassTag]: Assertion =
      assertThrows[E] {
        io.await
      }

    def shouldFail: Assertion = shouldFailWith[Throwable]
  }

  implicit class RichEither[A, B](either: Either[A, B]) {

    def shouldFailWith[E <: A: ClassTag]: Assertion =
      either match {
        case Left(_: E) => succeed
        case other      => fail(s"unexpected result $other")
      }

    def toF(implicit evidence: A => Throwable): F[B] =
      IO.fromEither(either.left.map(evidence))
  }

  def smartEqual[A](right: A): SmartEqualMatcher[A] =
    SmartEqualMatcher.smartEqual(right)
}

object SpecBase {
  type F[+T] = IO[T]
}
