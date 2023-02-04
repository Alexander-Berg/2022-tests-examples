package ru.auto.salesman.service.application_credit

import org.joda.time.DateTime
import ru.auto.salesman
import ru.auto.salesman.test.BaseSpec
import zio.ZIO

class ZioTestUtilSpec extends BaseSpec {

  import ZioTestUtil._

  "ZioTestUtil.runTestWithFixedTime" should {

    "run test" in {
      var testExecuted = false
      val test = ZIO.effect {
        1 shouldBe 1
        testExecuted = true
      }

      testExecuted shouldBe false
      runTestWithFixedTime(DateTime.now)(test)
      testExecuted shouldBe true
    }

    "fix time" in {
      val fixedTime = DateTime.now
      val testableCode =
        salesman.clock.now.flatMap(now => ZIO.effectTotal(now))

      runTestWithFixedTime(fixedTime) {
        testableCode.map { result =>
          result shouldBe fixedTime
        }
      }
    }

  }

}
