package vertis.zio.test

import common.zio.logging.Logging
import common.zio.ops.prometheus.Prometheus
import common.zio.ops.prometheus.Prometheus.Prometheus
import common.zio.ops.tracing.testkit.TestTracing
import common.zio.ops.tracing.Tracing.Tracing
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.ops.prometheus.CompositeCollector
import vertis.zio.test.ZioSpecBase.{TestBody, TestEnv}
import vertis.zio.util.ZioFiberEnrichers._
import vertis.zio.util.ZioExit._
import vertis.zio.util.{Errors, ZioFiles, ZioRunUtils}
import zio._
import zio.clock.Clock
import zio.duration._
import zio.internal.Platform
import zio.random.Random

import scala.concurrent.{Future, TimeoutException}
import scala.util.Try

/** Run zio tests in default runtime
  *
  * @author Ratskevich Natalia reimai@yandex-team.ru
  */
trait ZioSpecBase extends AnyWordSpec with Matchers with Logging with ZioFiles {

  val logger: Logging.Service = log

  implicit val ops: TestOperationalSupport.type = TestOperationalSupport

  protected lazy val defaultRt: Runtime[Unit] =
    Runtime((), Platform.default.withFatal(Errors.isFatal))

  protected val ioTestTimeout: Duration = 1.minute

  protected val env: ZLayer[Any, Throwable, TestEnv] =
    ZEnv.live >>> Logging.live ++ ZEnv.live ++ ZLayer.succeed[Prometheus.Service](
      new Prometheus.Live(new CompositeCollector())
    ) ++ TestTracing.mock

  def runSync[A](io: => RIO[TestEnv, A]): Try[A] =
    ZioRunUtils.runSync(defaultRt)(io.provideLayer(env))

  def runAsync[A](io: => RIO[TestEnv, A]): Future[A] =
    ZioRunUtils.runAsync(defaultRt)(io.provideLayer(env))

  def ioTest(body: => TestBody): Unit = customIoTest()(body)

  def customIoTest(timeout: Duration = ioTestTimeout)(body: => TestBody): Unit =
    runSync {
      body
        .withFiberName("io-test")
        .mapError {
          case t: Throwable => t
          case other => new RuntimeException(s"Test finished with unexpected error: $other")
        }
        .absorb
        .timeoutDumpAndFail(new TimeoutException(s"Test timeout after $timeout"), timeout)
    }.get: Unit

  def check(clue: String)(assertion: => Assertion): Task[Assertion] =
    Task(withClue(clue)(assertion))

  def checkM[R, E <: Throwable](clue: String)(assertion: => ZIO[R, E, Assertion]): RIO[R, Assertion] =
    assertion.absorb.either.map { eitherA =>
      withClue(clue)(eitherA.toTry.get)
    }

  def checkFailed[R, E, E1 <: E](action: ZIO[R, E, Any]): RIO[R, Unit] =
    action.foldM(
      {
        case e if e.isInstanceOf[E] => UIO.unit
        case other => ZIO.fail(new IllegalStateException(s"The action failed with $other"))
      },
      s => ZIO.fail(new IllegalStateException(s"The action succeeded with $s"))
    )

  def checkInterrupted[R, E](action: ZIO[R, E, Any]): RIO[R, Unit] =
    action.foldCauseM[R, Throwable, Unit](
      {
        case i if i.interrupted => UIO.unit
        case other =>
          ZIO.fail(new IllegalStateException(s"The action was not interrupted, but failed with ${other.prettyPrint}"))
      },
      _ => ZIO.fail(new IllegalStateException("The action succeeded"))
    )

  def checkDied[R](action: ZIO[R, Any, Any], deathCause: Any): RIO[R, Unit] =
    action.foldCauseM[R, Throwable, Unit](
      {
        case i if i.dieOption.contains(deathCause) => UIO.unit
        case other =>
          ZIO.fail(
            new IllegalStateException(
              s"The action did not die from the $deathCause, but failed with ${other.prettyPrint}"
            )
          )
      },
      _ => ZIO.fail(new IllegalStateException("The action succeeded"))
    )

  def checkEventually[R <: Clock, E <: Throwable](
      clue: String
    )(assertion: => ZIO[R, E, Assertion]
    )(patience: Schedule[R, Any, Any]): ZIO[R, Throwable, Assertion] =
    assertion.absorb.map(a => withClue(clue)(a)).retry(patience)

  def check(assertion: => Assertion): Task[Assertion] = Task(assertion)

  def randomNatural(max: Int = 100): URIO[Random, Int] =
    zio.random.nextIntBounded(max).map(_ + 1)

  // probably should move it somewhere
  implicit class ZioForTest[R, E, A](z: ZIO[R, E, A]) {

    def checkResult(f: A => Assertion)(implicit ev: E <:< Throwable): ZIO[R, Throwable, A] =
      z.mapError(ev).tap(r => check(f(r)))
  }
}

object ZioSpecBase {
  type TestEnv = ZEnv with Prometheus with Logging.Logging with Tracing
  type TestBody = ZIO[TestEnv, Any, _]
}
