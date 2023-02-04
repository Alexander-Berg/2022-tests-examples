package ru.yandex.vertis.telepony.util.future

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{ActorSystem, Scheduler}
import com.typesafe.config.ConfigFactory
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.util.TestComponent

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

/**
  * @author neron
  */
class RetryUtilsSpec extends SpecBase with TestComponent {

  import scala.concurrent.ExecutionContext.Implicits.global
  private lazy val config = ConfigFactory.parseResources("service.conf").resolve()
  lazy val actorSystem = ActorSystem(component.name, config)
  implicit val scheduler: Scheduler = actorSystem.scheduler

  "retry should fail when no more attempts" in {
    val counter = new AtomicInteger()
    val ex = new IllegalArgumentException("less than 5")
    val f = RetryUtils.retry(3) {
      Future { if (counter.incrementAndGet() < 5) throw ex else counter.get() }
    }
    f.failed.futureValue === ex
  }

  "retry should work" in {
    val counter = new AtomicInteger()
    val ex = new IllegalArgumentException("less than 5")
    val f = RetryUtils.retry(4) {
      Future { if (counter.incrementAndGet() < 5) throw ex else counter.get() }
    }
    f.futureValue === 5
  }

  "backoff retry should increase wait time" in {
    val ex = new IllegalArgumentException("error")
    val t0 = System.currentTimeMillis()
    RetryUtils
      .backoffRetry(10.millis, 4) { // 10ms, 20ms, 40ms, 80ms (sum = 150ms)
        Future.failed(ex)
      }
      .failed
      .futureValue
    val t1 = System.currentTimeMillis()
    (t1 - t0) should be >= 150L
  }

  "backoff retry should work" in {
    val counter = new AtomicInteger()
    val ex = new IllegalArgumentException("less than 5")
    val f = RetryUtils.backoffRetry(1.milli, 4) {
      Future { if (counter.incrementAndGet() < 5) throw ex else counter.get() }
    }
    f.futureValue === 5
  }

}
