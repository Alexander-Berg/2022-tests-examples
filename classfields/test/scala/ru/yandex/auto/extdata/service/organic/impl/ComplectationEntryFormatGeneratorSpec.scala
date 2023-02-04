package ru.yandex.auto.extdata.service.organic.impl

import org.junit.runner.RunWith
import org.scalatest.{FlatSpecLike, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.AutoLang
import ru.yandex.auto.core.catalog.model.{ComplectationImpl, ConfigurationImpl, SuperGenerationImpl, TechParameterImpl}
import ru.yandex.auto.core.dictionary.{Field, FieldPersistenceManager, Type}
import ru.yandex.auto.core.model.{ShortReview, ShortReviewsTree}
import ru.yandex.auto.core.region.{Region, RegionService}
import ru.yandex.auto.eds.catalog.cars.structure.{
  ComplectationStructure,
  ConfigurationStructure,
  SuperGenStructure,
  TechParamStructure
}
import ru.yandex.auto.eds.service.cars.CarsCatalogGroupingService
import ru.yandex.auto.extdata.service.organic.LandingTextBuilder.TextRequest
import ru.yandex.auto.extdata.service.organic.builder.IdBuilder
import ru.yandex.auto.extdata.service.organic.model.CarsLandingSource.ComplectationSource
import ru.yandex.auto.extdata.service.organic.model.LandingStat.{
  MutableGeoStat,
  MutableMarkStat,
  MutableModelStat,
  MutableSubcategoryStat
}
import ru.yandex.auto.extdata.service.organic.model.{LandingEntryFormat, LandingStat, LandingThumbFormat, LandingUrl}
import ru.yandex.auto.extdata.service.organic.{LandingTextBuilder, LandingUrlService}
import ru.yandex.auto.extdata.service.util.MockitoSyntax._
import ru.yandex.auto.traffic.utils.LandingConstants
import ru.yandex.vertis.mockito.MockitoSupport

import java.util.Optional
import scala.collection.JavaConverters._
import scala.util.matching.Regex

@RunWith(classOf[JUnitRunner])
class ComplectationEntryFormatGeneratorSpec extends FlatSpecLike with MockitoSupport with Matchers {
  private val url = "url"
  private val upgradedUrl = s"$url?do_not_redirect=true"
  private val mobileUrl = "mobileUrl"
  private val upgradedMobileUrl = s"$mobileUrl?do_not_redirect=true"
  private val reviewsUrl = "reviewsUrl"
  private val regionId = 213L
  private val mark = "kia"
  private val model = "rio"
  private val confId = 1L
  private val techParamId = 1L
  private val superGenId = 1L
  private val title = "title"
  private val markName = "mark_name"
  private val engineLitres = 2.0f
  private val engineCm3 = engineLitres * 1000
  private val horsePowers = 200
  private val reviewsNum = 10
  private val imagesMap = (1 to 10).map(id => id.toString -> s"image_$id").toMap
  private val imageRegex = "(image_[\\d]+)".r

  private val urlService = mock[LandingUrlService]
  when(urlService.listing(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)).answer { invocation =>
    (if (invocation.getArgument[Boolean](5)) Some(mobileUrl) else Some(url)).map(url => LandingUrl(url))
  }
  when(
    urlService.complectation(eq(Some(regionId)), eq(mark), eq(model), ?, eq(confId), eq(techParamId), eq(superGenId), ?)
  ).answer { invocation =>
    val name = invocation.getArgument[String](3)
    Some(s"url_$name")
  }
  private val complectationUrlRegex = "(url_[a-z\\d_]+)".r
  when(urlService.reviews(?, ?, ?)).thenReturn(Some(reviewsUrl))

  private val region = mock[Region]
  private val regionService = mock[RegionService]
  when(regionService.getRegionById(regionId.toInt)).thenReturn(Optional.of(region))

  private val textBuilder = mock[LandingTextBuilder]
  when(textBuilder.build(?)).answer { invocation =>
    invocation.getArgument[TextRequest](0) match {
      case _: TextRequest.Model    => title
      case _: TextRequest.MarkName => markName
    }
  }

  private val transmissionCode = "transmissionCode"
  private val superGenImpl = mock[SuperGenerationImpl]
  when(superGenImpl.getId).thenReturn(superGenId)
  private val superGenStructure = mock[SuperGenStructure]
  when(superGenStructure.entity).thenReturn(superGenImpl)
  private val configurationImpl = mock[ConfigurationImpl]
  when(configurationImpl.getId).thenReturn(confId)
  when(configurationImpl.getMobileWizardPhotosRetina).thenReturn(imagesMap.asJava)
  private val configurationStructure = mock[ConfigurationStructure]
  when(configurationStructure.entity).thenReturn(configurationImpl)
  when(configurationStructure.parent).thenReturn(superGenStructure)
  private val techParamImpl = mock[TechParameterImpl]
  when(techParamImpl.getDisplacementCm3).thenReturn(engineCm3.toInt)
  when(techParamImpl.getHorsePower).thenReturn(horsePowers)
  when(techParamImpl.getTransmissionCode).thenReturn(transmissionCode)
  private val techParamStructure = mock[TechParamStructure]
  when(techParamStructure.parent).thenReturn(configurationStructure)
  when(techParamStructure.entity).thenReturn(techParamImpl)

  private def complectationName(ind: Long) = s"complectation_name_$ind"
  private val complectationNameRegex: Regex = "(complectation_name_[\\d]+)".r
  private def complectationStructure(ind: Long) = {
    val complectationImpl = mock[ComplectationImpl]
    when(complectationImpl.getName).thenReturn(complectationName(ind))
    val complectationStructure = mock[ComplectationStructure]
    when(complectationStructure.parent).thenReturn(techParamStructure)
    when(complectationStructure.entity).thenReturn(Some(complectationImpl))
    complectationStructure
  }

  private val groupingService = mock[CarsCatalogGroupingService]
  when(groupingService.buildGroupByComplectation(?)).answer { invocation =>
    Some(complectationStructure(invocation.getArgument[Long](0)))
  }
  when(groupingService.buildGroupByTechParam(techParamId)).thenReturn(Some(techParamStructure))
  when(groupingService.buildGroupByConfiguration(?)).thenReturn(Some(configurationStructure))

  private val transmissionType = mock[Type]
  when(transmissionType.getShortName(AutoLang.RU)).thenReturn("автомат")
  private val transmissionField = mock[Field]
  when(transmissionField.getTypeByCode(transmissionCode)).thenReturn(transmissionType)
  private val fpm = mock[FieldPersistenceManager]
  when(fpm.getTransmissionField).thenReturn(transmissionField)
  private val shortReview = mock[ShortReview]
  private val shortReviewsTree = mock[ShortReviewsTree]
  when(shortReviewsTree.getReviewsByMarkModelAndGen(eq(mark), eq(model), eq(superGenId)))
    .thenReturn(List.fill(reviewsNum)(shortReview))

  private val generator = new ComplectationEntryFormatGenerator(
    urlService,
    regionService,
    textBuilder,
    groupingService,
    fpm,
    shortReviewsTree
  )

  private val source = ComplectationSource(regionId, mark, model)

  private def buildStatWithComplectations(source: ComplectationSource, maxComplectations: Int): LandingStat = {
    val modelStat = new MutableModelStat
    (1 to maxComplectations).foreach { ind =>
      modelStat.techParamComplectationCount.put((techParamId, ind), ind)
      modelStat.complectationMinPriceRub.put(ind, ind)
      modelStat.configurationCount.put(ind, ind)
    }
    val markStat = new MutableMarkStat
    markStat.models.put(model, modelStat)
    val subCategoryStat = new MutableSubcategoryStat
    subCategoryStat.marks.put(mark, markStat)
    val geoStat = new MutableGeoStat
    geoStat.subCategories.put(LandingConstants.CarsCategory, subCategoryStat)
    val validStat = new LandingStat
    validStat.geoStat.put(regionId, geoStat)
    validStat.geoStat.put(LandingConstants.Russia, geoStat)
    validStat
  }

  "ComplectationEntryFormatGenerator" should "build entries for correct source and stats" in {
    val fullStat = buildStatWithComplectations(source, 10)
    val entries = generator.entries(source, fullStat).toSeq
    (entries should have).length(1)
    val entry = entries.head
    val expectedUrls =
      List(url, mobileUrl, upgradedUrl, upgradedMobileUrl).map(url => LandingUrl(url))
    entry should matchPattern {
      case LandingEntryFormat(
          `expectedUrls`,
          `title`,
          _,
          _,
          _
          ) =>
    }
    (entry.thumbs should have).length(LandingConstants.ComplectationsCount)
    entry.thumbs.foreach { thumb =>
      val setsIds = entry.urls.map(lu => IdBuilder.id(lu.url))
      thumb should matchPattern {
        case LandingThumbFormat(
            complectationNameRegex(_),
            LandingUrl(complectationUrlRegex(_), _),
            `setsIds`,
            Seq(imageRegex(_)),
            LandingConstants.CarsCategory,
            _,
            Some(price),
            Some(`markName`),
            Some(count),
            Some(russiaCount),
            Some(`reviewsUrl`),
            Some(`reviewsNum`),
            Some("Автомат"),
            Some(`horsePowers`),
            Some(`engineLitres`),
            true
            ) if price <= 10 && price >= 1 && count == russiaCount && count == price =>
      }
    }
  }

  it should "not build entry if no complectations" in {
    val emptyStat = buildStatWithComplectations(source, 0)
    generator.entries(source, emptyStat).toSeq shouldBe empty
  }
}
