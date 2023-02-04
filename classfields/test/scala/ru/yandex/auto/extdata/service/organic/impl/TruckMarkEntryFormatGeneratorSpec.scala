package ru.yandex.auto.extdata.service.organic.impl

import com.yandex.yoctodb.query.Select
import org.junit.runner.RunWith
import org.scalatest.{FlatSpecLike, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.catalog.model.trucks.TrucksModelImpl
import ru.yandex.auto.core.region.Region
import ru.yandex.auto.eds.service.RegionService
import ru.yandex.auto.eds.service.trucks.TrucksCatalogGroupingService
import ru.yandex.auto.eds.service.trucks.tree.node.ModelNode
import ru.yandex.auto.extdata.service.organic.LandingTextBuilder.TextRequest
import ru.yandex.auto.extdata.service.organic.builder.IdBuilder
import ru.yandex.auto.extdata.service.organic.model.LandingStat.{
  MutableGeoStat,
  MutableMarkStat,
  MutableModelStat,
  MutableSubcategoryStat
}
import ru.yandex.auto.extdata.service.organic.model.TruckLandingSource.TruckMarkSource
import ru.yandex.auto.extdata.service.organic.model.{LandingEntryFormat, LandingStat, LandingThumbFormat, LandingUrl}
import ru.yandex.auto.extdata.service.organic.{LandingTextBuilder, LandingUrlService}
import ru.yandex.auto.extdata.service.util.MockitoSyntax._
import ru.yandex.auto.searcher.catalog.MinMax
import ru.yandex.auto.searcher.configuration.WizardSearchConfiguration
import ru.yandex.auto.traffic.utils.LandingConstants
import ru.yandex.auto.wizard.AdSnippet
import ru.yandex.auto.wizard.yocto.search.{WizardYoctoQueryBuilder, WizardYoctoSearcher}
import ru.yandex.auto.wizard.yocto.utils.SearchResult
import ru.yandex.vertis.mockito.MockitoSupport

import java.util.Optional
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

@RunWith(classOf[JUnitRunner])
class TruckMarkEntryFormatGeneratorSpec extends FlatSpecLike with MockitoSupport with Matchers {
  private val mainUrl = "url"
  private val landingMainUrl = LandingUrl(mainUrl)
  private val mobileUrl = "mobileUrl"
  private val landingMobileUrl = LandingUrl(mobileUrl)
  private val regionId = 213L
  private val mark = "kia"
  private val title = "title"
  private val image = "image"

  private def modelUrl(model: String, snippetId: Option[String]) = s"model_url_${model}_$snippetId"
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
      category = LandingConstants.TruckCategory,
      relevance = Some(1)
    )

  private val urlService = mock[LandingUrlService]
  when(
    urlService.listing(eq(Some(regionId)), eq(Some(mark)), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  ).answer { invocation =>
    (invocation.getArgument[Option[String]](2) match {
      case Some(model) =>
        invocation.getArgument[Option[String]](3) match {
          case snippetId @ Some(_) => Some(modelUrl(model, snippetId))
          case None | null         => Some(modelUrl(model, None))
        }
      case None | null =>
        if (invocation.getArgument[Boolean](5)) Some(mobileUrl) else Some(mainUrl)
    }).map(url => LandingUrl(url))
  }

  private val region = mock[Region]
  private val regionService = mock[RegionService]
  when(regionService.getRegionById(regionId.toInt)).thenReturn(Optional.of(region))

  private val textBuilder = mock[LandingTextBuilder]
  when(textBuilder.build(?)).answer { invocation =>
    invocation.getArgument[TextRequest](0) match {
      case _: TextRequest.Mark                        => title
      case TextRequest.TruckMark(_, _, mark)          => mark
      case TextRequest.TruckMarkName(mark)            => mark
      case TextRequest.TruckModelSnippet(_, _, model) => modelName(model)
      case TextRequest.OfferSnippet(adSnippet)        => adSnippetName(adSnippet)
    }
  }

  private val trucksModelImpl = mock[TrucksModelImpl]
  when(trucksModelImpl.getMobileWizardRetinaPhotos).thenReturn(Map(LandingConstants.TrucksPhoto -> image).asJava)
  private val modelNode = mock[ModelNode]
  when(modelNode.value).thenReturn(trucksModelImpl)
  private val groupingService = mock[TrucksCatalogGroupingService]
  when(groupingService.findModel(?, ?)).thenReturn(Some(modelNode))

  private val wizardYoctoSearcher = mock[WizardYoctoSearcher]

  private val select = mock[Select]
  private val queryBuilder = mock[WizardYoctoQueryBuilder[WizardSearchConfiguration]]
  when(queryBuilder.buildYoctoQuery(?)).thenReturn(select)

  private def buildStatWithModels(source: TruckMarkSource, maxModels: Int): LandingStat = {
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
    geoStat.subCategories.put(LandingConstants.TruckCategory, categoryStat)

    val stat = new LandingStat
    stat.geoStat.put(regionId, geoStat)
    stat.geoStat.put(LandingConstants.Russia, geoStat)

    stat
  }

  private val source = TruckMarkSource(
    regionId,
    LandingConstants.TruckCategory,
    mark,
    None
  )

  private val generator = new TruckMarkEntryFormatGenerator(
    urlService,
    regionService,
    textBuilder,
    groupingService,
    wizardYoctoSearcher,
    queryBuilder
  )

  "TruckMarkEntryFormatGenerator" should "build entries for correct data" in {
    val entries = generator.entries(source, buildStatWithModels(source, 20)).toSeq
    (entries should have).length(1)
    val entry = entries.head
    entry should matchPattern {
      case LandingEntryFormat(
          List(`landingMainUrl`, `landingMobileUrl`),
          `mark`,
          _,
          _,
          _
          ) =>
    }

    val topModels = models(20).reverse.take(LandingConstants.MaxOffers)
    entry.thumbs.zip(topModels).foreach {
      case (thumb, model) =>
        val name = modelName(model)
        val url = modelUrl(model, None)
        val modelLandingUrl = LandingUrl(url)
        val setsIds = Seq(mainUrl, mobileUrl).map(IdBuilder.id(_))
        val price = model.length.toLong
        val offersCount = model.length.toLong
        thumb should matchPattern {
          case LandingThumbFormat(
              `name`,
              `modelLandingUrl`,
              `setsIds`,
              Seq(`image`),
              LandingConstants.TruckCategory,
              _,
              Some(`price`),
              Some(`mark`),
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

  it should "not build entries if there is not enough data for thumbs" in {
    val wizardYoctoSearcherSearchResult = SearchResult.Empty
    when(wizardYoctoSearcher.search(?)).thenReturn(wizardYoctoSearcherSearchResult)

    val entries = generator.entries(source, buildStatWithModels(source, 3)).toSeq
    entries shouldBe empty
  }

  it should "build offer entries for marks with less than 4 models, but 4 or more offers" in {
    val wizardYoctoSearcherSearchResult = new SearchResult(
      ArrayBuffer(
        sampleAdSnippet("0", "a", "image_0"),
        sampleAdSnippet("1", "b", "image_1"),
        sampleAdSnippet("2", "c", "image_2"),
        sampleAdSnippet("3", "d", "image_3")
      ),
      new MinMax(0, 0),
      0,
      0
    )
    when(wizardYoctoSearcher.search(?)).thenReturn(wizardYoctoSearcherSearchResult)

    val entries = generator.entries(source, buildStatWithModels(source, 3)).toSeq

    (entries should have).length(1)
    val entry = entries.head
    entry should matchPattern {
      case LandingEntryFormat(
          List(`landingMainUrl`, `landingMobileUrl`),
          `mark`,
          _,
          _,
          _
          ) =>
    }

    entry.thumbs.zip(wizardYoctoSearcherSearchResult.ads).foreach {
      case (thumb, adSnippet) =>
        val name = textBuilder.build(TextRequest.OfferSnippet(adSnippet))
        val url = modelUrl(adSnippet.model, Some(adSnippet.id))
        val landingUlr = LandingUrl(url)
        val setsIds = Seq(mainUrl, mobileUrl).map(IdBuilder.id(_))

        thumb should matchPattern {
          case LandingThumbFormat(
              `name`,
              `landingUlr`,
              `setsIds`,
              Seq(adSnippet.image),
              LandingConstants.TruckCategory,
              _,
              Some(adSnippet.price),
              Some(`mark`),
              None,
              None,
              None,
              None,
              None,
              None,
              None,
              false
              ) =>
        }
    }
  }
}
