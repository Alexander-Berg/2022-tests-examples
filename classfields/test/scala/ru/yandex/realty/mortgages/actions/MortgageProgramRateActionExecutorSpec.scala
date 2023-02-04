package ru.yandex.realty.mortgages.actions

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatest.TryValues._
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import realty.palma.MortgageProgramConditionOuterClass.MortgageParamTypeNamespace.MortgageParamType
import realty.palma.MortgageProgramConditionOuterClass.ThenActionTypeNamespace.ThenActionType
import realty.palma.MortgageProgramConditionOuterClass.ThenActionTypeNamespace.ThenActionType.{
  ASSIGN_TO,
  DECREASE_BY,
  DIVIDE_BY,
  INCREASE_BY,
  MULTIPLY_BY,
  UNKNOWN
}
import realty.palma.MortgageProgramConditionOuterClass.{MortgageParamValue, ThenAction}
import ru.yandex.extdata.core.gens.Producer._
import ru.yandex.realty.mortgages.MortgageProgramSpecUtils.{
  asMortgageParamValue,
  asMortgageProgramContext,
  mortgageProgramParamsGen
}
import ru.yandex.realty.mortgages.util.MortgageCalculationUtils.roundRate
import ru.yandex.realty.mortgages.mortgage.actions.MortgageProgramRateActionExecutor

@RunWith(classOf[JUnitRunner])
class MortgageProgramRateActionExecutorSpec extends WordSpec with MockFactory with Matchers {

  import MortgageProgramRateActionExecutorSpec._

  "MortgageProgramRateActionExecutor" should {

    s"assign rate to given value when action is $ASSIGN_TO" in {
      val initialRateValue = 10.0f
      val actionValue = 12.0f
      val action = buildAction(ASSIGN_TO, asMortgageParamValue(actionValue))
      val paymentParams = mortgageProgramParamsGen(rate = Some(initialRateValue)).next
      val context = asMortgageProgramContext(paymentParams = paymentParams)

      val result = MortgageProgramRateActionExecutor.executeOn(context, action)

      result.success.value.paymentParams should be(paymentParams.copy(rate = actionValue))
    }

    s"increase rate by given value when action is $INCREASE_BY" in {
      val initialRateValue = 10.0f
      val actionValue = 2
      val action = buildAction(INCREASE_BY, asMortgageParamValue(actionValue))
      val paymentParams = mortgageProgramParamsGen(rate = Some(initialRateValue)).next
      val context = asMortgageProgramContext(paymentParams = paymentParams)

      val result = MortgageProgramRateActionExecutor.executeOn(context, action)

      result.success.value.paymentParams should be(paymentParams.copy(rate = initialRateValue + actionValue))
    }

    s"decrease rate by given value when action is $DECREASE_BY" in {
      val initialRateValue = 10.0f
      val actionValue = 2.1f
      val action = buildAction(DECREASE_BY, asMortgageParamValue(actionValue))
      val paymentParams = mortgageProgramParamsGen(rate = Some(initialRateValue)).next
      val context = asMortgageProgramContext(paymentParams = paymentParams)

      val result = MortgageProgramRateActionExecutor.executeOn(context, action)

      result.success.value.paymentParams should be(paymentParams.copy(rate = initialRateValue - actionValue))
    }

    s"multiply rate by given value when action is $MULTIPLY_BY" in {
      val initialRateValue = 10.0f
      val actionValue = 2
      val action = buildAction(MULTIPLY_BY, asMortgageParamValue(actionValue))
      val paymentParams = mortgageProgramParamsGen(rate = Some(initialRateValue)).next
      val context = asMortgageProgramContext(paymentParams = paymentParams)

      val result = MortgageProgramRateActionExecutor.executeOn(context, action)

      result.success.value.paymentParams should be(paymentParams.copy(rate = initialRateValue * actionValue))
    }

    s"divide rate by given value when action is $DIVIDE_BY" in {
      val initialRateValue = 10.0f
      val actionValue = 3
      val action = buildAction(DIVIDE_BY, asMortgageParamValue(actionValue))
      val paymentParams = mortgageProgramParamsGen(rate = Some(initialRateValue)).next
      val context = asMortgageProgramContext(paymentParams = paymentParams)

      val result = MortgageProgramRateActionExecutor.executeOn(context, action)

      result.success.value.paymentParams should be(paymentParams.copy(rate = roundRate(initialRateValue / actionValue)))
    }

    s"throw exception for unexpected action" in {
      val initialRateValue = 10.0f
      val actionValue = 3
      val action = buildAction(UNKNOWN, asMortgageParamValue(actionValue))
      val paymentParams = mortgageProgramParamsGen(rate = Some(initialRateValue)).next
      val context = asMortgageProgramContext(paymentParams = paymentParams)

      val result = MortgageProgramRateActionExecutor.executeOn(context, action)

      result.failure.exception.getClass should be(classOf[UnsupportedOperationException])
    }

  }

  private case class TestCase(
    changeDescription: String,
    initialValue: Float,
    actionType: ThenActionType,
    actionValue: MortgageParamValue,
    expectedValue: Float
  )

}

object MortgageProgramRateActionExecutorSpec {

  def buildAction(actionType: ThenActionType, value: MortgageParamValue): ThenAction = {
    ThenAction
      .newBuilder()
      .setParam(MortgageParamType.RATE)
      .setOperator(actionType)
      .setValue(value)
      .build()
  }

}
