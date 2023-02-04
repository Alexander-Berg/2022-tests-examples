package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty.model.message.ExtDataSchema.RelevanceModelMessage

trait ExtdataCatBoostModelsResourceStub extends ExtdataResourceStub {

  private val catBoostModels: Seq[RelevanceModelMessage] = Seq()

  stubGzipped(RealtyDataType.CatBoostModels, catBoostModels)

}
