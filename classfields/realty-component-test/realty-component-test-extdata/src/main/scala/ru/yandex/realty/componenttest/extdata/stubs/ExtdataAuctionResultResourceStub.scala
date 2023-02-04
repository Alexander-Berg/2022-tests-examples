package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.data.auctionresults.AuctionResult_57547
import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty.model.message.ExtDataSchema.AuctionResultMessage

trait ExtdataAuctionResultResourceStub extends ExtdataResourceStub {

  private val messages: Seq[AuctionResultMessage] = {
    Seq(
      AuctionResult_57547.Proto
    )
  }

  stubGzipped(RealtyDataType.AuctionResult, messages)

}
