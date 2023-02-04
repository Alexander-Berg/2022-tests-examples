package ru.yandex.realty.componenttest.data.auctionresults

import ru.yandex.realty.componenttest.data.extsitestatistics.ExtendedSiteStatistics_57547
import ru.yandex.realty.componenttest.data.salesdepartments.SalesDepartment_56576
import ru.yandex.realty.componenttest.data.sites.Site_57547
import ru.yandex.realty.componenttest.data.utils.ComponentTestDataUtils.extractIdFromClassName
import ru.yandex.realty.model.message.ExtDataSchema.AuctionResultMessage

object AuctionResult_57547 {

  val SiteId: Long = extractIdFromClassName(getClass)

  val Proto: AuctionResultMessage =
    AuctionResultMessage
      .newBuilder()
      .setSiteId(Site_57547.Id)
      .addItems(SalesDepartment_56576.Proto)
      .setStats(ExtendedSiteStatistics_57547.Atom)
      .build()

}
