package ru.yandex.auto.extdata.service.organic.impl

import org.junit.runner.RunWith
import org.scalatest.{FlatSpecLike, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.catalog.model.moto.MotoModelImpl
import ru.yandex.auto.core.region.{Region, RegionService}
import ru.yandex.auto.eds.service.moto.MotoCatalogGroupingService
import ru.yandex.auto.extdata.service.organic.LandingUrlService
import ru.yandex.auto.extdata.service.organic.builder.IdBuilder
import ru.yandex.auto.extdata.service.organic.model.LandingUrl
import ru.yandex.auto.extdata.service.organic.model.MotoLandingSource.{
  MotoCategorySource,
  MotoMarkModelSource,
  MotoMarkSource
}
import ru.yandex.auto.extdata.service.util.MockitoSyntax.SugarStubbing
import ru.yandex.auto.wizard.AdSnippet
import ru.yandex.auto.wizard.yocto.search.WizardYoctoSearcher
import ru.yandex.vertis.mockito.MockitoSupport

import java.util.Optional

@RunWith(classOf[JUnitRunner])
class MotoSourceGeneratorSpec extends FlatSpecLike with MockitoSupport with Matchers {

  private val regionType = ru.yandex.auto.core.region.RegionType.SUBJECT_FEDERATION
  private val category = "motorcycle"
  private val mark = "TOMOTO"
  private val mark2 = "OMOTO"
  private val model = "TM150"
  private val model2 = "T1000"
  private val url = "url"

  private val adSnippet = mock[AdSnippet]
  when(adSnippet.geoIds).thenReturn(Seq(213L))
  when(adSnippet.mark).thenReturn(mark)
  when(adSnippet.model).thenReturn(model)
  when(adSnippet.configuration).thenReturn(None)
  when(adSnippet.complectation).thenReturn(None)
  when(adSnippet.techParam).thenReturn(None)
  when(adSnippet.isNewState).thenReturn(false)
  when(adSnippet.price).thenReturn(500000)

  private val adSnippet2 = mock[AdSnippet]
  when(adSnippet2.geoIds).thenReturn(Seq(2L))
  when(adSnippet2.mark).thenReturn(mark2)
  when(adSnippet2.model).thenReturn(model2)
  when(adSnippet2.configuration).thenReturn(None)
  when(adSnippet2.complectation).thenReturn(None)
  when(adSnippet2.techParam).thenReturn(None)
  when(adSnippet2.isNewState).thenReturn(false)
  when(adSnippet2.price).thenReturn(300000)

  private val snippets = Seq(adSnippet, adSnippet2, adSnippet2, adSnippet, adSnippet)

  private val wizardYoctoSearcher = mock[WizardYoctoSearcher]
  when(wizardYoctoSearcher.foreach(?)).answer { f =>
    snippets.foreach(f.getArgument[AdSnippet => Unit](0))
  }

  private val region = mock[Region]
  when(region.getId).thenReturn(1)
  when(region.getType).thenReturn(regionType)
  when(region.getParent).thenReturn(null)
  private val regionService = mock[RegionService]
  when(regionService.getRegionById(?)).thenReturn(Optional.of(region))

  private val urlService = mock[LandingUrlService]
  when(urlService.listing(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?))
    .thenReturn(Some(LandingUrl(url)))

  private val motoModelImpl = mock[MotoModelImpl]
  when(motoModelImpl.getTypeCode).thenReturn(category)
  private val catalogService = mock[MotoCatalogGroupingService]
  when(catalogService.findModel(?, ?)).thenReturn(Some(motoModelImpl))

  private val motoSourceGenerator = new MotoSourceGenerator(
    wizardYoctoSearcher,
    regionService,
    urlService,
    catalogService
  )

  "MotoSourceGenerator" should "returns correct sources" in {
    val generatedSources = motoSourceGenerator.get
    val sources = generatedSources.landingSource.toSeq
    (sources should have).length(10)
    sources.foreach { source =>
      source should matchPattern {
        case MotoCategorySource(1, `category`, Some(false) | None)                     =>
        case MotoMarkSource(1, `category`, `mark`, Some(false) | None)                 =>
        case MotoMarkSource(1, `category`, `mark2`, Some(false) | None)                =>
        case MotoMarkModelSource(1, `category`, `mark`, `model`, Some(false) | None)   =>
        case MotoMarkModelSource(1, `category`, `mark2`, `model2`, Some(false) | None) =>
      }
    }
    val stat = generatedSources.landingStat
    val modelStat1 = stat.geoStat(1).subCategories(category).marks(mark).models(model)
    assert(modelStat1.getCount(None) == 3)
    val modelStat2 = stat.geoStat(1).subCategories(category).marks(mark2).models(model2)
    assert(modelStat2.getCount(None) == 2)

  }
}
