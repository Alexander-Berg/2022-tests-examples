package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.data.bunker.{
  BunkerTestingPhoneNumbers,
  BunkerTestingPinnedSpecialProjects,
  BunkerTestingSiteSpecialProjects
}
import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty.resources.BunkerNode

trait ExtdataBunkerResourceStub extends ExtdataResourceStub {

  private val bunker: Seq[BunkerNode] = Seq(
    BunkerTestingPhoneNumbers.Node,
    BunkerTestingSiteSpecialProjects.Node,
    BunkerTestingPinnedSpecialProjects.Node
  )

  stubGzipped(RealtyDataType.Bunker, bunker)

}
