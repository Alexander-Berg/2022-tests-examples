package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.data.villages.VillageCampaigns
import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty.model.message.VillageCampaign

trait ExtdataVillageCampaignsResourceStub extends ExtdataResourceStub {

  private val villageCampaigns: Seq[VillageCampaign] = {
    VillageCampaigns.all
  }

  stubGzipped(RealtyDataType.VillageCampaigns, villageCampaigns)

}
