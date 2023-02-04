package ru.yandex.auto.extdata.service.organic.impl

import org.junit.runner.RunWith
import org.scalatest.{FlatSpecLike, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.catalog.model.moto.MotoModelImpl
import ru.yandex.auto.core.region.{Region, RegionService}
import ru.yandex.auto.eds.service.moto.MotoCatalogGroupingService
import ru.yandex.auto.extdata.service.organic.LandingTextBuilder.TextRequest
import ru.yandex.auto.extdata.service.organic.builder.IdBuilder
import ru.yandex.auto.extdata.service.organic.model.LandingStat.{
  MutableGeoStat,
  MutableMarkStat,
  MutableModelStat,
  MutableSubcategoryStat
}
import ru.yandex.auto.extdata.service.organic.model.MotoLandingSource.MotoCategorySource
import ru.yandex.auto.extdata.service.organic.model.{LandingEntryFormat, LandingStat, LandingThumbFormat, LandingUrl}
import ru.yandex.auto.extdata.service.organic.{LandingTextBuilder, LandingUrlService}
import ru.yandex.auto.extdata.service.util.MockitoSyntax._
import ru.yandex.auto.traffic.utils.LandingConstants
import ru.yandex.vertis.mockito.MockitoSupport

import java.util.Optional
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class MotoCategoryEntryFormatGeneratorSpec extends FlatSpecLike with MockitoSupport with Matchers {

  private val regionId = 213L
  private val category = "snowmobile"
  private val mainUrl = "url"
  private val landingUrl = LandingUrl(mainUrl)
  private val mobileUrl = "mobileUrl"
  private val landingMobileUrl = LandingUrl(mobileUrl)
  private val title = "title"
  private val image = "image"
  private def marksNames(max: Int) =
    (1 to max)
      .map(n => Seq.fill(n)("a").mkString)

  private def markUrl(mark: String) = s"mark_url_$mark"
  private def markName(mark: String) = s"mark_name_$mark"

  private val source = MotoCategorySource(regionId, category, None)

  private val urlService = mock[LandingUrlService]
  when(urlService.listing(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?))
    .answer { invocation =>
      (invocation.getArgument[Option[String]](1) match {
        case Some(mark) => Some(markUrl(mark))
        case None | null =>
          if (invocation.getArgument[Boolean](5)) Some(mobileUrl) else Some(mainUrl)
      }).map(url => LandingUrl(url))
    }

  private val region = mock[Region]
  private val regionService = mock[RegionService]
  when(regionService.getRegionById(regionId.toInt)).thenReturn(Optional.of(region))

  private val landingTextBuilder = mock[LandingTextBuilder]
  when(landingTextBuilder.build(?)).answer { invocation =>
    invocation.getArgument[TextRequest](0) match {
      case _: TextRequest.MotoCategory          => title
      case TextRequest.MotoMarkSnippet(_, mark) => markName(mark)
    }
  }

  private val motoModelImpl = mock[MotoModelImpl]
  when(motoModelImpl.getCode).thenReturn("TM150")
  when(motoModelImpl.getMobileWizardRetinaPhotos).thenReturn(Map(LandingConstants.MotoPhoto -> image).asJava)
  private val groupingService = mock[MotoCatalogGroupingService]
  when(groupingService.getSetModels(?)).thenReturn(Set(motoModelImpl))

  private val categoryGenerator = new MotoCategoryEntryFormatGenerator(
    urlService,
    regionService,
    landingTextBuilder,
    groupingService
  )

  private def buildStatWithMarks(source: MotoCategorySource, marksNum: Int): LandingStat = {
    val categoryStat = new MutableSubcategoryStat
    marksNames(marksNum).foreach { markName =>
      val markStat = new MutableMarkStat
      categoryStat.marks.put(markName, markStat)
      markStat.addPrice(source, markName.length)
      (1 to markName.length).foreach(_ => markStat.inc(source))
      if (source.geoId != LandingConstants.Russia) {
        val russiaSource = source.copy(geoId = LandingConstants.Russia)
        markStat.addPrice(russiaSource, markName.length)
        (1 to markName.length).foreach(_ => markStat.inc(russiaSource))
      }
      val modelStat = new MutableModelStat
      markStat.models.put("TM150", modelStat)
      modelStat.configurationCount.put(1, 1)
    }

    val geoStat = new MutableGeoStat
    geoStat.subCategories.put("snowmobile", categoryStat)

    val stat = new LandingStat
    stat.geoStat.put(regionId, geoStat)
    stat.geoStat.put(LandingConstants.Russia, geoStat)
    stat
  }

  "MotoCategoryEntryFormatGenerator" should "build entry for correct data" in {
    val landingStat = buildStatWithMarks(source, 30)
    val entries = categoryGenerator.entries(source, landingStat).toSeq
    (entries should have).length(1)
    val entry = entries.head
    entry should matchPattern {
      case LandingEntryFormat(
          List(`landingUrl`, `landingMobileUrl`),
          `title`,
          _,
          _,
          _
          ) =>
    }

    val marks = marksNames(30).reverse.take(LandingConstants.MaxOffers)
    entry.thumbs.zip(marks).foreach {
      case (thumb, mark) =>
        val name = markName(mark)
        val url = LandingUrl(markUrl(mark))
        val setsIds = Seq(mainUrl, mobileUrl).map(IdBuilder.id(_))
        val price = mark.length.toLong
        val offersCount = mark.length.toLong
        thumb should matchPattern {
          case LandingThumbFormat(
              `name`,
              `url`,
              `setsIds`,
              Seq(`image`),
              `category`,
              _,
              Some(`price`),
              Some(`mark`),
              Some(`offersCount`),
              None,
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
