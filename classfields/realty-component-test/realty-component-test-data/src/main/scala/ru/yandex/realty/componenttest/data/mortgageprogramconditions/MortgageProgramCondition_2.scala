package ru.yandex.realty.componenttest.data.mortgageprogramconditions

import com.google.protobuf.{FloatValue, Int64Value, StringValue}
import realty.palma.MortgageProgramConditionOuterClass.IfConditionOperatorTypeNamespace.IfConditionOperatorType
import realty.palma.MortgageProgramConditionOuterClass.MortgageParamTypeNamespace.MortgageParamType
import realty.palma.MortgageProgramConditionOuterClass.ThenActionTypeNamespace.ThenActionType
import realty.palma.MortgageProgramConditionOuterClass.{
  IfCondition,
  MortgageParamValue,
  MortgageProgramCondition,
  ThenAction
}
import ru.yandex.realty.componenttest.data.mortgageprogramfactors.MortgageProgramFactor_1
import ru.yandex.realty.componenttest.data.mortgageprograms.MortgageProgram_2419311
import ru.yandex.realty.componenttest.data.utils.ComponentTestDataUtils.extractIdFromClassName

object MortgageProgramCondition_2 {

  val Id: Long = extractIdFromClassName(getClass)

  val Proto: MortgageProgramCondition =
    MortgageProgramCondition
      .newBuilder()
      .setId(Id.toString)
      .setMortgageProgramId(MortgageProgram_2419311.Id.toString)
      .addConditions(
        IfCondition
          .newBuilder()
          .setParam(MortgageParamType.FACTOR)
          .setOperator(IfConditionOperatorType.EQUAL)
          .addValues(
            MortgageParamValue
              .newBuilder()
              .setStringValue(StringValue.of(MortgageProgramFactor_1.Code))
              .build()
          )
          .build()
      )
      .addConditions(
        IfCondition
          .newBuilder()
          .setParam(MortgageParamType.CREDIT_SUM)
          .setOperator(IfConditionOperatorType.LESS)
          .addValues(
            MortgageParamValue
              .newBuilder()
              .setIntValue(Int64Value.of(10000000L))
              .build()
          )
          .build()
      )
      .addActions(
        ThenAction
          .newBuilder()
          .setParam(MortgageParamType.RATE)
          .setOperator(ThenActionType.DECREASE_BY)
          .setValue(
            MortgageParamValue
              .newBuilder()
              .setFloatValue(FloatValue.of(0.3f))
              .build()
          )
          .build()
      )
      .build()

}
