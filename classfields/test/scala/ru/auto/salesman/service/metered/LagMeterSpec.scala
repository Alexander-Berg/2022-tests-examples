package ru.auto.salesman.service.metered

import ru.auto.salesman.environment.now
import ru.auto.salesman.test.BaseSpec

import scala.concurrent.duration._

class LagMeterSpec extends BaseSpec with LagMeter {

  "lag" should {

    "return 0 minutes for now()" in {
      lag(now()).toMinutes shouldBe 0
    }

    "return zero for time in future" in {
      lag(now().plusDays(1)) shouldBe 0.millis
    }

    "return 1 hour for 1 hour ago" in {
      lag(now().minusHours(1)).toHours shouldBe 1
    }
  }
}
