package ru.yandex.auto.extdata.service.organic.impl

import cats.syntax.option._
import org.joda.time.Instant
import org.junit.runner.RunWith
import org.scalatest.{FlatSpecLike, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.region.{Region, RegionService}
import ru.yandex.auto.dealers.DealersProvider
import ru.yandex.auto.eds.service.cars.CarsCatalogGroupingService
import ru.yandex.auto.extdata.service.organic.LandingUrlService
import ru.yandex.auto.extdata.service.util.MockitoSyntax._
import ru.yandex.auto.extdata.service.util.UrlUtils._
import ru.yandex.auto.traffic.utils.LandingConstants
import ru.yandex.auto.wizard.AdSnippet
import ru.yandex.auto.wizard.yocto.search.WizardYoctoSearcher
import ru.yandex.vertis.mockito.MockitoSupport

import java.util.Optional

@RunWith(classOf[JUnitRunner])
class CarsModelFlatLandingGeneratorImplSpec extends FlatSpecLike with MockitoSupport with Matchers {
  private val regionId = 213

  private val region = mock[Region]
  when(region.getParent).thenReturn(null)
  when(region.getId).thenReturn(regionId)
  private val regionService = mock[RegionService]
  when(regionService.getRegionById(?)).thenReturn(Optional.of(region))

  private val urlService = mock[LandingUrlService]
  when(urlService.dealer(?, ?, ?, ?, ?)).thenReturn("".some)
  when(urlService.cardGroup(?, ?, ?, ?, ?, ?)).thenReturn("".some)
  when(
    urlService.listing(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  ).answer { invocation =>
    val maybeMark = Option(invocation.getArgument[Option[String]](1)).flatten
    val maybeModel = Option(invocation.getArgument[Option[String]](2)).flatten
    val snippetId = Option(invocation.getArgument[Option[String]](3)).flatten
    val onCredit = invocation.getArgument[Option[Boolean]](4)
    val isMobile = invocation.getArgument[Boolean](5)
    landingListingUrl(
      markName = maybeMark,
      modelName = maybeModel,
      snippetId = snippetId,
      onCredit = onCredit,
      isMobile = isMobile
    ).some
  }

  private val carsCatalogGroupingService = mock[CarsCatalogGroupingService]
  when(carsCatalogGroupingService.buildGroupByConfiguration(?)).thenReturn(None)
  when(carsCatalogGroupingService.buildGroupByTechParam(?)).thenReturn(None)

  private val dealerProvider = mock[DealersProvider]
  when(dealerProvider.getDealerById(?)).thenReturn(null)

  private def carsModelFlatLandingGenerator(wizardYoctoSearcher: WizardYoctoSearcher) =
    new CarsModelFlatLandingGeneratorImpl(
      regionService,
      urlService,
      wizardYoctoSearcher,
      carsCatalogGroupingService,
      dealerProvider,
      None
    )

  "CarsModelFlatLandingGeneratorImpl.stream" should "produce both credit and non credit urls when there are credit offers in the index" in {
    def creditAdSnippets(n: Int): Seq[AdSnippet] = {
      val creationTimestamp = Instant.now.getMillis
      (1 to n).map { i =>
        new AdSnippet(
          id = i.toString,
          hash = Seq.fill(i)("h").mkString,
          mark = Seq.fill(i)("a").mkString,
          model = Seq.fill(i)("b").mkString,
          image = "",
          imagesWithAngles = Map(),
          price = 0,
          year = 2022,
          run = 0,
          creationDate = creationTimestamp,
          colorCode = "",
          imagesCount = 0,
          ownersCount = 0,
          isCommercialSeller = false,
          geoIds = Seq(regionId),
          dealerId = None,
          category = LandingConstants.CarsCategory,
          relevance = None,
          onCredit = true.some
        )
      }
    }

    val numSnippets = 30

    val wizardYoctoSearcher = mock[WizardYoctoSearcher]
    when(wizardYoctoSearcher.buffer).thenReturn(creditAdSnippets(numSnippets))

    val generator = carsModelFlatLandingGenerator(wizardYoctoSearcher)
    val landingDefinitions = generator.stream.toSeq
    landingDefinitions should not be empty
    landingDefinitions.exists { ld =>
      isUrlOnCredit(ld.t.url)
    } should be(true)
    landingDefinitions.exists { ld =>
      !isUrlOnCredit(ld.t.url)
    } should be(true)
  }

  it should "produce non credit urls when there are no credit offers in the index" in {
    def nonCreditAdSnippets(n: Int): Seq[AdSnippet] = {
      val creationTimestamp = Instant.now.getMillis
      (1 to n).map { i =>
        new AdSnippet(
          id = i.toString,
          hash = Seq.fill(i)("h").mkString,
          mark = Seq.fill(i)("a").mkString,
          model = Seq.fill(i)("b").mkString,
          image = "",
          imagesWithAngles = Map(),
          price = 0,
          year = 2022,
          run = 0,
          creationDate = creationTimestamp,
          colorCode = "",
          imagesCount = 0,
          ownersCount = 0,
          isCommercialSeller = false,
          geoIds = Seq(regionId),
          dealerId = None,
          category = LandingConstants.CarsCategory,
          relevance = None,
          onCredit = false.some
        )
      }
    }

    val numSnippets = 30

    val wizardYoctoSearcher = mock[WizardYoctoSearcher]
    when(wizardYoctoSearcher.buffer).thenReturn(nonCreditAdSnippets(numSnippets))

    val generator = carsModelFlatLandingGenerator(wizardYoctoSearcher)
    val landingDefinitions = generator.stream.toSeq
    landingDefinitions should not be empty
    landingDefinitions.foreach { ld =>
      isUrlOnCredit(ld.t.url) should be(false)
    }
  }
}
