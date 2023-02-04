package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.data.campaigns.Campaigns
import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty.model.message.ExtDataSchema.CampaignMessage

trait ExtdataCampaignResourceStub extends ExtdataResourceStub {

  private val campaigns: Seq[CampaignMessage] = {
    Campaigns.all
  }

  stubGzipped(RealtyDataType.Campaign, campaigns)

}
