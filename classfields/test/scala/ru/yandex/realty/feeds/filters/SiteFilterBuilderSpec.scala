package ru.yandex.realty.feeds.filters

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.common.util.IOUtils
import ru.yandex.realty.context.v2.AuctionResultStorage
import ru.yandex.realty.feeds.SitesQueries
import ru.yandex.realty.graph.serialize.RegionGraphProtoConverter
import ru.yandex.realty.model.feed.FeedType
import ru.yandex.realty.model.message.ExtDataSchema
import ru.yandex.realty.model.message.ExtDataSchema.AuctionResultMessage
import ru.yandex.realty.model.serialization.SiteProtoConverter
import ru.yandex.realty.model.serialization.auction.AuctionResultProtoConverter
import ru.yandex.realty.sites.stat.SiteInfoStorage
import ru.yandex.realty.sites.stat.SiteInfoStorage.fromInputStream

import java.io.InputStream

@RunWith(classOf[JUnitRunner])
class SiteFilterBuilderSpec extends WordSpec with MockFactory with Matchers {

  val regionGraph = RegionGraphProtoConverter.deserialize(
    IOUtils.gunzip(
      getClass.getClassLoader.getResourceAsStream("region_graph-8-2.data")
    )
  )

  val siteInfoStorage: SiteInfoStorage =
    fromInputStream(IOUtils.gunzip(getClass.getClassLoader.getResourceAsStream("site_info_storage.data")))

  val auctionResultStorage: AuctionResultStorage = {
    val partners = getDataFromFile("auction_result.data")(AuctionResultMessage.parseDelimitedFrom)(
      AuctionResultProtoConverter.fromMessage
    )
    new AuctionResultStorage(partners)
  }

  val sites =
    getDataFromFile("sites.data")(ExtDataSchema.SiteMessage.parseDelimitedFrom)(SiteProtoConverter.fromMessage)

  val siteFilterBuilder = new SiteFilterBuilder(() => regionGraph, siteInfoStorage, () => auctionResultStorage)

  private def getDataFromFile[A, B](filename: String)(parse: InputStream => A)(convert: A => B): Seq[B] = {
    val is = IOUtils.gunzip(getClass.getClassLoader.getResourceAsStream(filename))
    Iterator
      .continually(parse(is))
      .takeWhile(_ != null)
      .map(convert)
      .toSeq
  }

  "SiteFilter" should {
    "build price filter and correctly apply it" in {
      val query = SitesQueries
        .empty("test", FeedType.AdWords)
        .copy(priceFrom = Some(300000), priceTo = Some(19000000), regionIds = Seq(11162))
      val siteFilter = siteFilterBuilder.build(query)
      val result = sites.filter(s => siteFilter.filter(s)).map(_.getName).sorted
      result.size should equal(2)
      result should equal(List("Шишимская горка", "Южные кварталы"))
    }
  }
}
