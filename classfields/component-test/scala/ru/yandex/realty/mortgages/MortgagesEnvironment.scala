package ru.yandex.realty.mortgages

import ru.yandex.realty.application.extdata.providers.bank.ExtdataBanksComponents
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
import ru.yandex.realty.application.CompletedSiteMortgageMatcherComponents
import ru.yandex.realty.componenttest.env.DefaultExtdataComponentTestEnvironment
import ru.yandex.realty.componenttest.extdata.core.{
  ComponentTestExtdataControllerProvider,
  DefaultComponentTestSearchContextComponents
}
import ru.yandex.realty.componenttest.extdata.stubs.{
  ExtdataAuctionResultResourceStub,
  ExtdataBankIndexResourceStub,
  ExtdataBanksResourceStub,
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
  ExtdataMortgageCalculatorParamsResourceStub,
  ExtdataMortgageCalculatorResourceStub,
  ExtdataMortgageProgramCanonicalUrlsResourceStub,
  ExtdataMortgageProgramConditionsResourceStub,
  ExtdataMortgageProgramIndexResourceStub,
  ExtdataParksResourceStub,
  ExtdataPondsResourceStub,
  ExtdataRawVillagesResourceStub,
  ExtdataRegionDocumentsStatisticsResourceStub,
  ExtdataRegionGraphResourceStub,
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
import ru.yandex.realty.mortgages.application.MortgagesAppConfig

object MortgagesEnvironment
  extends DefaultExtdataComponentTestEnvironment[MortgagesComponent, MortgagesAppConfig]
  with MortgagesComponentTestExtdataResourceStubs
  with MortgagesComponentTestExtdataControllerProvider
  with MortgagesComponentTestExtdataResourceIndexStubs {

  override def buildComponent(): MortgagesComponent = new MortgagesComponent

  override lazy val config: MortgagesAppConfig = MortgagesConfigBuilder.config

  override lazy val appConfig: AppConfig = config.appConfig
}

trait MortgagesComponentTestExtdataResourceStubs
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
  with ExtdataBanksResourceStub
  with ExtdataMortgageCalculatorResourceStub
  with ExtdataMortgageCalculatorParamsResourceStub
  with ExtdataVerbaDescriptionsResourceStub
  with ExtdataMortgageProgramConditionsResourceStub
  with ExtdataMortgageProgramCanonicalUrlsResourceStub
  with ExtdataBunkerResourceStub

trait MortgagesComponentTestExtdataControllerProvider
  extends ComponentTestExtdataControllerProvider
  with ExtdataRegionGraphComponents
  with ExtdataSitesServiceComponents
  with ExtdataAuctionResultComponents
  with ExtdataExtSiteStatisticsComponents
  with ExtdataExpectedMetroComponents
  with ExtdataVerbaComponents
  with ExtdataCompaniesComponents
  with ExtdataBanksComponents
  with ExtdataVillageComponents
  with ExtdataVillageDynamicInfoComponents
  with ExtdataCampaignComponents
  with CompletedSiteMortgageMatcherComponents
  with ExtdataParkComponents
  with ExtdataPondComponents {

  start()

}

trait MortgagesComponentTestExtdataResourceIndexStubs
  extends MortgagesComponentTestExtdataControllerProvider
  with DefaultComponentTestSearchContextComponents
  with ExtdataSitesSearchIndexResourceStub
  with ExtdataVillageIndexResourceStub
  with ExtdataRegionDocumentsStatisticsResourceStub
  with ExtdataBankIndexResourceStub
  with ExtdataMortgageProgramIndexResourceStub
