package ru.yandex.vos2.autoru.utils.currency

import java.util.Currency

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.OptionValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.yandex.vos2.AutoruModel.AutoruOffer.Price
import ru.yandex.vos2.BasicsModel.{Currency => VosCurrency}
import ru.yandex.vos2.autoru.utils.TestDataEngine

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class CurrencyRatesTest extends AnyFunSuite with Matchers with OptionValues {
  private val currencyRates = CurrencyRates.from(TestDataEngine)

  test("convert usd to rub") {
    val priceInfoBuilder = Price.newBuilder()
    priceInfoBuilder.setCurrency(VosCurrency.USD)
    priceInfoBuilder.setPrice(109900)
    priceInfoBuilder.setPriceRub(0)
    priceInfoBuilder.setCreated(1539950904000L)

    val priceInfo = priceInfoBuilder.build()
    val priceCurrency = Currency.getInstance(priceInfo.getCurrency.name())
    val rubCurrency = Currency.getInstance("RUR")
    val result = currencyRates.convert(BigDecimal(priceInfo.getPrice), priceCurrency, rubCurrency)
    result shouldBe defined
    result.value shouldBe BigDecimal(6411236.3)
  }
}
