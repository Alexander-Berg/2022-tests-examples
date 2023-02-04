package ru.yandex.vertis.shark.client.bank.converter.impl.rosgosstrah

import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.shark.model.{MoneyRub, MonthAmount}
import zio.test._
import ru.yandex.vertis.shark.util.RichModel.RichMoneyRub
import ru.yandex.vertis.shark.model.Tag

object LoanAmountRequestFormulaSpec extends DefaultRunnableSpec {

  private val loanAmountRequestFormula: LoanAmountRequestFormula = new LoanAmountRequestFormula {}

  override def spec: ZSpec[environment.TestEnvironment, Any] = {
    suite("LoanAmountRequestFormula")(
      test("calculate valid loan amount request tariff 1") {
        val amount: MoneyRub = 3_000_000L.taggedWith
        val termMonths: MonthAmount = 25.taggedWith
        val upperLimit = 2876250L.taggedWith[Tag.MoneyRub].toCoins
        val ratio = 0.0012
        val tariff = "1"
        val result = loanAmountRequestFormula.calculateLoanAmountRequest(amount, termMonths)
        val insuranceDataAmount = ((upperLimit * 1.1 * ratio * termMonths) / (1 - (1.1 * ratio) * termMonths)).toLong
          .taggedWith[Tag.MoneyRubCoin]
        assert(result)(
          Assertion.equalTo(
            InsuranceAmount(upperLimit, (upperLimit + insuranceDataAmount).taggedWith, insuranceDataAmount, tariff)
          )
        )
      },
      test("calculate valid loan amount request tariff 2") {
        val amount: MoneyRub = 700_000L.taggedWith
        val termMonths: MonthAmount = 25.taggedWith
        val ratio = 0.0015
        val tariff = "2"
        val result = loanAmountRequestFormula.calculateLoanAmountRequest(amount, termMonths)
        val insuranceDataAmount =
          ((amount.toCoins * 1.1 * ratio * termMonths) / (1 - (1.1 * ratio) * termMonths)).toLong
            .taggedWith[Tag.MoneyRubCoin]
        assert(result)(
          Assertion.equalTo(
            InsuranceAmount(
              amount.toCoins,
              (amount.toCoins + insuranceDataAmount).taggedWith,
              insuranceDataAmount,
              tariff
            )
          )
        )
      },
      test("calculate valid loan amount request tariff 3") {
        val amount: MoneyRub = 500_000L.taggedWith
        val termMonths: MonthAmount = 25.taggedWith
        val ratio = 0.002
        val tariff = "3"
        val result = loanAmountRequestFormula.calculateLoanAmountRequest(amount, termMonths)
        val insuranceDataAmount =
          ((amount.toCoins * 1.1 * ratio * termMonths) / (1 - (1.1 * ratio) * termMonths)).toLong
            .taggedWith[Tag.MoneyRubCoin]
        assert(result)(
          Assertion.equalTo(
            InsuranceAmount(
              amount.toCoins,
              (amount.toCoins + insuranceDataAmount).taggedWith,
              insuranceDataAmount,
              tariff
            )
          )
        )
      },
      test("calculate valid loan amount request tariff 6") {
        val amount: MoneyRub = 250_000L.taggedWith
        val termMonths: MonthAmount = 25.taggedWith
        val ratio = 0.0030
        val tariff = "6"
        val result = loanAmountRequestFormula.calculateLoanAmountRequest(amount, termMonths)
        val insuranceDataAmount =
          ((amount.toCoins * 1.1 * ratio * termMonths) / (1 - (1.1 * ratio) * termMonths)).toLong
            .taggedWith[Tag.MoneyRubCoin]
        assert(result)(
          Assertion.equalTo(
            InsuranceAmount(
              amount.toCoins,
              (amount.toCoins + insuranceDataAmount).taggedWith,
              insuranceDataAmount,
              tariff
            )
          )
        )
      }
    )
  }
}
