package vertis.zio.queue

import common.zio.logging.Logging
import vertis.zio.queue.HalfClosingZQueue.HalfClosingQueue
import vertis.zio.test.ZioSpecBase
import zio._
import zio.clock.Clock
import zio.duration._
import zio.random.Random

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class HalfClosingZQueueSpec extends ZioSpecBase {

  "HalfClosingZQueue" should {
    "work as a queue" in ioTest {
      HalfClosingZQueue.unbounded[Int]().use { q =>
        for {
          _ <- q.offer(1)
          _ <- checkM("got value from q")(q.take.map(_ shouldBe 1))
        } yield ()
      }
    }

    "wait to be drained" in ioTest {
      HalfClosingZQueue
        .unbounded[Int]()
        .use { q =>
          q.offer(1)
        }
        .disconnect
        .timeout(1.second)
        .map(_ shouldBe None)
    }

    "not wait on shutdown" in ioTest {
      HalfClosingZQueue.unbounded[Int]().use { q =>
        q.offer(1) *> q.shutdown
      }
    }

    "process all input sent before closing" in ioTest {
      HalfClosingZQueue.unbounded[Int]().use { q =>
        for {
          n <- randomNatural(80)
          sent <- send(q, n)
          closing <- q.close.fork
          received <- drain(q)
          _ <- closing.join
          _ <- check("received all")(received.size shouldBe n)
          _ <- check("sent == received")(sent shouldBe received)
        } yield ()
      }
    }

    (0 to 0).foreach { i =>
      s"process all acked input $i" in ioTest {
        HalfClosingZQueue.unbounded[Int]().use { q =>
          for {
            n <- randomNatural(80)
            sentAndClosed <- sendAndClose(q, n).fork
            received <- drain(q)
            sent <- sentAndClosed.join
            _ <- logger.info(s"Sent ${sent.size}/$n")
            _ <- logger.info(s"Received ${received.size}/$n")
            _ <- check("sent == received")(sent shouldBe received)
          } yield ()
        }
      }
    }

    "not ack after closing" in ioTest {
      HalfClosingZQueue.unbounded[Int]().use { q =>
        for {
          _ <- q.close
          _ <- checkM("not acked")(q.offer(42).map(_ shouldBe false))
          _ <- checkDied(q.put(42), HalfClosingZQueue.QueueIsHalfClosed)
        } yield ()
      }
    }

    "work with a dropping queue" in ioTest {
      HalfClosingZQueue.dropping[Int](2).use { q =>
        for {
          _ <- checkM("offered")(q.offer(1).map(_ shouldBe true))
          _ <- checkM("offered")(q.offer(2).map(_ shouldBe true))
          _ <- checkM("not offered")(q.offer(3).map(_ shouldBe false))
          _ <- q.shutdown
        } yield ()
      }
    }

    "close with timeout and release writers" in ioTest {
      for {
        p <- Promise.make[Throwable, Boolean]
        _ <- HalfClosingZQueue.bounded[Int](1, Some(3.seconds)).use { q =>
          for {
            _ <- q.offer(1) *> logger.info("put 1")
            _ <- (q.offer(2).tap(p.succeed) *> logger.info("put 2"))
              .onTermination(_ => logger.warn("Failed to put 2") *> p.fail(new RuntimeException("Put failed")))
              .forkDaemon
          } yield ()
        }
        _ <- logger.info("Waiting for second put")
        putSuccess <- p.await.either
        // put is either interrupted or failed to start
        _ <- check("Second put failed")(putSuccess should not be Right(true))
      } yield ()
    }
  }

  private def drain(q: HalfClosingQueue[Int]): URIO[Logging.Logging, Set[Int]] =
    Ref
      .make(Set.empty[Int])
      .tap { received =>
        (q.take >>= (i => received.update(_ + i))).forever
          .catchSomeCause { case i if i.interrupted => UIO.unit }
      }
      .flatMap(_.get)

  private def sendAndClose(q: HalfClosingQueue[Int], n: Int): URIO[Random with Clock with Logging.Logging, Set[Int]] =
    for {
      results <- Ref.make(Set.empty[Int])
      requests = q.close +: ZIO.replicate(n)(send(q, results).unit).toSeq
      _ <- ZIO.collectAllPar(scala.util.Random.shuffle(requests))
      sent <- results.get
    } yield sent

  private def send(q: HalfClosingQueue[Int], results: Ref[Set[Int]]): URIO[Random with Logging.Logging, Boolean] =
    for {
      i <- zio.random.nextInt
      offered <- q.offer(i)
      _ <- ZIO.when(offered)(logger.info(s"offered $i") *> results.update(_ + i))
    } yield offered

  private def send(q: HalfClosingQueue[Int], n: Int): URIO[Random with Clock with Logging.Logging, Set[Int]] =
    Ref
      .make(Set.empty[Int])
      .tap { sent =>
        send(q, sent).repeat(Schedule.recurs(n - 1))
      }
      .flatMap(_.get)
}
