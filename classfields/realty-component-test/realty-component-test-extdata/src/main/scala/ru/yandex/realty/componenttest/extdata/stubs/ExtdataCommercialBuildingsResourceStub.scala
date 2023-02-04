package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty.model.message.{BusinessCenterInfo, CommercialBuilding, CommercialBuildingName}
import ru.yandex.realty.proto.commercial.api.BusinessCenter
import ru.yandex.realty.proto.offer.CommercialBuildingType

trait ExtdataCommercialBuildingsResourceStub extends ExtdataResourceStub {

  private val commercialBuildings: Seq[CommercialBuilding] = Seq(
    CommercialBuilding
      .newBuilder()
      .setId(2253708)
      .setName(
        CommercialBuildingName
          .newBuilder()
          .setName("1 Zhukov")
          .setFullName("Бизнес-центр «1 Zhukov»")
          .build()
      )
      .setBuildingType(CommercialBuildingType.COMMERCIAL_BUILDING_BUSINESS_CENTER)
      .setBusinessCenterInfo(
        BusinessCenterInfo
          .newBuilder()
          .setGrade(BusinessCenter.Grade.GRADE_A_PLUS)
          .build()
      )
      .build()
  )

  stubGzipped(RealtyDataType.CommercialBuildings, commercialBuildings)

}
