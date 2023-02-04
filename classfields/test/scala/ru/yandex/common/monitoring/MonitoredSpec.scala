package ru.yandex.common.monitoring

import com.codahale.metrics.health.HealthCheck.Result
import org.junit.runner.RunWith
import org.scalatest.concurrent._
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}
import ru.yandex.common.monitoring.error.ErrorReservoir
import ru.yandex.common.monitoring.healthchecks.{containMessage, beHealthy, beUnhealthy}

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}


/**
 * @author evans
 */
@RunWith(classOf[JUnitRunner])
class MonitoredSpec
  extends WordSpec
  with Matchers
  with Monitored
  with BeforeAndAfter
  with ScalaFutures {

  import scala.concurrent.ExecutionContext.Implicits.global

  var reservoir: ErrorReservoirProbe = _

  class ErrorReservoirProbe extends ErrorReservoir {
    private val promise = Promise[Result]()

    override def error(): Unit = promise.success(Result.unhealthy(""))

    override def error(msg: String): Unit = promise.success(Result.unhealthy(msg))

    override def ok(): Unit = promise.success(Result.healthy())

    override def toResult = null

    def value = promise.future
  }

  before {
    reservoir = new ErrorReservoirProbe
  }

  "Metered call only once by-name parameter " should {
    "for future" in {
      val wr = new CountedCallValue(Future.successful(1))
      monitoredFuture(reservoir)(wr.get)
      reservoir.value.futureValue should beHealthy
      wr.count shouldBe 1
    }

    "for success try" in {
      val wr = new CountedCallValue(Success(1))
      monitoredTry(reservoir)(wr.get)
      reservoir.value.futureValue should beHealthy
      wr.count shouldBe 1
    }

    "for failure try" in {
      val wr = new CountedCallValue(Failure(new IllegalArgumentException("a")))
      monitoredTry(reservoir)(wr.get)
      reservoir.value.futureValue should (beUnhealthy and containMessage("a"))
      wr.count shouldBe 1
    }

    "for simple value" in {
      val wr = new CountedCallValue(1)
      monitored(reservoir)(wr.get)
      reservoir.value.futureValue should beHealthy
      wr.count shouldBe 1
    }
  }

}

