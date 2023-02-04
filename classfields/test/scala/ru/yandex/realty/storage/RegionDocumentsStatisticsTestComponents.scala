package ru.yandex.realty.storage

import ru.yandex.common.util.collections.Bag
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.index.{DocumentsStatistics, OfferFlag, RegionDocumentsStatistics}
import ru.yandex.realty.model.offer.{CategoryType, OfferType, RentTime}

import scala.collection.JavaConverters._

trait RegionDocumentsStatisticsTestComponents {

  def regionDocumentsStatisticsProvider: Provider[RegionDocumentsStatisticsStorage] =
    RegionDocumentsStatisticsTestComponents.regionDocumentsStatisticsProvider
}

object RegionDocumentsStatisticsTestComponents {
  val RostovskayaOblastStatistics = new RegionDocumentsStatistics(211571, buildDocumentsStatistics(1000))
  val RostovNaDonuCityDistrictStatistics = new RegionDocumentsStatistics(1746, buildDocumentsStatistics(500))
  val RostovNaDonuCityStatistics = new RegionDocumentsStatistics(214386, buildDocumentsStatistics(200))
  val KrasnayaPolanaStatistics = new RegionDocumentsStatistics(180000, buildDocumentsStatistics(500))

  val storage = new RegionDocumentsStatisticsStorage(
    Seq(
      RostovskayaOblastStatistics,
      RostovNaDonuCityDistrictStatistics,
      RostovNaDonuCityStatistics,
      KrasnayaPolanaStatistics
    ).asJava
  )

  val regionDocumentsStatisticsProvider: Provider[RegionDocumentsStatisticsStorage] = () => storage

  private def buildDocumentsStatistics(totalOffers: Int): java.util.List[DocumentsStatistics] = {
    val statistics = new DocumentsStatistics(
      OfferType.SELL,
      CategoryType.APARTMENT,
      RentTime.UNKNOWN,
      totalOffers,
      0,
      Bag.newEnumBag(classOf[OfferFlag])
    )
    Seq(statistics).asJava
  }
}
