package ru.yandex.realty.componenttest.data.mortgageprogramconditions

import com.google.protobuf.{FloatValue, Int64Value}
import realty.palma.MortgageProgramConditionOuterClass.IfConditionOperatorTypeNamespace.IfConditionOperatorType
import realty.palma.MortgageProgramConditionOuterClass.MortgageParamTypeNamespace.MortgageParamType
import realty.palma.MortgageProgramConditionOuterClass.ThenActionTypeNamespace.ThenActionType
import realty.palma.MortgageProgramConditionOuterClass.{
  IfCondition,
  MortgageParamValue,
  MortgageProgramCondition,
  ThenAction
}
import ru.yandex.realty.componenttest.data.companies.Company_56576
import ru.yandex.realty.componenttest.data.mortgageprogramconditions.MortgageProgramCondition_2.Id
import ru.yandex.realty.componenttest.data.mortgageprograms.MortgageProgram_2419311
import ru.yandex.realty.componenttest.data.sites.Site_57547
import ru.yandex.realty.componenttest.data.utils.ComponentTestDataUtils.extractIdFromClassName

object MortgageProgramCondition_1 {

  val Id: Long = extractIdFromClassName(getClass)

  val Proto: MortgageProgramCondition =
    MortgageProgramCondition
      .newBuilder()
      .setId(Id.toString)
      .setMortgageProgramId(MortgageProgram_2419311.Id.toString)
      .addConditions(
        IfCondition
          .newBuilder()
          .setParam(MortgageParamType.SITE_ID)
          .setOperator(IfConditionOperatorType.EQUAL)
          .addValues(
            MortgageParamValue
              .newBuilder()
              .setIntValue(Int64Value.of(Site_57547.Id))
              .build()
          )
          .build()
      )
      .addConditions(
        IfCondition
          .newBuilder()
          .setParam(MortgageParamType.DEVELOPER_ID)
          .setOperator(IfConditionOperatorType.EQUAL)
          .addValues(
            MortgageParamValue
              .newBuilder()
              .setIntValue(Int64Value.of(Company_56576.Id))
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
              .setFloatValue(FloatValue.of(0.1f))
              .build()
          )
          .build()
      )
      .build()

}
