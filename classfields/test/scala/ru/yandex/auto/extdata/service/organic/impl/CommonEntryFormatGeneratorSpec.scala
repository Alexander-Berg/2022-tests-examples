package ru.yandex.auto.extdata.service.organic.impl

import cats.syntax.option._
import org.junit.runner.RunWith
import org.scalatest.{FlatSpecLike, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.catalog.model.ConfigurationImpl
import ru.yandex.auto.core.model.{ShortReview, ShortReviewsTree}
import ru.yandex.auto.core.region.{Region, RegionService}
import ru.yandex.auto.eds.catalog.cars.structure.ConfigurationStructure
import ru.yandex.auto.eds.service.cars.CarsCatalogGroupingService
import ru.yandex.auto.extdata.service.organic.LandingTextBuilder.TextRequest
import ru.yandex.auto.extdata.service.organic.builder.IdBuilder
import ru.yandex.auto.extdata.service.organic.model.CarsLandingSource.CommonSource
import ru.yandex.auto.extdata.service.organic.model.LandingStat.{
  MutableGeoStat,
  MutableMarkStat,
  MutableModelStat,
  MutableSubcategoryStat
}
import ru.yandex.auto.extdata.service.organic.model.{LandingStat, LandingThumbFormat, LandingUrl}
import ru.yandex.auto.extdata.service.organic.{LandingTextBuilder, LandingUrlService}
import ru.yandex.auto.extdata.service.util.MockitoSyntax._
import ru.yandex.auto.extdata.service.util.UrlUtils._
import ru.yandex.auto.traffic.utils.LandingConstants
import ru.yandex.vertis.mockito.MockitoSupport

import java.util.Optional
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class CommonEntryFormatGeneratorSpec extends FlatSpecLike with MockitoSupport with Matchers {
  private val reviewsUrl = "reviewsUrl"
  private val indexUrl = LandingUrl("indexUrl")
  private val regionId = 213L
  private val title = "title"
  private val image = "image"
  private val bodyType = "bodyType"
  private val year = 2000
  private val priceTo = 1000000
  private def marksNames(max: Int) = (1 to max).map { ind =>
    Seq.fill(ind)("a").mkString
  }

  private val urlService = mock[LandingUrlService]
  when(urlService.listing(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)).answer { invocation =>
    val maybeMark = Option(invocation.getArgument[Option[String]](1)).flatten
    val isMobile = invocation.getArgument[Boolean](5)
    landingListingUrl(
      markName = maybeMark,
      modelName = None,
      isMobile = isMobile
    ).some
  }
  when(urlService.reviews(?, ?, eq(null))).thenReturn(Some(reviewsUrl))
  when(urlService.index(?, ?)).thenReturn(Some(indexUrl))

  private val region = mock[Region]
  private val regionService = mock[RegionService]
  when(regionService.getRegionById(?)).thenReturn(Optional.of(region))

  private def markTitle(markName: String) = s"title_$markName"
  private def modelTitle(modelName: String) = s"title_$modelName"

  private val textBuilder = mock[LandingTextBuilder]
  when(textBuilder.build(?)).answer { invocation =>
    invocation.getArgument[TextRequest](0) match {
      case _: TextRequest.Common                 => title
      case TextRequest.MarkSnippet(_, mark)      => markTitle(mark)
      case TextRequest.MarkName(mark)            => markTitle(mark)
      case TextRequest.ModelSnippet(_, _, model) => modelTitle(model)
    }
  }

  private val configurationImpl = mock[ConfigurationImpl]
  when(configurationImpl.getMobileWizardPhotosRetina).thenReturn(Map(LandingConstants.WizPhoto -> image).asJava)
  private val configurationStructure = mock[ConfigurationStructure]
  when(configurationStructure.entity).thenReturn(configurationImpl)
  private val groupingService = mock[CarsCatalogGroupingService]
  when(groupingService.buildGroupByConfiguration(?)).thenReturn(Some(configurationStructure))

  private val shortReview = mock[ShortReview]
  private val shortReviewsTree = mock[ShortReviewsTree]
  when(shortReviewsTree.getReviewsForMark(?)).answer { invocation =>
    val markName = invocation.getArgument[String](0)
    List.fill(markName.length)(shortReview)
  }
  when(shortReviewsTree.getReviewsByMarkAndModel(?, ?)).answer { invocation =>
    val markName = invocation.getArgument[String](0)
    List.fill(markName.length)(shortReview)
  }

  private def buildStatWithMarks(source: CommonSource, marksNum: Int): LandingStat = {
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
      modelStat.inc(source)
      modelStat.addPrice(source, markName.length)
      markStat.models.put("model", modelStat)
      modelStat.configurationCount.put(1, 1)
    }
    val geoStat = new MutableGeoStat
    geoStat.subCategories.put(LandingConstants.CarsCategory, categoryStat)

    val stat = new LandingStat
    stat.geoStat.put(source.geoId, geoStat)
    stat.geoStat.put(LandingConstants.Russia, geoStat)

    stat
  }

  private val generator = new CommonEntryFormatGenerator(
    urlService,
    regionService,
    textBuilder,
    groupingService,
    shortReviewsTree
  )

  private val nonCreditSource = CommonSource(
    regionId,
    Some(true),
    Some(bodyType),
    Some(year),
    Some(priceTo)
  )

  "CommonEntryFormatGenerator" should "build entry for correct data" in {
    val marksNum = 30
    val stat = buildStatWithMarks(nonCreditSource, marksNum)
    val entries = generator.entries(nonCreditSource, stat).toSeq
    (entries should have).length(1)
    val entry = entries.head
    entry.title should matchPattern {
      case `title` =>
    }
    entry.urls should matchPattern {
      case List(`nonCreditDesktopLandingUrl`, `nonCreditMobileLandingUrl`) =>
      case List(`nonCreditMobileLandingUrl`, `nonCreditDesktopLandingUrl`) =>
    }
    (entry.thumbs should have).length(LandingConstants.MaxOffers)
    val topMarks = marksNames(marksNum).reverse.take(LandingConstants.MaxOffers)
    entry.thumbs.zip(topMarks).foreach {
      case (thumb, markName) =>
        val id = IdBuilder.id(thumb.url.url)
        val url = LandingUrl(nonCreditMarkUrl(markName), id)
        val setsIds = entry.urls.map(lu => IdBuilder.id(lu.url))

        thumb should matchPattern {
          case LandingThumbFormat(
              _,
              _,
              `setsIds`,
              Seq(`image`),
              LandingConstants.CarsCategory,
              _,
              _,
              _,
              _,
              _,
              Some(`reviewsUrl`),
              _,
              None,
              None,
              None,
              true
              ) =>
        }
    }
  }

  // TODO: this test should pass in https://st.yandex-team.ru/VERTISTRAF-2579
  (it should "build two entries for Russia geoId").ignore {
    val russiaSource = nonCreditSource.copy(geoId = LandingConstants.Russia)
    val stat = buildStatWithMarks(russiaSource, 30)
    val entries = generator.entries(russiaSource, stat).toSeq
    (entries should have).length(2)
  }

  it should "not build entries for geo with less than 3 marks" in {
    val unfortunateSource = nonCreditSource.copy(geoId = 13)
    val stat = buildStatWithMarks(unfortunateSource, 2)

    val entries = generator.entries(unfortunateSource, stat).toSeq
    entries shouldBe empty
  }
}
