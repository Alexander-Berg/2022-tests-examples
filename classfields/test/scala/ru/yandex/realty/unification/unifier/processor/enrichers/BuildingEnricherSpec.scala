package ru.yandex.realty.unification.unifier.processor.enrichers

import org.junit.runner.RunWith
import org.mockito.Mockito
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.building.model.BuildingEpoch
import ru.yandex.realty.buildinginfo.model.Building
import ru.yandex.realty.buildinginfo.storage.BuildingStorage
import ru.yandex.realty.{model, AsyncSpecBase}
import ru.yandex.realty.model.gen.{OfferModelGenerators, RealtyGenerators}
import ru.yandex.realty.model.location.{GeoPoint, Location, LocationAccuracy}
import ru.yandex.realty.model.offer.{BuildingInfo, CategoryType}
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.protobuf.ProtoMacro._

/**
  * Specs on HTTP [[BuildingEnricher]].
  *
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class BuildingEnricherSpec extends AsyncSpecBase with Matchers with RealtyGenerators with ProducerProvider {

  implicit val trace: Traced = Traced.empty

  private val mockBuildingStorage = Mockito.mock(classOf[BuildingStorage])
  private val buildingEnricher = new BuildingEnricher(mockBuildingStorage)

  "BuildingEnricher" should {
    "not enrich building info from building database for offer with inexact location" in {
      val offers = for {
        locationAccuracy <- LocationAccuracy.values()
        if locationAccuracy != LocationAccuracy.EXACT
      } yield {
        val offer = OfferModelGenerators.offerGen().next
        val location = new Location()
        location.setGeocoderLocation(
          "Россия, Тверская область, Калининский район, деревня Тестово",
          GeoPoint.getPoint(56.981724f, 36.067107f)
        )
        location.setAccuracy(locationAccuracy)
        location.setRegionGraphId(posNum[Long].next)
        location.setGeocoderId(posNum[Int].next)
        offer.setLocation(location)
        offer.setCategoryType(CategoryType.APARTMENT)
        offer.setBuildingInfo(new BuildingInfo)
        offer
      }
      offers.foreach { o =>
        Mockito.reset(mockBuildingStorage)
        buildingEnricher.enrich(o).futureValue
        Mockito.verifyZeroInteractions(mockBuildingStorage)

        ?(o.getBuildingInfo.getBuildingId) should be(None)
      }
    }

    "enrich building info from building database for offer with exact location" in {
      val offer = OfferModelGenerators.offerGen().next
      val location = new Location()
      location.setGeocoderLocation(
        "Россия, Санкт-Петербург, Пискарёвский проспект, 2к2Щ",
        GeoPoint.getPoint(59.958606f, 30.405313f)
      )
      location.setAccuracy(LocationAccuracy.EXACT)
      location.setRegionGraphId(posNum[Long].next)
      location.setGeocoderId(posNum[Int].next)
      offer.setLocation(location)
      offer.setCategoryType(CategoryType.ROOMS)
      offer.setBuildingInfo(new BuildingInfo)
      offer.setApartmentInfo(null)

      val building = new Building.Builder
      building.buildingId = 665343139033270424L
      building.address = location.getGeocoderAddress
      building.latitude = 30.0f
      building.longitude = 60.0f
      building.buildYear = 2008
      building.floorsCount = 8
      building.buildingEpoch = BuildingEpoch.BUILDING_EPOCH_STALIN

      Mockito.reset(mockBuildingStorage)
      Mockito.when(mockBuildingStorage.getByAddress(location.getGeocoderAddress)).thenReturn(building.build())

      buildingEnricher.enrich(offer).futureValue

      ?(offer.getBuildingInfo.getBuildingId) should be(Some(building.buildingId))
      ?(offer.getBuildingInfo.getBuildYear) should be(Some(building.buildYear))
      ?(offer.getBuildingInfo.getFloorsTotal) should be(Some(building.floorsCount))
      ?(offer.getBuildingInfo.getBuildingEpoch) should be(Some(building.buildingEpoch))
    }
  }
}
