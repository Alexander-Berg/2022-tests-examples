package ru.yandex.realty.componenttest.extdata.stubs

import realty.palma.MortgageCalculatorParamsOuterClass.MortgageCalculatorParams
import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty.storage.MortgageCalculatorParamsStorage.DEFAULT_PARAMS_CODE

trait ExtdataMortgageCalculatorParamsResourceStub extends ExtdataResourceStub {

  private val mortgageCalculatorParams: Seq[MortgageCalculatorParams] = Seq(
    MortgageCalculatorParams
      .newBuilder()
      .setCode(DEFAULT_PARAMS_CODE)
      .setRateMin(6.99)
      .setRateMax(12.1)
      .setRateDefault(10.5)
      .setSumMin(600000.0)
      .setSumMax(12000000.0)
      .setPeriodMin(1)
      .setPeriodMax(20)
      .setPeriodDefault(10)
      .setCostDefault(6000000)
      .setDownpaymentMin(10)
      .setDownpaymentMax(30)
      .setDownpaymentDefault(15)
      .build()
  )

  stubGzipped(RealtyDataType.MortgageCalculatorParams, mortgageCalculatorParams)

}
