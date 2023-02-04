package ru.yandex.vertis.telepony.service.impl

import ru.yandex.vertis.telepony.api.SpecBase
import ru.yandex.vertis.telepony.dummy.InMemoryHydraClient
import ru.yandex.vertis.telepony.model.{Block, Pass, Phone, RefinedSource}
import ru.yandex.vertis.telepony.service.limiter.impl.HydraCallLimiter
import ru.yandex.vertis.telepony.service.logging.LoggingCallLimiter

/**
  * @author evans
  */
class HydraCallLimiterSpec extends SpecBase {

  val src = RefinedSource.from("1")
  val target = Phone("+79312320032")
  val target2 = Phone("+79312320033")

  "Call limiter" should {
    "allow new call" in {
      val client = new InMemoryHydraClient
      val callLimiter = new HydraCallLimiter(client, 10) with LoggingCallLimiter
      callLimiter.tryAcquire(src, target).futureValue shouldEqual Pass
      client.clicker(HydraCallLimiter.Ban).get(src.callerId.value).futureValue shouldEqual 0
    }
    "deny call for banned source" in {
      val client = new InMemoryHydraClient
      client.clicker(HydraCallLimiter.Ban).incrementAndGet(src.callerId.value).futureValue
      val callLimiter = new HydraCallLimiter(client, 10) with LoggingCallLimiter
      callLimiter.tryAcquire(src, target).futureValue shouldEqual Block
      client.clicker(HydraCallLimiter.Ban).get(src.callerId.value).futureValue shouldEqual 2
    }
    "deny call for many targets" in {
      val client = new InMemoryHydraClient
      val callLimiter = new HydraCallLimiter(client, 1) with LoggingCallLimiter
      callLimiter.tryAcquire(src, target).futureValue shouldEqual Pass
      callLimiter.tryAcquire(src, target2).futureValue shouldEqual Block
      client.clicker(HydraCallLimiter.Ban).get(src.callerId.value).futureValue shouldEqual 1
    }
    "allow multiple calls for one target" in {
      val client = new InMemoryHydraClient
      val callLimiter = new HydraCallLimiter(client, 1) with LoggingCallLimiter
      callLimiter.tryAcquire(src, target).futureValue shouldEqual Pass
      callLimiter.tryAcquire(src, target).futureValue shouldEqual Pass
      client.clicker(HydraCallLimiter.Ban).get(src.callerId.value).futureValue shouldEqual 0
    }
  }
}
