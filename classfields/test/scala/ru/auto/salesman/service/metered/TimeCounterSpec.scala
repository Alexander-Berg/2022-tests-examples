package ru.auto.salesman.service.metered

import nl.grons.metrics4.scala.Counter
import ru.auto.salesman.test.BaseSpec

class TimeCounterSpec extends BaseSpec {

  "TimeCounter" should {

    "time" in {
      val counter =
        new TimeCounter(new Counter(new com.codahale.metrics.Counter))
      counter.time(Thread.sleep(100))
      counter.counter.count should be >= 100L
      // big number to avoid some random flaps
      counter.counter.count should be < 1000L
    }
  }
}
