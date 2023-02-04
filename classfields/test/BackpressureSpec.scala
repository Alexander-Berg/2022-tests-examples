package vertis.zio.stream

import vertis.zio.test.ZioSpecBase.TestBody
import vertis.zio.test.ZioSpecBase

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class BackpressureSpec extends ZioSpecBase {

  "Backpressure" should {
    "work" in test(1) { backpressure =>
      for {
        _ <- backpressure.write
        delayedWrite <- backpressure.write.fork
        _ <- backpressure.ack
        _ <- delayedWrite.join
        _ <- backpressure.ack
      } yield ()
    }

    s"interrupt on close" in test(1) { backpressure =>
      for {
        _ <- backpressure.write
        writingFiber <- backpressure.write.forkDaemon
        _ <- backpressure.close
        res <- writingFiber.join.absorb.either
        _ <- check("got an interrupt")(res.isLeft shouldBe true)
      } yield ()
    }
  }

  private def test(limit: Int)(body: Backpressure => TestBody): Unit = {
    ioTest(Backpressure.make(limit).use(body))
  }
}
