package ru.auto.salesman.test

import org.joda.time.DateTime

import java.time.{OffsetDateTime, ZoneId}
import java.util.concurrent.TimeUnit
import org.scalactic.source.Position
import org.scalatest.exceptions.{StackDepthException, TestFailedException}
import ru.auto.salesman._
import ru.auto.salesman.util.{AutomatedContext, HasRequestContext, RequestContext}
import zio.Exit.{Failure, Success}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration.Duration
import zio.{Exit, ZIO}

trait ZIOValues {

  private val testRc: RequestContext = AutomatedContext("test")

  implicit class TestZIO[E, A](private val zio: IO[E, A]) {

    def provideConstantClock(now: DateTime): IO[E, A] =
      zio.provideSomeLayer[Blocking with HasRequestContext] {
        ZIO.succeed(constantClock(now)).toLayer
      }

    def success(implicit pos: Position): Success[A] =
      unsafeRunSync(zio) match {
        case success: Success[A] => success
        case Failure(cause) =>
          val messageFun = (_: StackDepthException) =>
            Some("The ZIO result on which success was invoked was not a Success.")
          val causeThrowable = cause.squashWith {
            case e: Throwable => e
            case e => new Exception(e.toString)
          }
          throw new TestFailedException(
            messageFun,
            Some(causeThrowable),
            pos
          )
      }

    def failure(implicit pos: Position): Failure[E] =
      unsafeRunSync(zio) match {
        case failure: Failure[E] => failure
        case success =>
          throw new TestFailedException(
            (_: StackDepthException) =>
              Some(
                s"The ZIO result on which failure was invoked was not a Failure: $success."
              ),
            None,
            pos
          )
      }

    private def unsafeRunSync(zio: IO[E, A]): Exit[E, A] =
      ZIORuntime.unsafeRunSync(zio.provideRc(testRc))
  }

  implicit class TestFailure(private val failure: Failure[Throwable]) {

    def exception(implicit pos: Position): Throwable =
      failure.cause.squash
  }

  def constantClock(now: DateTime): Clock.Service =
    new Clock.Service {

      def currentTime(unit: TimeUnit): ZIO[Any, Nothing, Long] =
        ZIO.succeed(unit.convert(now.getMillis * 1000000, TimeUnit.NANOSECONDS))

      def currentDateTime: ZIO[Any, Nothing, OffsetDateTime] =
        ZIO.succeed(
          OffsetDateTime
            .ofInstant(
              java.time.Instant.ofEpochMilli(now.toInstant.getMillis),
              ZoneId.of(now.getZone().getID())
            )
        )

      override val nanoTime: ZIO[Any, Nothing, Long] =
        ZIO.succeed(now.getMillis)
      def sleep(duration: Duration): ZIO[Any, Nothing, Unit] = ZIO.unit
    }
}
