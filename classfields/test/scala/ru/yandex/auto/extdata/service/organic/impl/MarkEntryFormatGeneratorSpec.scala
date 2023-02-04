package ru.yandex.auto.extdata.service.organic.impl

import cats.syntax.option._
import com.yandex.yoctodb.query.Select
import org.junit.runner.RunWith
import org.scalatest.{FlatSpecLike, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.catalog.model.ConfigurationImpl
import ru.yandex.auto.core.model.{ShortReview, ShortReviewsTree}
import ru.yandex.auto.core.region.{Region, RegionService}
import ru.yandex.auto.eds.catalog.cars.structure.ConfigurationStructure
import ru.yandex.auto.eds.service.cars.CarsCatalogGroupingService
import ru.yandex.auto.extdata.service.organic.LandingTextBuilder.TextRequest
import ru.yandex.auto.extdata.service.organic.model.CarsLandingSource.MarkSource
import ru.yandex.auto.extdata.service.organic.model.LandingStat.{
  MutableGeoStat,
  MutableMarkStat,
  MutableModelStat,
  MutableSubcategoryStat
}
import ru.yandex.auto.extdata.service.organic.model.{LandingEntryFormat, LandingStat, LandingThumbFormat, LandingUrl}
import ru.yandex.auto.extdata.service.organic.{LandingTextBuilder, LandingUrlService}
import ru.yandex.auto.extdata.service.util.MockitoSyntax._
import ru.yandex.auto.extdata.service.util.UrlUtils._
import ru.yandex.auto.searcher.catalog.MinMax
import ru.yandex.auto.searcher.configuration.CoreSearchConfiguration
import ru.yandex.auto.traffic.utils.LandingConstants
import ru.yandex.auto.wizard.AdSnippet
import ru.yandex.auto.wizard.yocto.search.{WizardYoctoQueryBuilder, WizardYoctoSearcher}
import ru.yandex.auto.wizard.yocto.utils.SearchResult
import ru.yandex.vertis.mockito.MockitoSupport

import java.util.Optional
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

@RunWith(classOf[JUnitRunner])
class MarkEntryFormatGeneratorSpec extends FlatSpecLike with MockitoSupport with Matchers {
  private val regionId = 213L
  private val mark = "kia"
  private val title = "title"
  private val image = "image"

  private def reviewUrl(model: String) = s"review_$model"
  private def modelName(model: String) = s"model_name_$model"
  private def adSnippetName(adSnippet: AdSnippet) = s"ad_snippet_${adSnippet.id}"

  private def models(max: Int) = (1 to max).map { ind =>
    Seq.fill(ind)("a").mkString
  }

  private def sampleAdSnippet(id: String, model: String, image: String): AdSnippet =
    new AdSnippet(
      id = id,
      hash = "",
      mark = mark,
      model = model,
      image = image,
      imagesWithAngles = Map.empty,
      price = 100,
      year = 2022,
      run = 0,
      creationDate = 0,
      colorCode = "",
      imagesCount = 1,
      ownersCount = 0,
      isCommercialSeller = false,
      geoIds = Seq(),
      dealerId = None,
      category = LandingConstants.CarsCategory,
      relevance = Some(1)
    )

  private val urlService = mock[LandingUrlService]
  when(urlService.reviews(eq(mark), ?, ?)).answer { invocationMock =>
    val model = invocationMock.getArgument[Option[String]](1).get
    Some(reviewUrl(model))
  }
  when(
    urlService.listing(eq(Some(regionId)), eq(Some(mark)), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  ).answer { invocation =>
    val maybeModel = Option(invocation.getArgument[Option[String]](2)).flatten
    val snippetId = Option(invocation.getArgument[Option[String]](3)).flatten
    val isMobile = invocation.getArgument[Boolean](5)
    landingListingUrl(
      modelName = maybeModel,
      snippetId = snippetId,
      isMobile = isMobile
    ).some
  }

  private val region = mock[Region]
  private val regionService = mock[RegionService]
  when(regionService.getRegionById(regionId.toInt)).thenReturn(Optional.of(region))

  private val textBuilder = mock[LandingTextBuilder]
  when(textBuilder.build(?)).answer { invocation =>
    invocation.getArgument[TextRequest](0) match {
      case _: TextRequest.Mark                   => title
      case TextRequest.MarkName(mark)            => mark
      case TextRequest.ModelSnippet(_, _, model) => modelName(model)
      case TextRequest.OfferSnippet(adSnippet)   => adSnippetName(adSnippet)
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
  when(shortReviewsTree.getReviewsByMarkAndModel(eq(mark), ?)).answer { invocation =>
    val modelName = invocation.getArgument[String](1)
    List.fill(modelName.length)(shortReview)
  }

  private val wizardYoctoSearcher = mock[WizardYoctoSearcher]

  private val select = mock[Select]
  private val queryBuilder = mock[WizardYoctoQueryBuilder[CoreSearchConfiguration]]
  when(queryBuilder.buildYoctoQuery(?)).thenReturn(select)

  private val generator = new MarkEntryFormatGenerator(
    urlService,
    regionService,
    textBuilder,
    groupingService,
    shortReviewsTree
  )

  private def buildStatWithModels(source: MarkSource, maxModels: Int): LandingStat = {
    val markStat = new MutableMarkStat
    models(maxModels).foreach { model =>
      val modelStat = new MutableModelStat
      markStat.models.put(model, modelStat)
      modelStat.configurationCount.put(1, 1)
      modelStat.addPrice(source, model.length)
      (1 to model.length).foreach(_ => modelStat.inc(source))
      if (source.geoId != LandingConstants.Russia) {
        val russiaSource = source.copy(geoId = LandingConstants.Russia)
        modelStat.addPrice(russiaSource, model.length)
        (1 to model.length).foreach(_ => modelStat.inc(russiaSource))
      }
    }

    val categoryStat = new MutableSubcategoryStat
    categoryStat.marks.put(mark, markStat)

    val geoStat = new MutableGeoStat
    geoStat.subCategories.put(LandingConstants.CarsCategory, categoryStat)

    val stat = new LandingStat
    stat.geoStat.put(regionId, geoStat)
    stat.geoStat.put(LandingConstants.Russia, geoStat)

    stat
  }

  private val nonCreditSource = MarkSource(
    regionId,
    mark,
    None
  )

  "MarkEntryFormatGenerator" should "build entry for correct data" in {
    val modelsNum = 30
    val entries = generator
      .entries(nonCreditSource, buildStatWithModels(nonCreditSource, modelsNum))
      .toList
    val topModels = models(modelsNum).reverse
      .take(LandingConstants.MaxOffers)

    (entries should have).length(1)

    val entry = entries.head
    entry.title should matchPattern {
      case `title` =>
    }
    entry.urls should matchPattern {
      case List(`nonCreditDesktopLandingUrl`, `nonCreditMobileLandingUrl`) =>
      case List(`nonCreditMobileLandingUrl`, `nonCreditDesktopLandingUrl`) =>
    }
    entry.thumbs.zip(topModels).foreach {
      case (thumb, model) =>
        val name = modelName(model)
        val modelUrl = nonCreditModelUrl(model)
        val url = LandingUrl(modelUrl)
        val setsIds = Seq(nonCreditMobileLandingUrl, nonCreditDesktopLandingUrl).map(_.id)
        val price = model.length.toLong
        val offersCount = model.length.toLong
        val review = reviewUrl(model)
        val reviewsNum = model.length
        thumb should matchPattern {
          case LandingThumbFormat(
              `name`,
              `url`,
              `setsIds`,
              Seq(`image`),
              LandingConstants.CarsCategory,
              `offersCount`,
              Some(`price`),
              Some(`mark`),
              Some(`offersCount`),
              Some(`offersCount`),
              Some(`review`),
              Some(`reviewsNum`),
              None,
              None,
              None,
              true
              ) =>
        }
    }
  }

  it should "not build entries if there is not enough data for thumbs" in {
    val wizardYoctoSearcherSearchResult = SearchResult.Empty
    when(wizardYoctoSearcher.search(?)).thenReturn(wizardYoctoSearcherSearchResult)

    val entries = generator.entries(nonCreditSource, buildStatWithModels(nonCreditSource, 3)).toSeq
    entries shouldBe empty
  }
}
