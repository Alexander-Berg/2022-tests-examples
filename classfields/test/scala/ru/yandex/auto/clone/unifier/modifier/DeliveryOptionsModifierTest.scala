package ru.yandex.auto.clone.unifier.modifier

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.auto.core.model.UnifiedAbstractInfo
import ru.yandex.auto.core.region.{Region, RegionService, RegionTree, RegionType}
import ru.yandex.auto.message.AutoUtilsSchema.DeliveryInfoMessage
import ru.yandex.vertis.mockito.MockitoSupport
import MockitoSupport._
import org.mockito.ArgumentMatchers

@RunWith(classOf[JUnitRunner])
class DeliveryOptionsModifierTest extends WordSpecLike with Matchers {
  val regions = mock[RegionService]
  val regionTree = mock[RegionTree]
  when(regions.getRegionTree).thenReturn(regionTree)
  when(regionTree.getRegion(ArgumentMatchers.eq(213))).thenReturn(new Region(213, "", RegionType.CITY, null, 38, 55))
  when(regionTree.getRegion(ArgumentMatchers.eq(-1))).thenReturn(null)
  val modifier = new DeliveryOptionsModifier(regions)

  "delivery without rid should be removed" in {
    val info = new UnifiedAbstractInfo {
      override def getState: String = ???
    }

    val delivery = DeliveryInfoMessage.newBuilder()
    val locationBuilder = delivery.addDeliveryRegionsBuilder().getLocationBuilder
    locationBuilder.getCoordinatesBuilder.setLatitude(10).setLongitude(12)

    info.setDeliveryOptions(delivery.build())

    info.getDeliveryOptions.getDeliveryRegionsCount shouldEqual 1

    modifier.modify(info)

    info.getDeliveryOptions.getDeliveryRegionsCount shouldEqual 0
  }

  "delivery without coordinates should be enriched" in {
    val info = new UnifiedAbstractInfo {
      override def getState: String = ???
    }

    val delivery = DeliveryInfoMessage.newBuilder()
    val locationBuilder = delivery.addDeliveryRegionsBuilder().getLocationBuilder
    locationBuilder.setGeobaseId(213)

    info.setDeliveryOptions(delivery.build())

    info.getDeliveryOptions.getDeliveryRegionsCount shouldEqual 1
    info.getDeliveryOptions.getDeliveryRegions(0).getLocation.getGeobaseId shouldEqual 213

    modifier.modify(info)

    info.getDeliveryOptions.getDeliveryRegionsCount shouldEqual 1
    info.getDeliveryOptions.getDeliveryRegions(0).getLocation.getCoordinates.getLatitude shouldEqual 38
  }

  "delivery without coordinates and unknown rid should be removed" in {
    val info = new UnifiedAbstractInfo {
      override def getState: String = ???
    }

    val delivery = DeliveryInfoMessage.newBuilder()
    val locationBuilder = delivery.addDeliveryRegionsBuilder().getLocationBuilder
    locationBuilder.setGeobaseId(-1)

    info.setDeliveryOptions(delivery.build())

    info.getDeliveryOptions.getDeliveryRegionsCount shouldEqual 1

    modifier.modify(info)

    info.getDeliveryOptions.getDeliveryRegionsCount shouldEqual 0
  }
}
