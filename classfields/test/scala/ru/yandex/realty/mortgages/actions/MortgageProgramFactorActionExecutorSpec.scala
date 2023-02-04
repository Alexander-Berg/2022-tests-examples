package ru.yandex.realty.mortgages.actions

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatest.TryValues._
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import realty.palma.MortgageProgramConditionOuterClass.MortgageParamTypeNamespace.MortgageParamType
import realty.palma.MortgageProgramConditionOuterClass.ThenActionTypeNamespace.ThenActionType
import realty.palma.MortgageProgramConditionOuterClass.ThenActionTypeNamespace.ThenActionType.{DISABLE, UNKNOWN}
import realty.palma.MortgageProgramConditionOuterClass.{MortgageParamValue, ThenAction}
import ru.yandex.realty.mortgages.MortgageProgramSpecUtils.{asMortgageParamValue, asMortgageProgramContext}
import ru.yandex.realty.mortgages.mortgage.domain.MortgageFactorStatuses
import ru.yandex.realty.mortgages.mortgage.actions.MortgageProgramFactorActionExecutor

@RunWith(classOf[JUnitRunner])
class MortgageProgramFactorActionExecutorSpec extends WordSpec with MockFactory with Matchers {

  import MortgageProgramFactorActionExecutorSpec._

  "MortgageProgramFactorActionExecutor" should {

    s"disable factor in context when action is $DISABLE" in {
      val factorCode = "factor_to_be_disabled"
      val factors = MortgageFactorStatuses(Map(factorCode -> true))
      val context = asMortgageProgramContext(factors = factors)
      val action = buildAction(DISABLE, asMortgageParamValue(factorCode))

      val result = MortgageProgramFactorActionExecutor.executeOn(context, action)

      result.success.value.factors.status(factorCode) should be(false)
    }

    s"throw exception for unsupported action" in {
      val factorCode = "factor_to_be_disabled"
      val action = buildAction(UNKNOWN, asMortgageParamValue(factorCode))
      val context = asMortgageProgramContext()

      val result = MortgageProgramFactorActionExecutor.executeOn(context, action)

      result.failure.exception.getClass should be(classOf[UnsupportedOperationException])
    }

  }

}

object MortgageProgramFactorActionExecutorSpec {

  def buildAction(actionType: ThenActionType, value: MortgageParamValue): ThenAction = {
    ThenAction
      .newBuilder()
      .setParam(MortgageParamType.RATE)
      .setOperator(actionType)
      .setValue(value)
      .build()
  }

}
