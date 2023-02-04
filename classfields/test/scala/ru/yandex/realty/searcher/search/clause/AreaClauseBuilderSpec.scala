package ru.yandex.realty.searcher.search.clause

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.offer.{AreaInfo, AreaUnit, Offer}
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.AreaClauseBuilder
import ru.yandex.realty.util.Range

import scala.collection.JavaConverters._

/**
  * купить квартиру от 50м² до 100м² -> в точный поиск попали кв площадью 51м² и 99м², в неточный 46м², 49м², 101м², 109м², не попали в выдачу за 44м², 111м²
  * купить квартиру от 50м² до 100м² + без похожих -> в выдачу попали кв площадью 51м² и 99м², не попали кв 49м² и 101м²
  */
@RunWith(classOf[JUnitRunner])
class AreaClauseBuilderSpec extends SpecBase with NRTIndexFixture with NRTIndexOfferGenerator {

  private val clauseBuilders = Seq(new AreaClauseBuilder())
  private val offers: Seq[Offer] =
    Seq(51, 99, 46, 49, 101, 109, 44, 111).zipWithIndex
      .map {
        case (area, index) => buildOffer(index, area)
      }

  insertOffers(offers)

  "AreaClauseBuilder" should {

    "search for all offers" in {
      val searchQuery = new SearchQuery()
      searchQuery.setArea(Range.create(0f, 200f))

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 8
    }

    "search offers with given area" in {
      val searchQuery = new SearchQuery()
      searchQuery.setArea(Range.create(50f, 100f))
      searchQuery.setShowSimilar(false)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 2
      result.getItems.asScala.map(offer => offer.getArea.getValue).toSet shouldBe Set(51f, 99f)
    }

    "search offers with given area with similar" in {
      val searchQuery = new SearchQuery()
      searchQuery.setArea(Range.create(50f, 100f))
      searchQuery.setShowSimilar(true)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 6
      result.getItems.asScala.map(offer => offer.getArea.getValue).toSet shouldBe Set(51f, 99f, 46f, 49f, 101f, 109f)
    }

  }

  private def buildOffer(offerId: Long, area: Int): Offer = {
    val offer = offerGen(offerId = offerId).next
    val areaInfo = AreaInfo.create(AreaUnit.SQUARE_METER, area)
    offer.setArea(areaInfo)
    offer
  }
}
