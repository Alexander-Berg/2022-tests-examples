package common.zio.ops.tracing.test

import common.zio.ops.tracing.Tracing
import common.zio.ops.tracing.testkit.TestTracing
import zio.ZIO
import zio.clock.Clock
import zio.duration._
import zio.stream.ZStream
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._

object TracingSpec extends DefaultRunnableSpec {

  private def catchSpanAfterInterrupt[R, E, A](zio: ZIO[R, E, A]) = {
    ZIO.uninterruptibleMask { restore =>
      restore(zio.forever).catchSomeCause {
        case c if c.interrupted =>
          Tracing.currentSpan
      }
    }
  }

  def spec =
    suite("Tracing")(
      testM("correctly clean span from FiberRef for ZIO") {
        val test =
          catchSpanAfterInterrupt {
            Tracing.withSpan("test")(ZIO.sleep(1.millis))
          }
        for {
          fiber <- test.fork
          _ <- ZIO.sleep(20.millis)
          res <- fiber.interrupt
        } yield assert(res)(succeeds(isNone) || isInterrupted)
      }.provideLayer(Clock.live >+> TestTracing.mock) @@ nonFlaky(200),
      testM("correctly clean span from FiberRef for ZStream") {
        val test =
          catchSpanAfterInterrupt {
            Tracing.withSpanStream("test")(ZStream.fromEffect(ZIO.sleep(1.millis))).runDrain
          }
        for {
          fiber <- test.fork
          _ <- ZIO.sleep(20.millis)
          res <- fiber.interrupt
        } yield assert(res)(succeeds(isNone) || isInterrupted)
      }.provideLayer(Clock.live >+> TestTracing.mock) @@ nonFlaky(200)
    )
}
