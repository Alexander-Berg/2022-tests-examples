package ru.yandex.auto.extdata.service.organic.impl

import org.junit.runner.RunWith
import org.scalatest.{FlatSpecLike, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.dealer.DealerInfo
import ru.yandex.auto.core.region.{Region, RegionService}
import ru.yandex.auto.extdata.service.organic.LandingTextBuilder.TextRequest
import ru.yandex.auto.extdata.service.organic.builder.IdBuilder
import ru.yandex.auto.extdata.service.organic.impl.dealers.DealersMarkEntryGenerator
import ru.yandex.auto.extdata.service.organic.model.CarsLandingSource.DealersMarkSource
import ru.yandex.auto.extdata.service.organic.model.LandingStat.{
  MutableDealerStat,
  MutableGeoStat,
  MutableMarkStat,
  MutableSubcategoryStat
}
import ru.yandex.auto.extdata.service.organic.model.{LandingEntryFormat, LandingStat, LandingThumbFormat, LandingUrl}
import ru.yandex.auto.extdata.service.organic.{LandingTextBuilder, LandingUrlService}
import ru.yandex.auto.extdata.service.util.MockitoSyntax._
import ru.yandex.auto.searcher.search.dealers.DealerSearcher
import ru.yandex.auto.traffic.utils.LandingConstants
import ru.yandex.vertis.mockito.MockitoSupport

import java.util.Optional
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class DealersMarkEntryFormatGeneratorSpec extends FlatSpecLike with MockitoSupport with Matchers {
  private val mainUrl = "url"
  private val landingMainUrl = LandingUrl(mainUrl)
  private val mobileUrl = "mobileUrl"
  private val landingMobileUrl = LandingUrl(mobileUrl)
  private def dealerUrl(name: String) = s"dealer_url_$name"
  private def markName(mark: String) = s"mark_name_$mark"
  private val regionId = 213L
  private val mark = "kia"
  private val title = "title"

  private val urlService = mock[LandingUrlService]
  when(urlService.dealers(eq(regionId), eq(Some(mark)), ?, ?)).answer { invocationMock =>
    val isMobile = invocationMock.getArgument[Boolean](3)
    if (isMobile)
      Some(mobileUrl)
    else
      Some(mainUrl)
  }
  when(urlService.dealer(?, ?, ?, ?, ?)).answer { invocationMock =>
    val restName = invocationMock.getArgument[String](0)
    Some(dealerUrl(restName))
  }

  private val region = mock[Region]
  private val regionService = mock[RegionService]
  when(regionService.getRegionById(regionId.toInt)).thenReturn(Optional.of(region))

  private val textBuilder = mock[LandingTextBuilder]
  when(textBuilder.build(?)).answer { invocation =>
    invocation.getArgument[TextRequest](0) match {
      case _: TextRequest.MarkDealers => title
      case TextRequest.MarkName(mark) => markName(mark)
    }
  }

  private val mockDealers = {
    (1 to 20)
      .map { ind =>
        val dealerInfo = mock[DealerInfo]
        val marks = (1 to ind).map(id => s"mark_$id").toList.asJava
        when(dealerInfo.getRestName).thenReturn(s"dealer_$ind")
        when(dealerInfo.getRegions).thenReturn(List(Integer.valueOf(regionId.toInt)).asJava)
        when(dealerInfo.hasAds).thenReturn(true)
        when(dealerInfo.getMainWizardPhoto).thenReturn(s"image_$ind")
        when(dealerInfo.getOrgType).thenReturn("1")
        when(dealerInfo.getFullRussianName).thenReturn(s"russian_dealer_$ind")
        when(dealerInfo.getMarks).thenReturn(marks)
        when(dealerInfo.getId).thenReturn(ind)
        when(dealerInfo.getOffersCount).thenReturn(ind)
        dealerInfo
      }
  }

  private val searcher = mock[DealerSearcher]
  when(searcher.searchDealersWithoutLimit(?)).thenReturn(mockDealers.asJava)

  private val generator = new DealersMarkEntryGenerator(
    urlService,
    regionService,
    textBuilder,
    searcher
  )

  private val source = DealersMarkSource(regionId, mark)

  private val stat = new LandingStat
  mockDealers.foreach { info =>
    val dealerStat = stat.geoStat
      .getOrElseUpdate(regionId, new MutableGeoStat)
      .subCategories
      .getOrElseUpdate(LandingConstants.CarsCategory, new MutableSubcategoryStat)
      .marks
      .getOrElseUpdate(mark, new MutableMarkStat)
      .dealers
      .getOrElseUpdate(info.getId, new MutableDealerStat)
    dealerStat.addPrice(info.getId.toInt)
    (1 to info.getOffersCount).foreach(_ => dealerStat.inc(true))
  }

  "DealersEntryFormatGenerator" should "build entry for correct data" in {
    val entries = generator.entries(source, stat).toSeq
    (entries should have).length(1)
    val entry = entries.head
    entry should matchPattern {
      case LandingEntryFormat(
          List(`landingMainUrl`, `landingMobileUrl`),
          `title`,
          _,
          _,
          _
          ) =>
    }
    entry.thumbs.zip(mockDealers).foreach {
      case (thumb, info) =>
        val url = LandingUrl(dealerUrl(info.getRestName))
        val name = info.getFullRussianName
        val setsIds = Seq(mainUrl, mobileUrl).map(IdBuilder.id(_))
        val img = s"image_${info.getId}"
        val price = info.getId
        val marks = info.getMarks.asScala.map(markName).mkString(",")
        val offersCount = info.getOffersCount.toLong
        thumb should matchPattern {
          case LandingThumbFormat(
              `name`,
              `url`,
              `setsIds`,
              Seq(`img`),
              LandingConstants.CarsCategory,
              _,
              Some(`price`),
              Some(`marks`),
              Some(`offersCount`),
              Some(`offersCount`),
              None,
              None,
              None,
              None,
              None,
              true
              ) =>
        }
    }
  }

}
