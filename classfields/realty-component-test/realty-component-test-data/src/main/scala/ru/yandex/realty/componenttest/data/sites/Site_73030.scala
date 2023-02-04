package ru.yandex.realty.componenttest.data.sites

import ru.yandex.realty.componenttest.data.utils.ComponentTestDataUtils.{
  extractIdFromClassName,
  loadProtoFromJsonResource
}
import ru.yandex.realty.model.message.ExtDataSchema.SiteMessage

object Site_73030 {

  val Id: Long = extractIdFromClassName(getClass)

  val Proto: SiteMessage = loadProtoFromJsonResource[SiteMessage](s"sites/site_$Id.json")

  require(
    Proto.getId == Id,
    s"Loaded proto ID is not matched to expected: expectedId=$Id, protoId=${Proto.getId}"
  )

}
