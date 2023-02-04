package vs.registry.consumer

import zio.test.{TestAspect, *}
import zio.*

object FiberSpec extends ZIOSpecDefault {

  val suites = suite("Fibers")(
    test("fork") {
      for {
        ref   <- ZIO.service[Ref[Int]]
        _     <- ref.set(0)
        fiber <- refLoop(ref).forever.fork
        _     <- TestClock.adjust(2.seconds)
        _     <- fiber.interrupt
        i     <- ref.get
      } yield assertTrue(i >= 20 && i <= 21)
    },
    test("forkDaemon in one test") {
      for {
        ref <- ZIO.service[Ref[Int]]
        _   <- ref.set(0)
        fiber <-
          refLoop(ref)
            .repeatWhile(_ => true)
            .catchAllCause { error =>
              ZIO.succeed(println(s"Pipeline failed: $error"))
            }
            .ensuring(ZIO.succeed(println(s"Finished")))
            .forkDaemon

        _ <- TestClock.adjust(2.seconds)
        _ <- fiber.interrupt
        i <- ref.get
      } yield assertTrue(i >= 20 && i <= 21)
    },
    /** Этот тест не работает на zio1 и zio2 в обоих случаях
      * forkDaemon/forkInScope, c любым Scope. Вместе с завершением первого test
      * закрывается скоуп, ref перестает обновляться.
      */
    suite("forkDaemon/forkInScope in two parts")(
      test("schedule reference update") {
        for {
          ref      <- ZIO.service[Ref[Int]]
          fiberRef <- ZIO.service[Ref[Option[Fiber[Nothing, Unit]]]]
          scope    <- ZIO.service[Scope]
          _        <- ref.set(0)
          fiber <- refLoop(ref)
            .repeatWhile(_ => true)
            .catchAllCause { error =>
              ZIO.succeed(println(s"Pipeline failed: $error"))
            }
            .ensuring(ZIO.succeed(println(s"Finished")))
            .forkIn(scope)
          _ <- fiberRef.set(Some(fiber))

        } yield assertTrue(true)
      },
      test("read updated reference") {
        for {
          ref      <- ZIO.service[Ref[Int]]
          fiberRef <- ZIO.service[Ref[Option[Fiber[Nothing, Unit]]]]
          fiber    <- fiberRef.get.map(_.get)
          _        <- TestClock.adjust(2.seconds)
          _        <- fiber.interrupt
          i        <- ref.get
        } yield assertTrue(i >= 20 && i <= 21)
      },
    ) @@ TestAspect.ignore,
  ).provideLayerShared(
    ZLayer.fromZIO(Ref.make(0)) ++
      ZLayer.fromZIO(Ref.make[Option[Fiber[Nothing, Unit]]](None)) ++
      ZLayer.fromZIO(Scope.make) ++ Annotations.live,
  )

  override def spec = suites @@ TestAspect.sequential

  def refLoop(ref: Ref[Int]) =
    for {
      _ <- ref.update(_ + 1)
      _ <- Clock.sleep(100.milliseconds)
    } yield ()

}
