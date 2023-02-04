package ru.yandex.realty2.extdataloader.loaders.feed

import org.junit.runner.RunWith
import org.mockito.Mockito
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.common.util.IOUtils
import ru.yandex.realty.SpecBase
import ru.yandex.realty.context.ProviderAdapter
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.graph.serialize.RegionGraphProtoConverter
import ru.yandex.realty.model.offer._
import ru.yandex.realty.searcher.context.{SearchContext, SearchContextProvider}
import ru.yandex.realty2.extdataloader.loaders.feed.TestUtils.offerMoscowApartmentSell
import ru.yandex.realty2.extdataloader.loaders.lucene.readers.filters.OfferFilter
import ru.yandex.realty2.extdataloader.loaders.lucene.readers.{Feed2GISBuilder, FeedOffersProcessor}

@RunWith(classOf[JUnitRunner])
class Feed2GISBuilderSpec extends SpecBase {

  val regionGraph: RegionGraph =
    RegionGraphProtoConverter.deserialize(
      IOUtils.gunzip(
        getClass.getClassLoader.getResourceAsStream("region_graph-8-2.data")
      )
    )
  private val regionGraphProvider = ProviderAdapter.create(regionGraph)

  private val searchContextProviderMock = Mockito.mock(classOf[SearchContextProvider[SearchContext]])
  private val filter = new OfferFilter {
    override def validateOffer(offer: Offer): Boolean = true
  }
  private val feedOfferProcessor = new FeedOffersProcessor(searchContextProviderMock)
  private val feed2GISBuilder = new Feed2GISBuilder(regionGraphProvider, feedOfferProcessor, filter)

  "Correct write header" in {
    feed2GISBuilder.header shouldBe
      "Final URL,Area,Rooms,Price,City,Address,GeoLatitude,GeoLongitude,Listing type"
  }

  "Correct write footer" in {
    feed2GISBuilder.footer shouldBe ""
  }

  "Correct write body" in {
    feed2GISBuilder
      .processOffer(offerMoscowApartmentSell)
      .getOrElse("") shouldBe "https://realty.test.vertis.yandex.ru/offer/6302897158613069191," +
      "91.4 м²," +
      "3-комнатная квартира,6918980 RUB," +
      "Москва и МО," +
      "\"Глухово д, жилой комплекс Ильинские Луга, к38\"," +
      "55.777267," +
      "37.242355," +
      "sell_new"
  }

}
