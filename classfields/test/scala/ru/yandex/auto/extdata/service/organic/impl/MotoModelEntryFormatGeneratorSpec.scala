package ru.yandex.auto.extdata.service.organic.impl

import com.yandex.yoctodb.query.Select
import org.junit.runner.RunWith
import org.scalatest.{FlatSpecLike, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.region.{Region, RegionService}
import ru.yandex.auto.extdata.service.organic.LandingTextBuilder.TextRequest
import ru.yandex.auto.extdata.service.organic.builder.IdBuilder
import ru.yandex.auto.extdata.service.organic.model.MotoLandingSource.MotoMarkModelSource
import ru.yandex.auto.extdata.service.organic.model.{LandingEntryFormat, LandingStat, LandingThumbFormat, LandingUrl}
import ru.yandex.auto.extdata.service.organic.{LandingTextBuilder, LandingUrlService}
import ru.yandex.auto.extdata.service.util.MockitoSyntax._
import ru.yandex.auto.searcher.configuration.WizardSearchConfiguration
import ru.yandex.auto.wizard.AdSnippet
import ru.yandex.auto.wizard.yocto.search.{WizardYoctoQueryBuilder, WizardYoctoSearcher}
import ru.yandex.auto.wizard.yocto.utils.SearchResult
import ru.yandex.vertis.mockito.MockitoSupport

import java.util.Optional

@RunWith(classOf[JUnitRunner])
class MotoModelEntryFormatGeneratorSpec extends FlatSpecLike with MockitoSupport with Matchers {

  private val mainUrl = "url"
  private val landingUrl = LandingUrl(mainUrl)
  private val mobileUrl = "mobileUrl"
  private val landingMobileUrl = LandingUrl(mobileUrl)
  private val title = "title"
  private val regionId = 213L
  private val category = "snowmobile"
  private val mark = "tomoto"
  private val model = "TM150"
  private val snippetName = "snippetName"
  private val image = "image"
  private val price = 100000
  private val id = "id"

  private val source = MotoMarkModelSource(regionId, category, mark, model, None)

  private val urlService = mock[LandingUrlService]
  when(
    urlService.listing(eq(Some(regionId)), eq(Some(mark)), eq(Some(model)), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  ).answer { invocation =>
    (if (invocation.getArgument[Boolean](5)) Some(mobileUrl) else Some(mainUrl))
      .map(url => LandingUrl(url))
  }

  private val landingStat = mock[LandingStat]

  private val region = mock[Region]
  private val regionService = mock[RegionService]
  when(regionService.getRegionById(regionId.toInt)).thenReturn(Optional.of(region))

  private val textBuilder = mock[LandingTextBuilder]
  when(textBuilder.build(?)).answer { invocateion =>
    invocateion.getArgument[TextRequest](0) match {
      case _: TextRequest.MotoMarkModel => title
      case _: TextRequest.OfferSnippet  => snippetName
      case _: TextRequest.MotoMarkName  => mark
    }
  }

  private val adSnippet = mock[AdSnippet]
  when(adSnippet.id).thenReturn(id)
  when(adSnippet.mark).thenReturn(mark)
  when(adSnippet.image).thenReturn(image)
  when(adSnippet.category).thenReturn(category)
  when(adSnippet.price).thenReturn(price)
  when(adSnippet.relevance).thenReturn(Some(0L))
  private val snippets = collection.mutable.Buffer(adSnippet, adSnippet, adSnippet, adSnippet)
  private val wizardYoctoSearcher = mock[WizardYoctoSearcher]
  private val searchResult = mock[SearchResult]
  when(searchResult.ads).thenReturn(snippets)

  private val select = mock[Select]
  private val motoQueryBuilder = mock[WizardYoctoQueryBuilder[WizardSearchConfiguration]]
  when(motoQueryBuilder.buildYoctoQuery(?)).thenReturn(select)
  when(wizardYoctoSearcher.search(?)).thenReturn(searchResult)

  private val motoGenerator = new MotoModelEntryFormatGenerator(
    urlService,
    regionService,
    textBuilder,
    wizardYoctoSearcher,
    motoQueryBuilder
  )

  "MotoModelEntryFormatGenerator" should "build entry for correct data" in {
    val entries = motoGenerator.entries(source, landingStat).toSeq
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

    entry.thumbs.foreach { thumb =>
      val setsIds = Seq(mainUrl, mobileUrl).map(IdBuilder.id(_))
      thumb should matchPattern {
        case LandingThumbFormat(
            `snippetName`,
            `landingUrl`,
            `setsIds`,
            Seq(`image`),
            `category`,
            _,
            Some(`price`),
            Some(`mark`),
            None,
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
