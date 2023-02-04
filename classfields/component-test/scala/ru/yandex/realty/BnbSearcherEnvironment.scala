package ru.yandex.realty

import ru.yandex.realty.application.extdata.providers.site.{
  ExtdataExtSiteStatisticsComponents,
  ExtdataSitesServiceComponents
}
import ru.yandex.realty.application.extdata.providers.village.{
  ExtdataVillageComponents,
  ExtdataVillageDynamicInfoComponents
}
import ru.yandex.realty.application.extdata.providers.{
  ExtdataAuctionResultComponents,
  ExtdataCampaignComponents,
  ExtdataCompaniesComponents,
  ExtdataExpectedMetroComponents,
  ExtdataParkComponents,
  ExtdataPondComponents,
  ExtdataRegionGraphComponents,
  ExtdataVerbaComponents
}
import ru.yandex.realty.application.ng.AppConfig
import ru.yandex.realty.application.{BnbSearcherConfig, CompletedSiteMortgageMatcherComponents}
import ru.yandex.realty.componenttest.env.DefaultExtdataComponentTestEnvironment
import ru.yandex.realty.componenttest.env.initializers.ComponentTestTeleponyInitializer
import ru.yandex.realty.componenttest.extdata.core.{
  ComponentTestExtdataControllerProvider,
  DefaultComponentTestSearchContextComponents
}
import ru.yandex.realty.componenttest.extdata.stubs.{
  ExtdataAuctionResultResourceStub,
  ExtdataBunkerResourceStub,
  ExtdataCampaignResourceStub,
  ExtdataCatBoostModelsResourceStub,
  ExtdataCommercialBuildingsResourceStub,
  ExtdataCompaniesResourceStub,
  ExtdataCurrencyConversionResourceStub,
  ExtdataCurrencyStocksResourceStub,
  ExtdataDeveloperGeoStatisticResourceStub,
  ExtdataDeveloperWithChatResourceStub,
  ExtdataExtSiteStatisticsResourceStub,
  ExtdataGeoFactsResourceStub,
  ExtdataHeatmapsResourceStub,
  ExtdataParksResourceStub,
  ExtdataPinnedSpecialProjectsResourceStub,
  ExtdataPondsResourceStub,
  ExtdataRawVillagesResourceStub,
  ExtdataRegionDocumentsStatisticsResourceStub,
  ExtdataRegionGraphResourceStub,
  ExtdataRegionNamesTranslationsResourceStub,
  ExtdataSchoolIndexResourceStub,
  ExtdataSiteCtrResourceStub,
  ExtdataSiteRelevanceResourceStub,
  ExtdataSiteStatisticsResourceStub,
  ExtdataSitesResourceStub,
  ExtdataSitesSearchIndexResourceStub,
  ExtdataStreetIndexResourceStub,
  ExtdataTagsResourceStub,
  ExtdataVerba2ResourceStub,
  ExtdataVerbaDescriptionsResourceStub,
  ExtdataVillageCampaignsResourceStub,
  ExtdataVillageDynamicInfoResourceStub,
  ExtdataVillageIndexResourceStub,
  ExtdataVillagesResourceStub
}

object BnbSearcherEnvironment
  extends DefaultExtdataComponentTestEnvironment[BnbSearcherComponent, BnbSearcherConfig]
  with BnbSeacherComonentTestExternalHttpInitializers
  with BnbSearcherComponentTestExtdataResourceStubs
  with BnbSearcherComponentTestExtdataControllerProvider
  with BnbSearcherComponentTestExtdataResourceIndexStubs {

  override def buildComponent(): BnbSearcherComponent = new BnbSearcherComponent

  override lazy val config: BnbSearcherConfig = BnbSearcherConfigBuilder.config

  override lazy val appConfig: AppConfig = config.appConfig
}

trait BnbSeacherComonentTestExternalHttpInitializers extends ComponentTestTeleponyInitializer

trait BnbSearcherComponentTestExtdataResourceStubs
  extends ExtdataSitesResourceStub
  with ExtdataRegionGraphResourceStub
  with ExtdataDeveloperGeoStatisticResourceStub
  with ExtdataDeveloperWithChatResourceStub
  with ExtdataCompaniesResourceStub
  with ExtdataCampaignResourceStub
  with ExtdataAuctionResultResourceStub
  with ExtdataCurrencyConversionResourceStub
  with ExtdataCurrencyStocksResourceStub
  with ExtdataParksResourceStub
  with ExtdataPondsResourceStub
  with ExtdataPinnedSpecialProjectsResourceStub
  with ExtdataHeatmapsResourceStub
  with ExtdataSiteStatisticsResourceStub
  with ExtdataStreetIndexResourceStub
  with ExtdataSiteRelevanceResourceStub
  with ExtdataSiteCtrResourceStub
  with ExtdataExtSiteStatisticsResourceStub
  with ExtdataVerba2ResourceStub
  with ExtdataGeoFactsResourceStub
  with ExtdataVillageCampaignsResourceStub
  with ExtdataVillageDynamicInfoResourceStub
  with ExtdataVillagesResourceStub
  with ExtdataRawVillagesResourceStub
  with ExtdataTagsResourceStub
  with ExtdataCommercialBuildingsResourceStub
  with ExtdataSchoolIndexResourceStub
  with ExtdataCatBoostModelsResourceStub
  with ExtdataVerbaDescriptionsResourceStub
  with ExtdataBunkerResourceStub
  with ExtdataRegionNamesTranslationsResourceStub

trait BnbSearcherComponentTestExtdataControllerProvider
  extends ComponentTestExtdataControllerProvider
  with ExtdataRegionGraphComponents
  with ExtdataSitesServiceComponents
  with ExtdataAuctionResultComponents
  with ExtdataExtSiteStatisticsComponents
  with ExtdataExpectedMetroComponents
  with ExtdataVerbaComponents
  with ExtdataCompaniesComponents
  with ExtdataVillageComponents
  with ExtdataVillageDynamicInfoComponents
  with ExtdataCampaignComponents
  with CompletedSiteMortgageMatcherComponents
  with ExtdataParkComponents
  with ExtdataPondComponents {

  start()

}

trait BnbSearcherComponentTestExtdataResourceIndexStubs
  extends BnbSearcherComponentTestExtdataControllerProvider
  with DefaultComponentTestSearchContextComponents
  with ExtdataSitesSearchIndexResourceStub
  with ExtdataVillageIndexResourceStub
  with ExtdataRegionDocumentsStatisticsResourceStub
