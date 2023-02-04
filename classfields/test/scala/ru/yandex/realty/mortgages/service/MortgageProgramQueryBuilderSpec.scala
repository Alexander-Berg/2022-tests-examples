package ru.yandex.realty.mortgages.service

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.main.ComponentsTest
import ru.yandex.realty.mortgages.handlers.mortgage.MortgageProgramSearchUserInput
import ru.yandex.realty.mortgages.services.MortgageCalculatorParamsProvider
import ru.yandex.realty.search.mortgage.MortgageProgramSearchQuery

@RunWith(classOf[JUnitRunner])
class MortgageProgramQueryBuilderSpec extends SpecBase with ComponentsTest {

  private val mortgageCalculatorParamsProvider = mock[MortgageCalculatorParamsProvider]
  private val mortgageProgramQueryBuilder = new MortgageProgramQueryBuilder(
    mortgageCalculatorParamsProvider,
    regionGraphProvider
  )

  "mortgageProgramQueryBuilder " should {
    " mix regionId and subject Federation from geoid in built searchQuery " in {
      val input = MortgageProgramSearchUserInput(
        bankRegionId = Seq(1),
        rgid = Seq(582357),
        propertyCost = Some(1000000),
        downPaymentSum = Some(500000),
        periodYears = Some(10)
      )
      val expectedSearchQuery = MortgageProgramSearchQuery(
        bankRegionId = Seq(1, 11119),
        propertyCost = Some(1000000),
        downPaymentSum = Some(500000),
        downPaymentPercents = Some(50f),
        periodYears = Some(10)
      )

      val actualSearchQuery: MortgageProgramSearchQuery = mortgageProgramQueryBuilder.buildQuery(input)
      actualSearchQuery shouldBe expectedSearchQuery
    }
  }
}
