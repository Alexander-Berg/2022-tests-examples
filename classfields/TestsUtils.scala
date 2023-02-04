package ru.yandex.realty.rent.util

import ru.yandex.realty.deployment.Environment
import ru.yandex.realty.rent.proto.model.contract.Insurance

object TestsUtils {

  val CorrectInsuranceAmount =
    List(51100L, 81100L, 111100L, 141100L, 171100L, 201100L, 231100L, 261100L, 291100L, 321100L)

  def isCorrectInsuranceData(insurance: Insurance): Boolean =
    isCorrectInsuranceDataForTesting(insurance) || Environment.isProduction

  private def isCorrectInsuranceDataForTesting(insurance: Insurance) =
    !Environment.isProduction && isCorrectInsuranceAmount(insurance)

  private def isCorrectInsuranceAmount(insurance: Insurance): Boolean =
    CorrectInsuranceAmount.contains(insurance.getInsuranceAmount)
}
