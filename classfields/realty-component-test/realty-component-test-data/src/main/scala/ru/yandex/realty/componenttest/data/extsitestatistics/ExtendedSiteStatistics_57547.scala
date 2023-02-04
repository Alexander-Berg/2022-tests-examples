package ru.yandex.realty.componenttest.data.extsitestatistics

import ru.yandex.realty.componenttest.data.sites.Site_57547
import ru.yandex.realty.componenttest.data.utils.ComponentTestDataUtils.extractIdFromClassName
import ru.yandex.realty.model.message.ExtDataSchema.{
  ExtendedSiteStatisticsAtomMessage,
  ExtendedSiteStatisticsEntryMessage,
  ExtendedSiteStatisticsMessage,
  FloatRangeMessage,
  RoomsToSimpleSiteStatisticsMessage,
  SimpleSiteStatisticsMessage
}
import ru.yandex.realty.model.message.RealtySchema.CommissioningDateMessage
import ru.yandex.realty.model.offer.Rooms
import ru.yandex.realty.model.serialization.RealtySchemaVersions.{
  COMMISSIONING_DATE_VERSION,
  EXTENDED_SITES_STATISTICS_VERSION
}
import ru.yandex.realty.model.sites.SaleStatus

object ExtendedSiteStatistics_57547 {

  val Id: Long = extractIdFromClassName(getClass)

  val Atom: ExtendedSiteStatisticsAtomMessage =
    ExtendedSiteStatisticsAtomMessage
      .newBuilder()
      .setTotal(
        SimpleSiteStatisticsMessage
          .newBuilder()
          .setArea(
            FloatRangeMessage
              .newBuilder()
              .setFrom(25.7f)
              .setTill(38.0f)
              .build()
          )
          .build()
      )
      .addByRooms(
        RoomsToSimpleSiteStatisticsMessage
          .newBuilder()
          .setKey(Rooms._3.value())
          .setValue(
            SimpleSiteStatisticsMessage
              .newBuilder()
              .setArea(
                FloatRangeMessage
                  .newBuilder()
                  .setFrom(38.0f)
                  .setTill(38.0f)
                  .build()
              )
              .setSaleStatusInt(SaleStatus.TEMPORARILY_UNAVAILABLE.value())
              .build()
          )
          .build()
      )
      .addByRooms(
        RoomsToSimpleSiteStatisticsMessage
          .newBuilder()
          .setKey(Rooms.STUDIO.value())
          .setValue(
            SimpleSiteStatisticsMessage
              .newBuilder()
              .setArea(
                FloatRangeMessage
                  .newBuilder()
                  .setFrom(25.7f)
                  .setTill(25.7f)
                  .build()
              )
              .setSaleStatusInt(SaleStatus.TEMPORARILY_UNAVAILABLE.value())
              .build()
          )
          .build()
      )
      .build()

  val Proto: ExtendedSiteStatisticsEntryMessage =
    ExtendedSiteStatisticsEntryMessage
      .newBuilder()
      .setSiteId(Site_57547.Id)
      .setByDate(
        ExtendedSiteStatisticsMessage
          .newBuilder()
          .setVersion(EXTENDED_SITES_STATISTICS_VERSION)
          .setWithPrimarySale(Atom)
          .addDatesWithOffers(
            CommissioningDateMessage
              .newBuilder()
              .setVersion(COMMISSIONING_DATE_VERSION)
              .setFinished(true)
              .build()
          )
          .addPartners(Site_57547.Proto.getBuilders(0))
          .setTotal(Atom)
          .setTopPriority(Atom)
          .setPriorityValue(2)
          .build()
      )
      .build()

  require(Proto.getSiteId == Id, s"Site ID is not matched to expected: expectedId=$Id, protoId=${Proto.getSiteId}")

}
