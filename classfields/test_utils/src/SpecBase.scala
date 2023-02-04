package ru.yandex.vertis.vsquality.utils.test_utils

import java.nio.charset.StandardCharsets

import cats.effect.{ConcurrentEffect, ContextShift, IO, Timer}
import org.scalacheck.Shrink
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{Assertion, BeforeAndAfter, BeforeAndAfterAll}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._
import ru.yandex.vertis.scalatest.matcher.SmartEqualMatcher

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.BufferedSource
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

  implicit protected val contextShift: ContextShift[F] = IO.contextShift(global)

  implicit val concurrentEffect: ConcurrentEffect[F] = cats.effect.IO.ioConcurrentEffect(contextShift)

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

    def toF(implicit evidence: A => Throwable): F[B] = IO.fromEither(either.left.map(evidence))
  }

  def smartEqual[A](right: A): SmartEqualMatcher[A] = SmartEqualMatcher.smartEqual(right)

  private def readResourceFile(path: String): BufferedSource =
    scala.io.Source.fromInputStream(getClass.getResourceAsStream(path), StandardCharsets.UTF_8.name())

  protected def readResourceFileAsString(path: String): String = readResourceFile(path).mkString
}

object SpecBase {
  type F[+T] = IO[T]
}
