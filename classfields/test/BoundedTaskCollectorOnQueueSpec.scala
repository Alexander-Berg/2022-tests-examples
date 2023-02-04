package vertis.zio.tasks.waiting

import vertis.zio.util.ZioExit.RichCause
import vertis.zio.{BTask, BaseEnv}
import vertis.zio.test.ZioSpecBase
import zio._
import zio.duration.durationInt

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class BoundedTaskCollectorOnQueueSpec extends ZioSpecBase {

  private val callback = { in: Int =>
    logger.info(s"Got $in")
  }

  private val taskInf: URIO[BaseEnv, Int] =
    logger.info("Not really putting anything") *> ZIO.never

  "BoundedTaskCollector" should {
    "close by timeout if the q is not empty and not being processed" in ioTest {
      val kill = { cause: Cause[Any] =>
        logger.error(s"Task failed with ${cause.shortPrint}")
      }
      BoundedTaskCollectorOnQueue
        .make[BaseEnv, Int](10)(callback, kill, 1.second)
        .use { collector =>
          collector.offer(task(1)) *>
            collector.offer(ZIO.fail(new IllegalStateException("nope"))) *>
            collector.offer(task(2))
        }
        .unit
    }

    "close by timeout if the q is full and not being processed" in ioTest {
      val kill = { cause: Cause[Any] =>
        logger.error(s"Task failed with ${cause.shortPrint}")
      }
      BoundedTaskCollectorOnQueue
        .make[BaseEnv, Int](2)(callback, kill, 1.second)
        .use { collector =>
          collector.offer(taskInf) *>
            collector.offer(taskInf) *>
            collector.offer(taskInf) *>
            collector.offer(taskInf).fork
        }
        .unit
    }
  }

  private def task(in: Int): BTask[Int] =
    logger.info(s"Producing $in").as(in)
}
