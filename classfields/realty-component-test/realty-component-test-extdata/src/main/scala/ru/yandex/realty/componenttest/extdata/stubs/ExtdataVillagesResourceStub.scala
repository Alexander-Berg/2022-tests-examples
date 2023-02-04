package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.data.villages.Villages
import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty.proto.village.Village

trait ExtdataVillagesResourceStub extends ExtdataResourceStub {

  private val villages: Seq[Village] = {
    Villages.all
  }

  stubGzipped(RealtyDataType.Villages, villages)

}
