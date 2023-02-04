package ru.yandex.realty2.extdataloader.loaders.feed

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.common.util.IOUtils
import ru.yandex.realty.SpecBase
import ru.yandex.realty.context.ProviderAdapter
import ru.yandex.realty.graph.serialize.RegionGraphProtoConverter
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.offer._
import ru.yandex.realty.model.region.Regions
import ru.yandex.realty2.extdataloader.loaders.lucene.readers.filters.Feed2GISFilter

@RunWith(classOf[JUnitRunner])
class Feed2GISFilterSpec extends SpecBase {

  val regionGraph: RegionGraph =
    RegionGraphProtoConverter.deserialize(
      IOUtils.gunzip(
        getClass.getClassLoader.getResourceAsStream("region_graph-8-2.data")
      )
    )
  private val regionGraphProvider = ProviderAdapter.create(regionGraph)

  "Feed2GISFilter" should {
    "correct filter offers" in {
      val filter = new Feed2GISFilter(regionGraphProvider, Set(Regions.SPB_AND_LEN_OBLAST))

      filter.validateOffer(OfferSpbApartmentSell) shouldBe true
      filter.validateOffer(OfferSpbRoomSell) shouldBe true

      filter.validateOffer(OfferMoscowApartmentSell) shouldBe false
      filter.validateOffer(OfferMoscowRoomSell) shouldBe false

      filter.validateOffer(OfferMoscowApartmentRent) shouldBe false
      filter.validateOffer(OfferMoscowHouseSell) shouldBe false
    }
  }

  private val OfferSpbApartmentSell: Offer = {
    val offer = new Offer()
    offer.setCategoryType(CategoryType.APARTMENT)
    offer.setOfferType(OfferType.SELL)
    offer.setLocation(new Location)
    offer.getLocation.setGeocoderId(Regions.SPB)
    offer
  }

  private val OfferSpbRoomSell: Offer = {
    val offer = new Offer()
    offer.setCategoryType(CategoryType.ROOMS)
    offer.setOfferType(OfferType.SELL)
    offer.setLocation(new Location)
    offer.getLocation.setGeocoderId(Regions.SPB)
    offer
  }

  private val OfferMoscowApartmentSell: Offer = {
    val offer = new Offer()
    offer.setCategoryType(CategoryType.APARTMENT)
    offer.setOfferType(OfferType.SELL)
    offer.setLocation(new Location)
    offer.getLocation.setGeocoderId(Regions.MSK_AND_MOS_OBLAST)
    offer
  }

  private val OfferMoscowRoomSell: Offer = {
    val offer = new Offer()
    offer.setCategoryType(CategoryType.ROOMS)
    offer.setOfferType(OfferType.SELL)
    offer.setLocation(new Location)
    offer.getLocation.setGeocoderId(Regions.MSK_AND_MOS_OBLAST)
    offer
  }

  private val OfferMoscowHouseSell: Offer = {
    val offer = new Offer()
    offer.setCategoryType(CategoryType.HOUSE)
    offer.setOfferType(OfferType.SELL)
    offer.setLocation(new Location)
    offer.getLocation.setGeocoderId(Regions.MSK_AND_MOS_OBLAST)
    offer
  }

  private val OfferMoscowApartmentRent: Offer = {
    val offer = new Offer()
    offer.setCategoryType(CategoryType.APARTMENT)
    offer.setOfferType(OfferType.RENT)
    offer.setLocation(new Location)
    offer.getLocation.setGeocoderId(Regions.MSK_AND_MOS_OBLAST)
    offer
  }

}
