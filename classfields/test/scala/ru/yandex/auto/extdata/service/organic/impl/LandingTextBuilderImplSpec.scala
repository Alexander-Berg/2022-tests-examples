package ru.yandex.auto.extdata.service.organic.impl

import org.junit.runner.RunWith
import ru.yandex.auto.core.region.Region
import org.scalatest.{FlatSpecLike, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.catalog.model.moto.{MotoCategoryImpl, MotoMarkImpl, MotoModelImpl}
import ru.yandex.auto.core.dictionary.{
  FieldPersistenceManager,
  MotoFieldPersistenceManager,
  TrucksFieldPersistenceManager
}
import ru.yandex.auto.dealers.DealersProvider
import ru.yandex.auto.eds.service.RegionService
import ru.yandex.auto.eds.service.moto.MotoCatalogGroupingService
import ru.yandex.auto.eds.service.trucks.TrucksCatalogGroupingService
import ru.yandex.auto.extdata.service.organic.LandingTextBuilder.{TextRequest, TextTypes}
import ru.yandex.auto.searcher.configuration.WizardSearchConfiguration
import ru.yandex.auto.wizard.utils.builders.description.{
  AutoDescriptionBuilder,
  MotoDescriptionBuilder,
  TrucksDescriptionBuilder
}
import ru.yandex.auto.wizard.utils.builders.titles.{AutoTitleTextBuilder, MotoTitleTextBuilder, TrucksTitleTextBuilder}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.auto.extdata.service.util.MockitoSyntax._

@RunWith(classOf[JUnitRunner])
class LandingTextBuilderImplSpec extends FlatSpecLike with MockitoSupport with Matchers {

  private val motoCategory = mock[MotoCategoryImpl]
  private val motoMark = mock[MotoMarkImpl]
  when(motoMark.getCode).answer(_ => "tomoto")
  private val setMarks = Set(motoMark)
  private val motoModel = mock[MotoModelImpl]
  when(motoModel.getCode).answer(_ => "TM150")
  private val setModels = Set(motoModel)
  private val region = mock[Region]
  private val sc = mock[WizardSearchConfiguration]
  when(sc.getWizardRegion).answer(_ => region)

  private val autoTitleTextBuilder = mock[AutoTitleTextBuilder]
  private val autoDescriptionBuilder = mock[AutoDescriptionBuilder]
  private val trucksTitleTextBuilder = mock[TrucksTitleTextBuilder]
  private val trucksDescriptionBuilder = mock[TrucksDescriptionBuilder]
  private val motoTitleTextBuilder = mock[MotoTitleTextBuilder]
  when(motoTitleTextBuilder.buildForCategory(?, ?, ?, ?, ?)).answer(_ => "Moto category title")
  when(motoTitleTextBuilder.buildForMark(?, ?, ?, ?, ?, ?)).answer(_ => "Moto mark title")
  when(motoTitleTextBuilder.buildForModel(?, ?, ?, ?, ?)).answer(_ => "Moto model title")

  private val motoDescriptionBuilder = mock[MotoDescriptionBuilder]
  private val fpm = mock[FieldPersistenceManager]
  private val trucksCatalogGroupingService = mock[TrucksCatalogGroupingService]
  private val motoCatalogGroupingService = mock[MotoCatalogGroupingService]
  when(motoCatalogGroupingService.getTypeByCode).answer(_ => _ => Some(motoCategory))
  when(motoCatalogGroupingService.getSetMarks(?)).answer(_ => setMarks)
  when(motoCatalogGroupingService.getSetModels(?)).answer(_ => setModels)

  private val truckFpm = mock[TrucksFieldPersistenceManager]
  private val motoFpm = mock[MotoFieldPersistenceManager]
  private val regionService = mock[RegionService]
  private val dealersProvider = mock[DealersProvider]

  "LandingTextBuilderImpl" should "returns correct titles" in {
    val landingTextBuilder = new LandingTextBuilderImpl(
      autoTitleTextBuilder,
      autoDescriptionBuilder,
      trucksTitleTextBuilder,
      trucksDescriptionBuilder,
      motoTitleTextBuilder,
      motoDescriptionBuilder,
      fpm,
      trucksCatalogGroupingService,
      motoCatalogGroupingService,
      truckFpm,
      motoFpm,
      regionService,
      dealersProvider
    )

    landingTextBuilder.build(TextRequest.MotoCategory(TextTypes.Title, sc, "snowmobile")) shouldBe "Moto category title"
    landingTextBuilder.build(TextRequest.MotoMark(TextTypes.Title, sc, "snowmobile", "tomoto")) shouldBe "Moto mark title"
    landingTextBuilder.build(TextRequest.MotoMarkModel(TextTypes.Title, sc, "snowmobile", "tomoto", "tm150")) shouldBe "Moto model title"
  }

}
