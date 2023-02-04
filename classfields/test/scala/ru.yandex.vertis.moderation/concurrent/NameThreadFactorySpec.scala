package ru.yandex.vertis.moderation.concurrent

import java.util.concurrent.{ForkJoinPool, ThreadLocalRandom, TimeUnit}

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.util.RandomUtil

/**
  * Specs for [[NamedForkJoinWorkerThreadFactory]]
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
class NameThreadFactorySpec extends SpecBase {

  "NamedForkJoinWorkerThreadFactory" should {

    trait Named {

      def name: Option[String]
    }

    def newRunnable =
      new Runnable with Named {
        private var _name: Option[String] = None
        override def run(): Unit = {
          Thread.sleep(ThreadLocalRandom.current.nextInt(100))
          _name = Some(Thread.currentThread().getName)
        }
        override def name: Option[String] = _name
      }

    "pass to name with simple" in {
      val pool = new ForkJoinPool(1, NamedForkJoinWorkerThreadFactory("a"), null, false)
      val rs = (0 to 2).map(_ => newRunnable)
      rs.foreach(pool.execute)
      pool.shutdown()
      pool.awaitTermination(1, TimeUnit.SECONDS)
      rs.flatMap(_.name) should be(Seq("a", "a", "a"))
    }
    "pass to name with format" in {
      val pool = new ForkJoinPool(2, NamedForkJoinWorkerThreadFactory("b-%d"), null, false)
      val rs = (0 to 10).map(_ => newRunnable)
      rs.foreach(pool.execute)
      pool.shutdown()
      pool.awaitTermination(1, TimeUnit.SECONDS)
      rs.flatMap(_.name) should contain allOf ("b-0", "b-1")
    }

  }
}
