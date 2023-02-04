package ru.yandex.vertis.general.wizard.api.services

import common.geobase.model.RegionIds.RegionId
import common.geobase.{GeobaseParser, Tree}
import general.search.offer_count_model.OfferAttributeCountSnapshot
import pureconfig.ConfigSource
import ru.yandex.vertis.general.wizard.core.service.RegionService
import ru.yandex.vertis.general.wizard.core.service.UrlBuilder.UrlBuilderConfig
import ru.yandex.vertis.general.wizard.core.service.impl.FormBuilder.FormBuilderConfig
import ru.yandex.vertis.general.wizard.core.service.impl._
import ru.yandex.vertis.general.wizard.model.{Platform, Settings, WizardEssentials}
import zio.test.Assertion.equalTo
import zio.test._

object CommonGetFormBuilderSpec extends DefaultRunnableSpec {
  private val baseUrl = "general.yandex"
  private val urlBuilderConfig = UrlBuilderConfig(baseUrl)
  private val formBuilderConfig = FormBuilderConfig(baseUrl)
  private val regions = GeobaseParser.parse(CommonGetFormBuilderSpec.getClass.getResourceAsStream("/regions"))
  private val tree = new Tree(regions)
  private val regionService = new LiveRegionService(tree)
  private val bonsai = LiveBonsaiService.create(TestCatalog.bonsaiSnapshot)
  private val config = ConfigSource.url(CommonGetFormBuilderSpec.getClass.getResource("/application_test.conf"))

  private val offerCountService =
    new LiveOfferStatsService(OfferAttributeCountSnapshot(), LiveBonsaiService.create(TestCatalog.bonsaiSnapshot))

  private val wizardSettings =
    config.at("settings").load[Settings].getOrElse(throw new IllegalStateException("settings not found"))
  private val settingsService = LiveSettingsService(wizardSettings)
  private val templateService = new LiveTemplateService(settingsService, bonsai)
  private val textBuilder = new LiveTextBuilder(regionService, bonsai, templateService, offerCountService)
  private val urlBuilder = new LiveUrlBuilder(urlBuilderConfig, bonsai)
  private val regionsHRUService = new LiveRegionsHRUService(Map(RegionId(213) -> "moskva"))

  private val commonGetFormBuilder =
    new CommonGetFormBuilder(bonsai, urlBuilder, textBuilder, regionsHRUService, regionService, formBuilderConfig)

  private val expectedGeoPart = "в России"

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("BrandFormBuilder")(
      testM("build brand form") {
        for {
          wizardForm <- commonGetFormBuilder.build(
            WizardEssentials.Brand(RegionService.DefaultRegionId, Seq.empty, Set.empty, Platform.Desktop)
          )
        } yield assert(wizardForm.title.get.text.get.__hl.contains(expectedGeoPart))(equalTo(true))
      }
    )
  }

}
