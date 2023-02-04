package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.data.villages.VillageDynamicInfos
import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty.model.message.VillageDynamicInfo

trait ExtdataVillageDynamicInfoResourceStub extends ExtdataResourceStub {

  private val villageDynamicInfos: Seq[VillageDynamicInfo] = {
    VillageDynamicInfos.all
  }

  stubGzipped(RealtyDataType.VillageDynamicInfo, villageDynamicInfos)

}
