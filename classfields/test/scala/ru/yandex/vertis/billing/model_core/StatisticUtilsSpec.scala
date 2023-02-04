package ru.yandex.vertis.billing.model_core

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.StatisticUtils._

/**
  * Specs on [[StatisticUtils]]
  *
  * @author alesavin
  */
class StatisticUtilsSpec extends AnyWordSpec with Matchers {

  "StatisticUtils" should {
    "return rounded value of  average" in {
      average(10812L, 6042L) should be(2L)
      average(10812L, 9000L) should be(1L)
      average(10812L, 3000L) should be(4L)
      average(10812L, 0L) should be(0L)
    }
  }
}
