package ru.yandex.realty.mortgages

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.componenttest.data.companies.Company_56576
import ru.yandex.realty.componenttest.data.mortgageprograms.{MortgageProgram_2418785, MortgageProgram_2419311}
import ru.yandex.realty.componenttest.data.sites.Site_57547
import ru.yandex.realty.mortgages.mortgage.domain.{
  MortgageFactorStatuses,
  MortgageProgramPaymentCalculationInput,
  MortgageProgramPaymentParams
}

@RunWith(classOf[JUnitRunner])
class MortgageProgramPaymentParamsTransformerSpec extends AbstractMortgagesComponentTestSpec {

  private val transformer = component.mortgageProgramPaymentParamsTransformer

  "MortgageProgramPaymentParamsTransformer" should {

    "not transform params when all conditions were not matched" in {
      // given
      val input = MortgageProgramPaymentCalculationInput(
        mortgageProgram = MortgageProgram_2418785.Proto
      )
      val initialPaymentParams = MortgageProgramPaymentParams(
        propertyCost = 10000000,
        downPaymentSum = 1000000,
        creditAmount = 9000000,
        rate = 4.5f,
        periodYears = 15
      )

      // when
      val actualPaymentParams = transformer.transformParams(input, initialPaymentParams)

      // then
      actualPaymentParams should be(initialPaymentParams)
    }

    "transform params according to matched conditions" in {
      // given
      val input = MortgageProgramPaymentCalculationInput(
        mortgageProgram = MortgageProgram_2419311.Proto,
        siteId = Some(Site_57547.Id),
        developerId = Some(Company_56576.Id),
        factors = MortgageFactorStatuses(
          statuses = Map(
            "factor_down_payment_less_20pts" -> true
          )
        )
      )
      val initialPaymentParams = MortgageProgramPaymentParams(
        propertyCost = 10000000,
        downPaymentSum = 1000000,
        creditAmount = 9000000,
        rate = 4.5f,
        periodYears = 15
      )

      // when
      val actualPaymentParams = transformer.transformParams(input, initialPaymentParams)

      // then
      actualPaymentParams.rate should be(4.1f)
    }

  }

}
