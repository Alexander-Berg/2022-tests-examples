package ru.yandex.vertis.general.wizard.api.services

import common.geobase.model.RegionIds
import common.geobase.{GeobaseParser, Tree}
import general.search.offer_count_model.{
  OfferAttributeCountByCategory,
  OfferAttributeCountSnapshot,
  OfferCountByAttribute,
  OfferCountByAttributeValue
}
import pureconfig.ConfigSource
import ru.yandex.vertis.general.wizard.core.service.RegionService
import ru.yandex.vertis.general.wizard.core.service.impl._
import ru.yandex.vertis.general.wizard.model.OfferFilter.Attribute.Dictionary
import ru.yandex.vertis.general.wizard.model.OfferFilter.{Attribute, PriceFilter}
import ru.yandex.vertis.general.wizard.model._
import zio.ZIO
import zio.test.Assertion._
import zio.test._

object TextBuilderSpec extends DefaultRunnableSpec {

  private val regions = GeobaseParser.parse(CommonGetFormBuilderSpec.getClass.getResourceAsStream("/regions"))
  private val tree = new Tree(regions)
  private val regionService = new LiveRegionService(tree)
  private val bonsai = LiveBonsaiService.create(TestCatalog.bonsaiSnapshot)
  private val config = ConfigSource.url(CommonGetFormBuilderSpec.getClass.getResource("/application_test.conf"))

  private val wizardSettings =
    config.at("settings").load[Settings].getOrElse(throw new IllegalStateException("settings not found"))
  private val settingsService = LiveSettingsService(wizardSettings)

  private val templateService = new LiveTemplateService(settingsService, bonsai)

  private val offerCountService = new LiveOfferStatsService(
    OfferAttributeCountSnapshot(
      Map(
        2L -> OfferAttributeCountByCategory(
          Map(
            TestCatalog.guitars.id -> OfferCountByAttribute(
              Map(
                "some_attr_id" -> OfferCountByAttributeValue(Map("value" -> 10))
              )
            )
          )
        ),
        213L -> OfferAttributeCountByCategory(
          Map(
            TestCatalog.guitars.id -> OfferCountByAttribute(
              Map(
                "some_attr_id" -> OfferCountByAttributeValue(Map("value" -> 12))
              )
            )
          )
        )
      )
    ),
    LiveBonsaiService.create(TestCatalog.bonsaiSnapshot)
  )

  private val textBuilder = new LiveTextBuilder(regionService, bonsai, templateService, offerCountService)

  private val BrandEssential =
    WizardEssentials.Brand(RegionService.DefaultRegionId, Seq.empty, Set.empty, Platform.Desktop)

  private val GetEssential =
    WizardEssentials.Get(
      TestCatalog.cats,
      RegionService.DefaultRegionId,
      Seq.empty,
      Seq.empty,
      Set.empty,
      Platform.Desktop
    )

  private def huskyFilter = OfferFilter.Attribute.Dictionary(
    TestCatalog.dogBreeds.id,
    TestCatalog.husky.key
  )

  private def extraAttrFilter = OfferFilter.Attribute.Dictionary(
    TestCatalog.extraAutonomousAttr.id,
    TestCatalog.extraAutonomousAttrValue.key
  )

  private def sexFilter = OfferFilter.Attribute.Dictionary(
    TestCatalog.sexes.id,
    TestCatalog.male.key
  )

  private val DogsGetEssentials =
    WizardEssentials.Get(
      TestCatalog.dogs,
      RegionService.DefaultRegionId,
      Seq.empty,
      Seq.empty,
      Set.empty,
      Platform.Desktop
    )

  private def iPhoneAttrFilter = OfferFilter.Attribute.Dictionary(
    TestCatalog.iPhoneAttr.id,
    TestCatalog.iPhoneAttrValue.key
  )

  private val IphoneGetEssentials =
    WizardEssentials.Get(
      TestCatalog.phone,
      RegionService.DefaultRegionId,
      Seq.empty,
      Seq.empty,
      Set.empty,
      Platform.Desktop
    )

  private val PostEssential =
    WizardEssentials.Post(TestCatalog.cats, RegionService.DefaultRegionId, Seq.empty, Platform.Desktop, Set.empty)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("TextBuilder")(
      testM("Correctly process Brand title") {
        for {
          text <- textBuilder.build(
            Text.Title,
            BrandEssential
          )
        } yield assert(text)(equalTo("Яндекс.Объявления - свежие объявления в России"))
      },
      testM("Correctly process Get title") {
        for {
          text <- textBuilder.build(
            Text.Title,
            GetEssential
          )
        } yield assert(text)(equalTo("Яндекс.Объявления - купить кошек в России"))
      },
      testM("Correctly process Post title") {
        for {
          text <- textBuilder.build(
            Text.Title,
            PostEssential
          )
        } yield assert(text)(equalTo("Яндекс.Объявления - продать кошек в России"))
      },
      testM("Don't fail on filters") {

        val filters = Seq(
          Attribute.Dictionary("attr_id", "value"),
          Attribute.Bool("attr_id2", value = false),
          PriceFilter(PriceRange.OpenRange)
        )

        val essentials = Seq(
          GetEssential.copy(offerFilters = filters),
          PostEssential.copy(offerFilters = filters)
        )

        for {
          texts <- ZIO.foreach(essentials)(textBuilder.build(Text.Title, _))
        } yield assert(texts)(forall(isNonEmptyString))
      },
      testM("Correctly return count for spb") {
        for {
          text <- textBuilder.build(
            Text.Title,
            WizardEssentials
              .Get(TestCatalog.guitars, RegionIds.SaintPetersburg, Seq.empty, Seq.empty, Set.empty, Platform.Desktop)
          )
        } yield assert(text)(equalTo("Купить 10 гитар"))
      },
      testM("Correctly return count for msk") {
        for {
          text <- textBuilder
            .build(
              Text.Title,
              WizardEssentials
                .Get(TestCatalog.guitars, RegionIds.Moscow, Seq.empty, Seq.empty, Set.empty, Platform.Desktop)
            )
        } yield assert(text)(equalTo("Купить 12 гитар"))
      },
      testM("Correctly return 0 count for non existing geo") {
        for {
          text <- textBuilder.build(
            Text.Title,
            WizardEssentials.Get(
              TestCatalog.guitars,
              RegionIds.MoscowAndMoscowRegion,
              Seq.empty,
              Seq.empty,
              Set.empty,
              Platform.Desktop
            )
          )
        } yield assert(text)(equalTo("Купить 0 гитар"))
      },
      testM("Correctly append autonomous attribute if present") {
        for {
          text <- textBuilder.build(
            Text.Title,
            DogsGetEssentials.copy(offerFilters = Seq(huskyFilter))
          )
        } yield assert(text)(equalTo("Собаки хаски - купить в России"))
      },
      testM("Skip autonomous attribute if not present in offerFilters") {
        for {
          text <- textBuilder.build(
            Text.Title,
            DogsGetEssentials
          )
        } yield assert(text)(equalTo("Собаки - купить в России"))
      },
      testM("Skip non autonomous attributes") {
        for {
          text <- textBuilder.build(
            Text.Title,
            DogsGetEssentials.copy(offerFilters = Seq(sexFilter))
          )
        } yield assert(text)(equalTo("Собаки - купить в России"))
      },
      testM("Skip non autonomous attributes and append autonomous") {
        for {
          text <- textBuilder.build(
            Text.Title,
            DogsGetEssentials.copy(offerFilters = Seq(sexFilter, huskyFilter))
          )
        } yield assert(text)(equalTo("Собаки хаски - купить в России"))
      },
      testM("Append several autonomous attributes") {
        for {
          text <- textBuilder.build(
            Text.Title,
            DogsGetEssentials.copy(offerFilters = Seq(huskyFilter, extraAttrFilter))
          )
        } yield assert(text)(equalTo("Собаки хаски какие то - купить в России"))
      },
      testM("Correctly append brand placeholder if present") {
        for {
          text <- textBuilder.build(
            Text.Title,
            IphoneGetEssentials.copy(offerFilters = Seq(iPhoneAttrFilter))
          )
        } yield assert(text)(equalTo("Мобильные телефоны iPhone - купить в России"))
      }
    )
}
