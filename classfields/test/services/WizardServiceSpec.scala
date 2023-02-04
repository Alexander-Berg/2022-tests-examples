package ru.yandex.vertis.general.wizard.api.services

import common.geobase.Region
import common.geobase.model.RegionIds.RegionId
import izumi.reflect.Tag
import ru.yandex.vertis.general.wizard.api.services.WizardMatchSelector.WizardMatchSelectionError
import ru.yandex.vertis.general.wizard.core.search.SearchService
import ru.yandex.vertis.general.wizard.api.services.impl.LiveWizardEssentialsBuilder
import ru.yandex.vertis.general.wizard.api.services.impl.LiveWizardEssentialsBuilder.EmptyResult
import ru.yandex.vertis.general.wizard.core.service.OfferStatsService.OfferStatsService
import ru.yandex.vertis.general.wizard.core.service.RegionService
import ru.yandex.vertis.general.wizard.core.service.impl.LiveBonsaiService
import ru.yandex.vertis.general.wizard.meta.rules.RulesFactory
import ru.yandex.vertis.general.wizard.meta.service.impl.LiveDictionaryService
import ru.yandex.vertis.general.wizard.meta.service.{DictionaryService, MetaWizard}
import ru.yandex.vertis.general.wizard.model._
import common.zio.logging.Logging
import zio.test.Assertion._
import zio.test._
import zio.{Has, Task, ULayer, ZIO, ZLayer}

import scala.reflect.ClassTag

object WizardServiceSpec extends DefaultRunnableSpec {

  private val dictionaryService =
    new LiveDictionaryService(
      TestIntentionPragmatics.dictionarySnapshot,
      TestCatalog.metaPragmaticsSnapshot,
      TestCatalog.bonsaiSnapshot,
      TestCatalog.catalogSynonymsMapping
    )

  private val Offers = Seq(
    MicroOffer(
      "micro",
      "111",
      Seq(RegionId(213)),
      "category",
      Price.SalaryRub(100L),
      "imageUrl",
      "imageUrl260x194",
      true
    )
  )

  private def simpleListing(categoryId: CategoryId): Listing =
    CategoryListing(RegionService.DefaultRegionId, categoryId, Seq.empty)

  private val testOffersStatService = TestOffersStatService(
    Map(
      simpleListing(TestCatalog.guitars.id) -> 5,
      simpleListing(TestCatalog.rabota.id) -> 5,
      simpleListing(TestCatalog.cats.id) -> 4
    )
  )

  private val onlyInDefaultRegionSeracher = new SearchService.Service {
    override def search(request: SearchService.Request): Task[Seq[MicroOffer]] = Task.succeed(Offers)
  }

  private val categoryTagsService = TestCategoryTagsResource.simple(CategoryTag.WithoutCarousel, Set("rabota"))

  private val mockRegionService = new RegionService.Service {
    override def getRegion(regionId: RegionId): Task[Option[Region]] = ZIO.none

    override def getPathToRoot(regionId: RegionId): Task[Seq[Region]] = Task.succeed(Seq.empty)
  }

  private def hasCorrectEssentials(expected: WizardEssentials): Assertion[WizardResult] =
    hasField[WizardResult, Either[Throwable, WizardEssentials]](
      "essentials",
      _.wizardEssentials,
      isRight(equalTo(expected))
    )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("WizardService")(
      testM("parse brand query") {
        for {
          wizardResult <- WizardService.process(WizardRequest.empty("купить яндекс.объявления"))
        } yield assert(wizardResult)(
          hasCorrectEssentials(
            WizardEssentials.Brand(RegionService.DefaultRegionId, Offers, Set.empty, Platform.Desktop)
          )
        )
      },
      testM("parse post query") {
        for {
          wizardResult <- WizardService.process(WizardRequest.empty("продать гитару"))
        } yield assert(wizardResult)(
          hasCorrectEssentials(
            WizardEssentials.Post(
              TestCatalog.guitars,
              RegionService.DefaultRegionId,
              Seq.empty,
              Platform.Desktop,
              Set.empty
            )
          )
        )
      },
      testM("parse get query") {
        for {
          wizardResult <- WizardService.process(WizardRequest.empty("купить гитару"))
        } yield assert(wizardResult)(
          hasCorrectEssentials(
            WizardEssentials.Get(
              TestCatalog.guitars,
              RegionService.DefaultRegionId,
              Seq.empty,
              Offers,
              Set.empty,
              Platform.Desktop
            )
          )
        )
      },
      testM("parse get query with without-carousel-category tag") {
        for {
          wizardResult <- WizardService.process(WizardRequest.empty("работы"))
        } yield assert(wizardResult)(
          hasCorrectEssentials(
            WizardEssentials.Get(
              TestCatalog.rabota,
              RegionService.DefaultRegionId,
              Seq.empty,
              Seq.empty,
              Set.empty,
              Platform.Desktop
            )
          )
        )
      },
      testM("priority parse get query") {
        for {
          wizardResult <- WizardService.process(WizardRequest.empty("продать купить гитару"))
        } yield assert(wizardResult)(
          hasCorrectEssentials(
            WizardEssentials.Get(
              TestCatalog.guitars,
              RegionService.DefaultRegionId,
              Seq.empty,
              Offers,
              Set.empty,
              Platform.Desktop
            )
          )
        )
      },
      testM("parse get by default") {
        for {
          wizardResult <- WizardService.process(WizardRequest.empty("гитару"))
        } yield assert(wizardResult)(
          hasCorrectEssentials(
            WizardEssentials.Get(
              TestCatalog.guitars,
              RegionService.DefaultRegionId,
              Seq.empty,
              Offers,
              Set.empty,
              Platform.Desktop
            )
          )
        )
      },
      failExpected[WizardMatchSelectionError]("fail with attributes without category", "сибирскую"),
      failExpected[WizardMatchSelectionError]("fail with disabled for category attributes", "купить маленькую кошку"),
      failExpected[WizardMatchSelectionError]("fail with disabled attributes", "купить черную кошку"),
      failExpected[WizardMatchSelectionError]("fail with non-intersection attributes", "купить сибирскую мягкую"),
      failExpected[EmptyResult]("fail because of less than 5 offers in OffersStatService", "купить кошку")
    )
  }.provideCustomLayer {
    val bonsai = ZLayer.succeed(LiveBonsaiService.create(TestCatalog.bonsaiSnapshot))
    val logging = Logging.live
    val dictionary = ZLayer.succeed[DictionaryService.Service](dictionaryService)
    val categoryTagsLayer = ZLayer.succeed(categoryTagsService)
    val rulesFactory = ZLayer.succeed(mockRegionService) ++
      dictionary ++
      bonsai ++
      categoryTagsLayer >>>
      RulesFactory.live
    val metaWizard = rulesFactory >>> MetaWizard.live
    val searchService = ZLayer.succeed(onlyInDefaultRegionSeracher)
    val offersStatService: ULayer[OfferStatsService] = ZLayer.succeed(testOffersStatService)
    val wizardResultBuilder = bonsai ++
      searchService ++
      categoryTagsLayer ++
      offersStatService >>>
      WizardEssentialsBuilder.live
    val wizardMatchSelector = bonsai ++ categoryTagsLayer >>> WizardMatchSelector.live
    wizardMatchSelector ++ wizardResultBuilder ++ metaWizard ++ logging >>> WizardService.live
  }

  private def failExpected[E: ClassTag](label: String, query: String) =
    testM(label) {
      for {
        result <- WizardService.process(WizardRequest.empty(query))
      } yield assert(result)(
        hasField[WizardResult, Either[Throwable, WizardEssentials]](
          "essentials",
          _.wizardEssentials,
          isLeft(isSubtype[E](anything))
        )
      )
    }
}
