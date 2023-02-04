package ru.yandex.vos2.util

import org.scalatest.FunSuite
import ru.yandex.vos2.BasicsModel.Currency

/**
  * Created by andrey on 1/26/17.
  */
class CurrencyUtilsTest extends FunSuite {

  test("testFromCurrency") {
    assert(CurrencyUtils.fromCurrency(Currency.RUB) == "RUR")
    assert(CurrencyUtils.fromCurrency(Currency.USD) == "USD")
    assert(CurrencyUtils.fromCurrency(Currency.EUR) == "EUR")
  }

  test("testFromString") {
    assert(CurrencyUtils.fromString("RUR").contains(Currency.RUB))
    assert(CurrencyUtils.fromString("RUB").contains(Currency.RUB))
    assert(CurrencyUtils.fromString("EUR").contains(Currency.EUR))
    assert(CurrencyUtils.fromString("USD").contains(Currency.USD))
    assert(CurrencyUtils.fromString("xxx").isEmpty)
    assert(CurrencyUtils.fromString("").isEmpty)
  }

}
