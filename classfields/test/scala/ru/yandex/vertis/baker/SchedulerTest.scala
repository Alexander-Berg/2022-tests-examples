package ru.yandex.vertis.baker

import org.junit.runner.RunWith
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.baker.components.ApplicationAware
import ru.yandex.vertis.baker.components.akka.AkkaSupport
import ru.yandex.vertis.baker.components.execution.metrics.ExecutionContextMetricsSupport
import ru.yandex.vertis.baker.components.io.IOSupport
import ru.yandex.vertis.baker.components.monitor.service.MonitorSupport
import ru.yandex.vertis.baker.components.operational.TestOperationalSupport
import ru.yandex.vertis.baker.components.time.TimeSupport
import ru.yandex.vertis.baker.components.tokens.{WorkTokensAware, WorkTokensDistributionSupport}
import ru.yandex.vertis.baker.components.tracing.TracingSupport
import ru.yandex.vertis.baker.components.workdistribution.{WorkDistributionSupport, WorkerToken}
import ru.yandex.vertis.baker.components.workers.{AutoStart, Worker, WorkerImpl}
import ru.yandex.vertis.baker.components.workersfactory.workers.{WorkDone, WorkResult, WorkersExecutionContextSupport}
import ru.yandex.vertis.baker.components.workersfactory.{WorkersFactoryAware, WorkersFactorySupport}
import ru.yandex.vertis.baker.components.zookeeper.ZookeeperSupport
import ru.yandex.vertis.baker.lifecycle.{Application, DefaultApplication}

import java.util.concurrent.atomic.AtomicInteger
import ru.yandex.vertis.baker.components.pool.ForkJoinPoolSupport

import scala.concurrent.duration._

abstract class TestWorker extends Worker {
  val counter = new AtomicInteger()

  override def token: WorkerToken = new WorkerToken {
    override def name: String = "test-worker"
  }

  override protected def doWork(): WorkResult = {
    counter.updateAndGet { prev =>
      if (prev < 5) prev + 1 else prev
    }
    WorkDone(100.millis)
  }
}

trait WorkersSupport extends WorkersFactoryAware with WorkTokensAware with ApplicationAware {

  trait WorkerCompletion
    extends WorkerImpl
    with WorkersFactoryAwareImpl
    with WorkTokensAwareImpl
    with AutoStart
    with ApplicationAwareImpl

  val worker = new TestWorker with WorkerCompletion
}

class SchedulerAppComponents(val app: Application)
  extends TracingSupport
  with IOSupport
  with ForkJoinPoolSupport
  with AkkaSupport
  with ZookeeperSupport
  with TestOperationalSupport
  with TimeSupport
  with ExecutionContextMetricsSupport
  with WorkersExecutionContextSupport
  with WorkersFactorySupport
  with MonitorSupport
  with WorkDistributionSupport
  with WorkTokensDistributionSupport
  with WorkersSupport

class SchedulerTestApp extends DefaultApplication {
  val components = new SchedulerAppComponents(this)
}

@RunWith(classOf[JUnitRunner])
class SchedulerTest extends AnyWordSpec with Matchers with Eventually {
  "Scheduler App" should {
    "increase counter five times" in {
      val app = new SchedulerTestApp
      app.main(Array())
      val components = app.components
      eventually(Timeout(1.second)) {
        components.worker.counter.get() shouldBe 5
      }
    }
  }
}
