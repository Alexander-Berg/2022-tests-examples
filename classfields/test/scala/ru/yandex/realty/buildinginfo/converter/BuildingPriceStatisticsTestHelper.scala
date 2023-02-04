package ru.yandex.realty.buildinginfo.converter

import ru.yandex.realty.model.building.{BuildingPriceStatistics, BuildingPriceStatisticsItem}

/**
  * @author azakharov
  */
object BuildingPriceStatisticsTestHelper {

  def createBuildingPriceStatistics(
    sellPriceSqm: Long,
    sellOfferCount: Long,
    sellPrice1Room: Long,
    rentPrice1Room: Long,
    rentOfferCount: Long,
    profitability: Float
  ): BuildingPriceStatistics = {
    BuildingPriceStatistics(
      sellPrice = Some(BuildingPriceStatisticsItem(sellPriceSqm, sellOfferCount, None)),
      sellPriceByRooms = Map(1 -> BuildingPriceStatisticsItem(sellPrice1Room, sellOfferCount, None)),
      rentPriceByRooms = Map(1 -> BuildingPriceStatisticsItem(rentPrice1Room, rentOfferCount, None)),
      profitability = Some(profitability)
    )
  }
}
