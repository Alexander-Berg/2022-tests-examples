package ru.yandex.auto.vin.decoder.report.converters.formats.proto.builder.essentials

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.CommonModel.SteeringWheel
import ru.yandex.auto.vin.decoder.manager.vin.catalog.UnifiedData
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfo
import ru.yandex.auto.vin.decoder.report.converters.raw.blocks.offers.PreparedVosData

class CarInfoBuilderTest extends AnyFunSuite {

  val preparedCatalogData = UnifiedData.Empty.copy(
    mark = Some(""),
    markName = Some(""),
    model = Some(""),
    modelName = Some(""),
    optWheel = Some(SteeringWheel.RIGHT)
  )

  val lastOffer = VinInfo.newBuilder().setWheel("RIGHT").build()
  val preparedVosData = PreparedVosData(Seq(lastOffer), Some(lastOffer))

  test("build") {
    val carInfo = CarInfoBuilder.build(preparedCatalogData)
    assert(carInfo.getSteeringWheel == SteeringWheel.RIGHT)
  }
}
