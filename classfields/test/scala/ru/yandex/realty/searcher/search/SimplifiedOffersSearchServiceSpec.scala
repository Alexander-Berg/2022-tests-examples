package ru.yandex.realty.searcher.search

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.buildinginfo.model.Building
import ru.yandex.realty.buildinginfo.storage.BuildingStorage
import ru.yandex.realty.context.v2.{AuctionResultStorage, CatBoostModelStorage}
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.model.offer.{BuildingInfo, Offer}
import ru.yandex.realty.model.offer.OfferType
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.personalization.persistence.PersonalizationApi
import ru.yandex.realty.searcher.search.clause.{NRTIndexFixture, NRTIndexOfferGenerator}
import ru.yandex.realty.storage.{CurrencyStorage, TargetCallRegionsStorage}
import ru.yandex.realty.searcher.api.SearcherApi.BuildingWithStats

import java.lang
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class SimplifiedOffersSearchServiceSpec
  extends AsyncSpecBase
  with NRTIndexFixture
  with NRTIndexOfferGenerator
  with RegionGraphTestComponents {

  private val relevanceModelProvider: Provider[CatBoostModelStorage] = () => new CatBoostModelStorage(Seq.empty)
  private val currencyProvider: Provider[CurrencyStorage] = () =>
    new CurrencyStorage(Seq.empty.asJava, Seq.empty.asJava, regionGraphProvider)
  private val auctionResultProvider: Provider[AuctionResultStorage] = () => new AuctionResultStorage(Seq())
  private val targetCallRegionsProvider: Provider[TargetCallRegionsStorage] = () =>
    new TargetCallRegionsStorage(Set.empty, Set.empty)
  private val personalizationApi: PersonalizationApi = mock[PersonalizationApi]
  private val buildingStorage: BuildingStorage = new BuildingStorage(null) {
    override def getByBuildingId(buildingId: lang.Long): Building = buildingId.toLong match {
      case 1 => buildBuilding(1, "1")
      case 3 => buildBuilding(3, "3")
      case _ => null
    }
  }
  private val offerSearchV2Helper = new OfferSearchV2Helper(
    currencyProvider,
    regionGraphProvider,
    relevanceModelProvider,
    auctionResultProvider,
    targetCallRegionsProvider,
    personalizationApi
  )
  private val simplifiedOffersSearchService =
    new SimplifiedOffersSearchService(offerSearchV2Helper, buildingStorage, index)

  "SimplifiedOffersSearchService" should {
    "count offers by building" in {
      val buildingId2OfferIds = Map(1 -> Seq(100, 101, 102), 2 -> Seq.empty, 3 -> Seq(103), 4 -> Seq(104, 105))
      val offers = for {
        (buildingId, offerIds) <- buildingId2OfferIds
        offerId <- offerIds
      } yield buildOffer(offerId, buildingId)
      insertOffers(offers.toSeq)

      val searchQuery = new SearchQuery()
      searchQuery.setType(OfferType.SELL)
      searchQuery.setRevoked(null)
      searchQuery.setClusterHead(null)
      searchQuery.setPremoderation(null)

      val expected =
        Set(buildBuildingWithStats(1, "1", 3), buildBuildingWithStats(3, "3", 1))
      simplifiedOffersSearchService.countOffersByBuilding(searchQuery).toSet shouldBe expected
    }
  }

  private def buildOffer(id: Long, buildingId: Long): Offer = {
    val offer = offerGen(id).next
    offer.setOfferType(OfferType.SELL)
    val buildingInfo = new BuildingInfo()
    buildingInfo.setBuildingId(buildingId)
    offer.setBuildingInfo(buildingInfo)
    offer
  }

  private def buildBuilding(id: Long, houseNumber: String): Building = {
    val b = new Building.Builder()
    b.buildingId = id
    b.houseNumber = houseNumber
    b.address = ""
    b.latitude = 0
    b.longitude = 0
    b.build()
  }

  private def buildBuildingWithStats(buildingId: Long, houseNumber: String, totalOffers: Int): BuildingWithStats = {
    BuildingWithStats
      .newBuilder()
      .setBuildingId(buildingId)
      .setHouseNumber(houseNumber)
      .setTotalOffers(totalOffers)
      .build()
  }
}
