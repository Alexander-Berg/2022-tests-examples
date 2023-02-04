package ru.yandex.realty.searcher.search.clause

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.offer.{BuildingInfo, BuildingType, Offer}
import ru.yandex.realty.proto.offer.BalconyType
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.{
  BalconyClauseBuilder,
  BuildingTypeOrEpochClauseBuilder,
  MatchAllDocsClauseBuilder
}

import scala.collection.JavaConverters._

/**
  * купить квартиру + тип дома - есть 8 квартир всех типов:
  * кирпичный, монолитный, панельный, кирпично-молитный, панельный, деревянный, железобетонный + 1, где тип не указан
  * - поочередно проверяем фильтрацию по каждому типу,
  * в точном совпадении должен оставаться выбранный,
  * в неточном - кв, где тип не указан, кв остальных типов должны отсекать.
  */
@RunWith(classOf[JUnitRunner])
class BuildingTypeClauseBuilderSpec extends SpecBase with NRTIndexFixture with NRTIndexOfferGenerator {

  private val clauseBuilders = Seq(new MatchAllDocsClauseBuilder, new BuildingTypeOrEpochClauseBuilder())
  private val supportedTypes = Seq(
    BuildingType.WOOD,
    BuildingType.MONOLIT,
    BuildingType.BRICK,
    BuildingType.MONOLIT_BRICK,
    BuildingType.PANEL,
    BuildingType.FERROCONCRETE,
    BuildingType.METAL
  )
  private val offers: Seq[Offer] = supportedTypes.map(bt => buildOffer(bt.value(), Some(bt))) :+ buildOffer(1000, None)

  insertOffers(offers)

  "BuildingTypeClauseBuilder" should {

    "search for all offers" in {
      val searchQuery = new SearchQuery()
      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe offers.size
    }

    "search offers with concrete building type with similar" in {
      supportedTypes.foreach { bt =>
        val searchQuery = new SearchQuery()
        searchQuery.setShowSimilar(true)
        searchQuery.setBuildingType(bt)
        val resultWithSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size)

        resultWithSimilar.getItems.asScala.map(offer => offer.getLongId).toSet shouldBe Set(1000L, bt.value())
      }
    }

    "search offers with concrete building type without similar" in {
      supportedTypes.foreach { bt =>
        val searchQuery = new SearchQuery()
        searchQuery.setShowSimilar(false)
        searchQuery.setBuildingType(bt)
        val resultWithSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size)

        resultWithSimilar.getItems.asScala.map(offer => offer.getLongId).toSet shouldBe Set(bt.value())
      }
    }

  }

  private def buildOffer(offerId: Long, buildingType: Option[BuildingType]): Offer = {
    val offer = offerGen(offerId = offerId).next
    if (offer.getBuildingInfo == null) {
      offer.setBuildingInfo(new BuildingInfo())
    }
    offer.getBuildingInfo.setBuildingType(BuildingType.UNKNOWN)
    buildingType.foreach(b => offer.getBuildingInfo.setBuildingType(b))
    offer
  }
}
