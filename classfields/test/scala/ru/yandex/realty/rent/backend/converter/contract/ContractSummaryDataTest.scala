package ru.yandex.realty.rent.backend.converter.contract

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.rent.backend.converter.contract.ContractSummaryData.formatMoney

@RunWith(classOf[JUnitRunner])
class ContractSummaryDataTest extends FunSuite {
  test("formatMoney") {
    assert(formatMoney(5050050) == "50 501 ₽/мес.")
  }
}
