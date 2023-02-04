package ru.yandex.vertis.telepony.hydra.impl

import org.scalatest.time.{Milliseconds, Seconds, Span}
import ru.yandex.vertis.telepony.hydra.HydraClient.{Clicker, Counter}
import ru.yandex.vertis.telepony.service.impl.OperatorAvailableServiceImpl
import ru.yandex.vertis.telepony.service.limiter.impl.HydraCallLimiter
import ru.yandex.vertis.telepony.{IntegrationSpecTemplate, SpecBase}

import scala.util.Random

/**
  * @author @logab
  */
class HydraClientImplIntSpec extends SpecBase with IntegrationSpecTemplate {

  implicit override def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(3, Seconds), interval = Span(300, Milliseconds))

  private def random: Long = Random.nextLong()

  "hydra client impl" should {
    "clicker incrementAndGet not fail key with whitespace" in {
      val clicker: Clicker = hydraClient.clicker(OperatorAvailableServiceImpl.Component)
      val key = s"test $random"
      clicker.incrementAndGet(key).futureValue
    }
    "clicker incrementAndGet key with whitespace" ignore {
      val clicker: Clicker = hydraClient.clicker(OperatorAvailableServiceImpl.Component)
      val key = s"test $random"
      clicker.incrementAndGet(key).futureValue
      eventually {
        clicker.get(key).futureValue shouldEqual 1
      }
    }
    "clicker incrementAndGet" in {
      val clicker: Clicker = hydraClient.clicker(OperatorAvailableServiceImpl.Component)
      val key = s"test-$random"
      clicker.incrementAndGet(key).futureValue
      eventually {
        clicker.get(key).futureValue shouldEqual 1
      }
    }
    Seq(
      (s"test-key $random", s"test-value $random "),
      (s"test-key-$random", s"test-value-$random ")
    ).foreach {
      case (key, value) =>
        s"counter incrementAndGet key='$key' and value='$value'" in {
          val counter: Counter = hydraClient.counter(HydraCallLimiter.Target)
          counter.incrementAndGet(key, value).futureValue
          eventually {
            counter.get(key).futureValue shouldEqual 1
          }
        }
    }

  }
}
