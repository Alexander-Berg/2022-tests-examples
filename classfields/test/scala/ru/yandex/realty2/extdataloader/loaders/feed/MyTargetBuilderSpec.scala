package ru.yandex.realty2.extdataloader.loaders.feed

import org.mockito.Mockito
import ru.yandex.common.util.IOUtils
import ru.yandex.realty.SpecBase
import ru.yandex.realty.context.ProviderAdapter
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.graph.serialize.RegionGraphProtoConverter
import ru.yandex.realty.picapica.MdsUrlBuilder
import ru.yandex.realty.searcher.context.{SearchContext, SearchContextProvider}
import ru.yandex.realty2.extdataloader.loaders.feed.TestUtils.offerMoscowApartmentSell
import ru.yandex.realty2.extdataloader.loaders.lucene.readers.{FeedOffersProcessor, MyTargetBuilder}

import java.text.SimpleDateFormat
import java.util.Calendar

class MyTargetBuilderSpec extends SpecBase {

  val regionGraph: RegionGraph =
    RegionGraphProtoConverter.deserialize(
      IOUtils.gunzip(
        getClass.getClassLoader.getResourceAsStream("region_graph-8-2.data")
      )
    )
  private val regionGraphProvider = ProviderAdapter.create(regionGraph)

  private val searchContextProviderMock = Mockito.mock(classOf[SearchContextProvider[SearchContext]])
  private val filter = new TestFilter(regionGraphProvider)
  private val feedOfferProcessor = new FeedOffersProcessor(searchContextProviderMock)
  private val feed2GISBuilder =
    new MyTargetBuilder(regionGraphProvider, feedOfferProcessor, filter, new MdsUrlBuilder("//avatarnica.test"))

  "Correct write header" in {
    val pattern = "yyyy-MM-dd HH:mm"
    val formatter = new SimpleDateFormat(pattern)
    val time = Calendar.getInstance.getTime
    val date = formatter.format(time)
    feed2GISBuilder.header shouldBe
      s"""|<?xml version="1.0" encoding="UTF-8"?>
          |<torg_price date="$date">
          |<shop>
          |    <name>realty</name>
          |  <company>Яндекс.Недвижимость</company>
          |  <url>https://realty.yandex.ru</url>
          |    <currencies>
          |      <currency id="RUB"/>
          |    </currencies>
          |    <categories>
          |      <category id="1" parentId="0">Недвижимость</category>
          |    </categories>
          |    <offers>
     """.stripMargin
  }

  "Correct write footer" in {
    feed2GISBuilder.footer shouldBe
      s"""</offers>
          </shop>
          </torg_price>"""
  }

  "Correct write body" in {
    feed2GISBuilder
      .processOffer(offerMoscowApartmentSell)
      .getOrElse("") shouldBe
      s"""<offer id="6302897158613069191" available="">
         <url>https://realty.test.vertis.yandex.ru/offer/6302897158613069191/</url>
         <price>6918980</price>
         <oldprice></oldprice>
         <currencyId>RUB</currencyId>
         <categoryId>1</categoryId>
         <picture>https://avatars.mds.yandex.net/get-realty/898673/REALTY-15185-3/app_snippet_large</picture>
         <typePrefix>flat</typePrefix>
         <vendor>3-комнатная квартира</vendor>
         <model>91,4 м²</model>
         <description></description>
     </offer>"""
  }

}
