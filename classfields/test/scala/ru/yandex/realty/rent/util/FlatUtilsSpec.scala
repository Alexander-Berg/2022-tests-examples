package ru.yandex.realty.rent.util

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.rent.util.FlatUtils.calculateFlatsAdValue

@RunWith(classOf[JUnitRunner])
class FlatUtilsSpec extends FlatSpec {
  val CommissionValue = 0.05f

  "calculateFlatsAdValue" should "return correct value" in {
    assertResult(3200000)(calculateFlatsAdValue(3000000, CommissionValue))
    assertResult(6950000)(calculateFlatsAdValue(6500000, CommissionValue))
    assertResult(10650000)(calculateFlatsAdValue(10000000, CommissionValue))
    assertResult(15400000)(calculateFlatsAdValue(14500000, CommissionValue))
    assertResult(18050000)(calculateFlatsAdValue(17000000, CommissionValue))
    assertResult(21750000)(calculateFlatsAdValue(20500000, CommissionValue))
    assertResult(21750000)(calculateFlatsAdValue(20500000, CommissionValue))
    assertResult(25500000)(calculateFlatsAdValue(24000000, CommissionValue))
    assertResult(21750000)(calculateFlatsAdValue(20500000, CommissionValue))
    assertResult(31850000)(calculateFlatsAdValue(30000000, CommissionValue))
  }
}
