package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty.model.message.ExtDataSchema.DeveloperWithChat

trait ExtdataDeveloperWithChatResourceStub extends ExtdataResourceStub {

  private val developerWithChat: Seq[DeveloperWithChat] = Seq(
    DeveloperWithChat
      .newBuilder()
      .addSiteId(387280)
      .build()
  )

  stubGzipped(RealtyDataType.DeveloperWithChat, developerWithChat)

}
