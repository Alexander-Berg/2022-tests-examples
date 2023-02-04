package vertis.zio.actor

import vertis.zio.actor.Actor.Insides
import vertis.zio.actor.ZioActor.{ActorBody, ActorSettings}
import ActorSpec._
import vertis.zio.BaseEnv
import common.zio.logging.Logging
import vertis.zio.test.ZioSpecBase
import zio._
import zio.duration._

import scala.util.Random

/** @author zvez
  */
class ActorSpec extends ZioSpecBase with ActorTest {

  "Actor" should {
    type M = Promise[Nothing, Int]

    "process messages" in ioTest {
      for {
        actor <- createActor[Logging.Logging, M, Int](0) { (_, s, msg) =>
          val nstate = s + 1
          for {
            _ <- msg.succeed(nstate)
          } yield nstate
        }
        expected = 1 to 1000
        msgs <- ZIO.collectAllPar(
          expected.map { _ =>
            Promise.make[Nothing, Int].flatMap { p =>
              for {
                _ <- actor.send(p)
                res <- p.await
              } yield res
            }
          }
        )
        _ <- actor.close
      } yield msgs should contain theSameElementsAs expected
    }

    "close" in ioTest {
      for {
        closedRef <- Ref.make(0)
        actor <- unitActor(closedRef.update(_ + 1))
        _ <- actor.close
        _ <- checkClosed(actor, closedRef)
      } yield ()
    }

    "close just once if closed in parallel" in ioTest {
      for {
        closedRef <- Ref.make(0)
        actor <- unitActor(closedRef.update(_ + 1))
        n <- randomNatural()
        _ <- logger.info(s"Gonna close actor $n times in parallel")
        _ <- ZIO.collectAllPar(ZIO.replicate(n)(actor.close))
        _ <- checkClosed(actor, closedRef)
      } yield ()
    }

    s"close just once if closed from the outside and on error" in ioTest {
      for {
        closedRef <- Ref.make(0)
        chanceOfFailure <- randomNatural(10)
        actor <- unitActor(closedRef.update(_ + 1), bodyWithRandomFailure(chanceOfFailure))
        n <- randomNatural(20)
        requests = ZIO.replicate(n * chanceOfFailure)(actor.send(()))
        closeRequests = ZIO.replicate(n)(actor.close)
        _ <- ZIO.collectAllPar(Random.shuffle(requests ++ closeRequests).toSeq)
        _ <- checkClosed(actor, closedRef)
      } yield ()
    }

    List("failure" -> bodyWithFailure, "defect" -> bodyWithDefect, "interrupt" -> bodyWithInterrupt).foreach {
      case (error, body) =>
        s"close on $error" in ioTest {
          for {
            closedPromise <- Promise.make[Throwable, Unit]
            actor <- unitActor(closedPromise.succeed(()).unit, body)
            _ <- actor.send(())
            _ <- checkM("actor properly closed")(closedPromise.await.either.map(_.isRight shouldBe true))
          } yield ()
        }
    }

    "process all acknowledged before closing" in ioTest {
      for {
        res <- Ref.make(List.empty[Int])
        actor <- ZioActor.create[BaseEnv, Int, List[Int]](
          ActorSettings(
            Nil,
            "test",
            ActorBody(
              (_, s, m) => logger.info(s"processing $m").as(m :: s) <* ZIO.when(s.isEmpty)(ZIO.sleep(200.millis)),
              close = res.set
            )
          )
        )
        n <- randomNatural(100)
        requests <- ZIO
          .collectAll(ZIO.replicate(n)(zio.random.nextInt))
          .map(_.map(actor.sendAck))
          .map(_.toSeq :+ actor.close.as(false))
        acked <- ZIO.collectAllParN(16)(requests).map(_.count(ack => ack))
        _ <- logger.info(s"Got $acked/$n acks")
        _ <- checkM(s"All $acked messages received")(res.get.map(_.size shouldBe acked))
      } yield ()
    }

    "wait till all messages are processed on close" in ioTest {
      val count = 10000
      for {
        counterRef <- Ref.make(0)
        actor <- createActor[Logging.Logging, Unit, Unit](()) { (_, _, _) =>
          counterRef.update(_ + 1)
        }
        _ <- ZIO.foreach_(1 to count)(_ => actor.send(()))
        _ <- actor.close
        _ <- checkM("processed all sent before close")(counterRef.get.map(_ shouldBe count))
      } yield ()
    }
  }

  private def checkClosed(actor: Actor[_], closedRef: Ref[Int]) =
    checkM("Actor is closed")(actor.isClosed.map(_ shouldBe true)) *>
      checkM("Closed exactly once")(closedRef.get.map(_ shouldBe 1))
}

object ActorSpec {
  private def paws = new UnsupportedOperationException("paws")

  val emptyBody: Insides[Unit, Any, Unit] =
    (_, _, _) => UIO.unit

  val bodyWithFailure: Insides[Unit, Any, Unit] =
    (_, _, _) => Task.fail(paws)

  val bodyWithDefect: Insides[Unit, Any, Unit] =
    (_, _, _) => UIO(throw paws)

  val bodyWithInterrupt: Insides[Unit, Any, Unit] =
    (_, _, _) => Task.interrupt

  def bodyWithRandomFailure[M](chanceOfFailure: Int): Insides[Unit, zio.random.Random, M] =
    (_, _, _) => ZIO.whenM(zio.random.nextIntBounded(chanceOfFailure).map(_ == 0))(Task.fail(paws))

  def unitActor(onClose: UIO[Unit], body: Insides[Unit, BaseEnv, Unit] = emptyBody): URIO[BaseEnv, Actor[Unit]] =
    ZioActor.create[BaseEnv, Unit, Unit](ActorSettings((), "test", ActorBody(body, close = _ => onClose)))
}
