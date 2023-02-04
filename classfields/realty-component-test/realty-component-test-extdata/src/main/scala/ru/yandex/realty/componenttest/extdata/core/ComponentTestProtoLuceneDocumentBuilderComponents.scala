package ru.yandex.realty.componenttest.extdata.core

import ru.yandex.realty.application.providers.{
  CampaignComponents,
  ParkComponents,
  PondComponents,
  RegionGraphComponents
}
import ru.yandex.realty.application.providers.site.{ExtSiteStatisticsComponents, SitesServiceComponents}
import ru.yandex.realty.graph.DocumentBuilderHelper
import ru.yandex.realty.lucene.ProtoLuceneDocumentBuilder
import ru.yandex.realty.picapica.MdsUrlBuilder
import ru.yandex.realty.sites.CampaignService

trait ComponentTestProtoLuceneDocumentBuilderComponents {

  def protoLuceneDocumentBuilder: ProtoLuceneDocumentBuilder

}

trait DefaultComponentTestProtoLuceneDocumentBuilderComponents
  extends ComponentTestProtoLuceneDocumentBuilderComponents {
  self: RegionGraphComponents
    with SitesServiceComponents
    with ExtSiteStatisticsComponents
    with CampaignComponents
    with PondComponents
    with ParkComponents =>

  override lazy val protoLuceneDocumentBuilder: ProtoLuceneDocumentBuilder = {
    val documentBuilderHelper = new DocumentBuilderHelper(regionGraphProvider)
    val campaignService = new CampaignService(campaignProvider)

    new ProtoLuceneDocumentBuilder(
      regionGraphProvider,
      documentBuilderHelper,
      sitesService,
      extStatisticsProvider,
      campaignService,
      pondStorageProvider,
      parkStorageProvider,
      new MdsUrlBuilder("//realty.vertis.component-test.ru")
    )
  }

}
