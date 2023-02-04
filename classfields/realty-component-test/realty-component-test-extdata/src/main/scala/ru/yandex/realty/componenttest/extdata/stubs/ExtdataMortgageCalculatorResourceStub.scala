package ru.yandex.realty.componenttest.extdata.stubs

import realty.palma.Mortgage.MortgageCalculator
import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType

trait ExtdataMortgageCalculatorResourceStub extends ExtdataResourceStub {

  private val mortgageCalculator: Seq[MortgageCalculator] = {
    Seq(
      MortgageCalculator
        .newBuilder()
        .build()
    )
  }

  stubGzipped(RealtyDataType.MortgageCalculator, mortgageCalculator)

}
