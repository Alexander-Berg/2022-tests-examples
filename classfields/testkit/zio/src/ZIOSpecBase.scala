package billing.common.testkit.zio

import cats.data.NonEmptyList
import zio.blocking.Blocking
import zio.clock.Clock
import zio.random.Random
import zio.{Runtime, Task}
import cats.syntax.list._
import scala.util.Try

trait ZIOSpecBase {

  val zioRuntime: zio.Runtime[Blocking with Clock with Random] = Runtime.default

  /**
   * Использовать эти имплиситы стоит только в крайних случаях. Более правильным решением будет написание тестов на zio.
   * В биллинге это используется, так как он переписывается на zio.
   */
  implicit class RichZIO[R, E, A](zio: Task[A]) {

    def unsafeRun(): A = {
      zioRuntime.unsafeRunTask(zio)
    }

    def unsafeRunToTry(): Try[A] = {
      zioRuntime.unsafeRunSync(zio).toEither.toTry
    }
  }

  implicit class RichIterable[A](private val iterable: Iterable[A]) {

    def toNelUnsafe: NonEmptyList[A] = {
      iterable.toList.toNel.getOrElse(throw new Exception("unexpected empty iterable"))
    }
  }
}
