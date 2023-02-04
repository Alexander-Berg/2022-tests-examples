package ru.yandex.util.xml.breaker

import org.scalatest.{Matchers, WordSpec}
import ru.yandex.common.monitoring.error.{ErrorReservoir, ExpiringWarningErrorCounterReservoir}
import ru.yandex.vos2.util.breaker.{CircuitBreaker, CircuitOpenedException}

import scala.concurrent.duration._
import scala.util._

/**
  * @author Ilya Gerasimov (747mmhg@yandex-team.ru)
  */
class CircuitBreakerSpec extends WordSpec with Matchers {

  "Breaker" should {
    "pass everything while error rate is acceptable" in {
      val breaker = new CircuitBreaker {
        override def createClosedModeReservoir(): ErrorReservoir =
          new ExpiringWarningErrorCounterReservoir(10, 100, 5.minutes, 1000)

        override def createOpenModeReservoir(): ErrorReservoir =
          new ExpiringWarningErrorCounterReservoir(1, 10, 5.minutes, 100)
      }
      val client = new WorkingClient
      val results = (1 to 100).map(_ ⇒ breaker.call(client.call()))
      assert(results.forall(_.isSuccess))
      assert(client.callCount == 100)
    }
  }

  "Breaker" should {
    "switch to open mode when error rate is unacceptable" in {
      val breaker = new CircuitBreaker {
        override def createClosedModeReservoir(): ErrorReservoir =
          new ExpiringWarningErrorCounterReservoir(10, 100, 5.minutes, 1000)

        override def createOpenModeReservoir(): ErrorReservoir =
          new ExpiringWarningErrorCounterReservoir(1, 10, 5.minutes, 100)
      }
      val client = new LimitedFailingClient(11)
      val results = (1 to 100).map(_ ⇒ breaker.call(client.call()))
      val (close, open) = results.splitAt(11)
      assert(close.forall(_.isFailure))
      assert(open.exists(_.isSuccess))
      assert(open.filter(_.isFailure).forall(_.failed.get.isInstanceOf[CircuitOpenedException]))
      assert(client.callCount < 100)
    }
  }

  "Breaker" should {
    "switch back to closed mode when error rate becomes acceptable again" in {
      val breaker = new CircuitBreaker {
        override def createClosedModeReservoir(): ErrorReservoir =
          new ExpiringWarningErrorCounterReservoir(10, 100, 5.minutes, 1000)

        override def createOpenModeReservoir(): ErrorReservoir =
          new ExpiringWarningErrorCounterReservoir(1, 5, 5.minutes, 10)
      }
      val client = new LimitedFailingClient(11)
      val results = (1 to 500).map(_ ⇒ breaker.call(client.call()))
      assert(client.callCount < 500)
      val (close, open) = results.splitAt(11)
      assert(close.forall(_.isFailure))
      assert(open.filter(_.isFailure).forall(_.failed.get.isInstanceOf[CircuitOpenedException]))
      val workingClient = new WorkingClient
      val okResults = (1 to 100).map(_ ⇒ breaker.call(workingClient.call()))
      assert(okResults.forall(_.isSuccess))
      assert(workingClient.callCount == 100)
    }
  }

  "Breaker" should {
    "not flap on switch back to closed mode" in {
      val breaker = new CircuitBreaker {
        override def createClosedModeReservoir(): ErrorReservoir =
          new ExpiringWarningErrorCounterReservoir(10, 100, 5.minutes, 1000)

        override def createOpenModeReservoir(): ErrorReservoir =
          new ExpiringWarningErrorCounterReservoir(1, 5, 5.minutes, 10)
      }
      val client = new LimitedFailingClient(11)
      val results = (1 to 500).map(_ ⇒ breaker.call(client.call()))
      assert(client.callCount < 500)
      val (close, open) = results.splitAt(11)
      assert(close.forall(_.isFailure))
      assert(open.filter(_.isFailure).forall(_.failed.get.isInstanceOf[CircuitOpenedException]))
      val workingClient = new LimitedFailingClient(10)
      val (failed, succeeded) = (1 to 100).map(_ ⇒ breaker.call(workingClient.call())).splitAt(10)
      assert(failed.forall(_.isFailure))
      assert(failed.forall(_.failed.get.isInstanceOf[IllegalArgumentException]))
      assert(succeeded.forall(_.isSuccess))
      assert(workingClient.callCount == 100)
    }
  }

  class FailingClient {

    var callCount = 0

    def call(): Try[Unit] = {
      callCount += 1
      Failure(new IllegalArgumentException)
    }
  }

  class WorkingClient {

    var callCount = 0

    def call(): Try[Unit] = {
      callCount += 1
      Success(())
    }
  }

  class LimitedFailingClient(val failureNum: Int) {

    var callCount = 0

    def call(): Try[Unit] = {
      callCount += 1
      if (callCount <= failureNum) {
        Failure(new IllegalArgumentException("You shall not pass!"))
      } else {
        Success(())
      }
    }
  }
}
