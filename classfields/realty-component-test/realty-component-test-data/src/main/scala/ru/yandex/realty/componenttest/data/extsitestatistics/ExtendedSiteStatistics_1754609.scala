package ru.yandex.realty.componenttest.data.extsitestatistics

import ru.yandex.realty.componenttest.data.utils.ComponentTestDataUtils.{
  extractIdFromClassName,
  loadProtoFromJsonResource
}
import ru.yandex.realty.model.message.ExtDataSchema.ExtendedSiteStatisticsEntryMessage

object ExtendedSiteStatistics_1754609 {

  val Id: Long = extractIdFromClassName(getClass)

  val Proto: ExtendedSiteStatisticsEntryMessage =
    loadProtoFromJsonResource[ExtendedSiteStatisticsEntryMessage](s"ext_site_statistics/ext_site_statistics_$Id.json")

  require(Proto.getSiteId == Id, s"Site ID is not matched to expected: expectedId=$Id, protoId=${Proto.getSiteId}")

}
