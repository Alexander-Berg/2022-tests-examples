package ru.yandex.realty.building.searcher.flatplans

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.building.searcher.flatplans.manager.FlatPlansManager
import ru.yandex.realty.buildinginfo.model.Building
import ru.yandex.realty.buildinginfo.storage.BuildingStorage
import ru.yandex.realty.flatplan.FlatPlanSearcherTestUtils
import ru.yandex.realty.flatplan.FlatPlanSearcherTestUtils.Plan
import ru.yandex.realty.storage.{BuildingSeries, BuildingSeriesStorage}

@RunWith(classOf[JUnitRunner])
class FlatPlansManagerSpec extends FlatSpec with Matchers {

  behavior.of(classOf[FlatPlansManager].getName)

  private val indexedAddress = "Indexed Address"
  private val indexedAddressSeries = 123L
  private val indexedAddressSeriesName = "Series"
  private val indexedBuilding = {
    val b = new Building.Builder()
    b.address = indexedAddress
    b.buildingId = 1L
    b.buildingSeriesId = indexedAddressSeries
    b.latitude = 0f
    b.longitude = 0f
    b.build()
  }
  private val buildingStorage: BuildingStorage = new BuildingStorage(null) {
    override def getByAddress(address: String): Building =
      if (address == indexedAddress) {
        indexedBuilding
      } else {
        null
      }
  }
  private val buildingSeriesStorage = BuildingSeriesStorage(
    Seq(
      BuildingSeries(indexedAddressSeries, indexedAddressSeriesName, Seq.empty, None)
    )
  )
  private var manager: FlatPlansManager = _

  private def index(plans: Plan*): Unit = {
    manager = new FlatPlansManager(
      FlatPlanSearcherTestUtils.createSearcherOverPlans(plans: _*),
      buildingStorage,
      new Provider[BuildingSeriesStorage] {
        override def get(): BuildingSeriesStorage = buildingSeriesStorage
      }
    )
  }

  it should "suggest multiple variants when not sure" in {
    index(
      Plan(indexedAddressSeries, None, "v1", 10f, 10f, Seq(10f), 10f, "//1-1/orig"),
      Plan(indexedAddressSeries, None, "v1", 20f, 10f, Seq(10f), 10f, "//1-2/orig"),
      Plan(indexedAddressSeries, None, "v2", 11f, 10f, Seq(10f), 10f, "//2-1/orig")
    )
    val response = manager.suggest(
      FlatPlansQuery(indexedAddress, totalArea = Some(10.5f), livingArea = None, kitchenArea = None, roomsCount = None)
    )
    response.seriesName shouldBe indexedAddressSeriesName
    response.plans.size shouldBe 2
    response.plans.map(_.getImageLinkSvg).toSet shouldBe Set("//1-1/orig", "//2-1/orig")
  }

}
