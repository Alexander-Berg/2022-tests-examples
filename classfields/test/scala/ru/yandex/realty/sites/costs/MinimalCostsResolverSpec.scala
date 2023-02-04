package ru.yandex.realty.sites.costs

import org.junit.runner.RunWith
import org.scalactic.Equality
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.graph.core.Node
import ru.yandex.realty.model.sites.{BuildingClass, MinimalCosts, Site}
import ru.yandex.realty.proto.unified.offer.address.Highway
import ru.yandex.realty.sites.DefaultBidService.DEFAULT_SITE_MINIMAL_COST

import java.util
import scala.collection.mutable

@RunWith(classOf[JUnitRunner])
class MinimalCostsResolverSpec extends SpecBase with PropertyChecks {

  implicit val costsEq: Equality[MinimalCosts] =
    (a: MinimalCosts, b: Any) =>
      b match {
        case p: MinimalCosts =>
          a.minimalCost == p.minimalCost &&
            a.specialPartnerMinimalCost === p.specialPartnerMinimalCost &&
            a.specialPrice === p.specialPrice
        case _ => false
      }

  private val defaultCosts =
    new MinimalCosts(
      DEFAULT_SITE_MINIMAL_COST,
      DEFAULT_SITE_MINIMAL_COST,
      DEFAULT_SITE_MINIMAL_COST
    )

  private val costsFromMaxDistance = new MinimalCosts(1, 1, 1)
  private val costsFromMedDistance = new MinimalCosts(3, 2, 4)
  private val costsFromMinDistance = new MinimalCosts(5, 7, 6)
  private val costsFromZeroDistance = new MinimalCosts(10, 8, 9)

  private val distancesMap: CostsByDistance = {
    val map = new util.TreeMap[Int, MinimalCosts]()
    map.put(0, costsFromZeroDistance)
    map.put(1, costsFromMinDistance)
    map.put(5, costsFromMedDistance)
    map.put(10, costsFromMaxDistance)
    CostsByDistance(map)
  }

  private val costsMap: MinimalCostsData = MinimalCostsData(
    mutable.Map(1 -> CostsByBuildingClass(Map(BuildingClass.STANDART -> distancesMap)))
  )

  private val testData =
    Table(
      ("desc", "costsMap", "geoId", "distance", "buildingClass", "expected"),
      (
        "return default value for empty costs map",
        MinimalCostsData(mutable.Map()),
        Integer.valueOf(1),
        0,
        BuildingClass.ECONOM,
        defaultCosts
      ),
      (
        "return zero distance costs for empty highway distance",
        costsMap,
        Integer.valueOf(1),
        0,
        BuildingClass.STANDART,
        costsFromZeroDistance
      ),
      (
        "return zero distance costs for null highway distance",
        costsMap,
        Integer.valueOf(1),
        0,
        BuildingClass.STANDART,
        costsFromZeroDistance
      ),
      (
        "return default costs if no config found for given building class",
        costsMap,
        Integer.valueOf(1),
        0,
        BuildingClass.ELITE,
        defaultCosts
      ),
      (
        "return medium costs cause highway distance is 6",
        costsMap,
        Integer.valueOf(1),
        6,
        BuildingClass.STANDART,
        costsFromMedDistance
      ),
      (
        "return min costs for long highway distance",
        costsMap,
        Integer.valueOf(1),
        55,
        BuildingClass.STANDART,
        costsFromMaxDistance
      ),
      (
        "return min costs is no corresponding geo found",
        costsMap,
        null,
        0,
        BuildingClass.STANDART,
        defaultCosts
      )
    )

  "MinimalCostsResolver " should {
    val graph = stub[RegionGraph]
    (graph.getNodeByGeoId(_: Integer)).when(*).returns(null)
    (graph.getRandomPathToRoot(_: Node)).when(*).returns(null)

    forAll(testData) {
      (
        desc: String,
        costsMap: MinimalCostsData,
        geoId: Integer,
        distanceKm: Int,
        buildingClass: BuildingClass,
        expected: MinimalCosts
      ) =>
        desc in {
          val resolver = new MinimalCostsResolverImpl(new MinimalCostsHolder(costsMap))
          val minimalCosts = resolver.getMinimalCosts(geoId, distanceKm, buildingClass, graph)
          minimalCosts should equal(expected)
        }
    }
  }

}
