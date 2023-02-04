package ru.yandex.realty.sites.costs

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.graph.core.Node
import ru.yandex.realty.model.sites.BuildingClass

import java.util
import scala.collection.mutable

@RunWith(classOf[JUnitRunner])
class MinimalCostsHolderSpec extends SpecBase with PropertyChecks {

  "MinimalCostsHolder " should {
    "invoke graph getRandomPathToRoot only once, cause map was updated" in {
      val graph = mock[RegionGraph]
      val node = new Node
      node.setGeoId(3);
      val node2 = new Node
      node2.setGeoId(1);
      val path = new util.ArrayList[Node]()
      path.add(node2)

      (graph.getNodeByGeoId(_: Int)).expects(Integer.valueOf(2).intValue()).returning(node).once()
      (graph.getRandomPathToRoot(_: Node)).expects(node).returns(path).once()

      val costsMap = MinimalCostsData(mutable.Map(1 -> CostsByBuildingClass(Map())))
      val resolver = new MinimalCostsResolverImpl(new MinimalCostsHolder(costsMap))
      var minimalCosts = resolver.getMinimalCosts(
        Integer.valueOf(2),
        0,
        BuildingClass.BUSINESS,
        graph
      )

      (graph.getNodeByGeoId(_: Int)).expects(*).returning(node).never()
      (graph.getRandomPathToRoot _).expects(*).returning(path).never()
      minimalCosts = resolver.getMinimalCosts(
        Integer.valueOf(2),
        0,
        BuildingClass.BUSINESS,
        graph
      )
    }
  }

}
