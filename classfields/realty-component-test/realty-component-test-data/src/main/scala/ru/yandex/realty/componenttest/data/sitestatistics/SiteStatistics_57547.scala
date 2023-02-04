package ru.yandex.realty.componenttest.data.sitestatistics

import ru.yandex.realty.componenttest.data.sites.Site_57547
import ru.yandex.realty.componenttest.data.utils.ComponentTestDataUtils.extractIdFromClassName
import ru.yandex.realty.model.message.ExtDataSchema.SiteStatisticsStorageEntryMessage
import ru.yandex.realty.model.serialization.RealtySchemaVersions

object SiteStatistics_57547 {

  val Id: Long = extractIdFromClassName(getClass)

  val Proto: SiteStatisticsStorageEntryMessage =
    SiteStatisticsStorageEntryMessage
      .newBuilder()
      .setVersion(RealtySchemaVersions.SITES_STATISTICS_VERSION)
      .setSiteId(Site_57547.Id)
      .setTrustedOffers(true)
      .setOffersCount(12)
      .setOffersCount2Room(11)
      .setOffersCount3Room(1)
      .setPriceMin(3000000.0f)
      .setPriceMax(125555558.0f)
      .build()

}
