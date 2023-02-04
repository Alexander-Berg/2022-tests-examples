package ru.yandex.realty.componenttest.data.verba

import ru.yandex.realty.componenttest.data.utils.ComponentTestDataUtils
import ru.yandex.realty.model.message.RealtySchema.VerbaDescription

object VerbaDescriptions {

  val all: Seq[VerbaDescription] =
    ComponentTestDataUtils.loadProtoListFromJsonResource[VerbaDescription]("verba/verba_descriptions.json")

  require(all.nonEmpty, "Verba descriptions should not be empty")

}
