package ru.yandex.realty.componenttest.extdata.core

import java.nio.file.Paths
import ru.yandex.realty.application.ng.{AppConfigProvider, DefaultOperationalProvider}
import ru.yandex.realty.application.providers.site.{ExtSiteStatisticsComponents, SitesServiceComponents}
import ru.yandex.realty.application.providers.{
  CampaignComponents,
  ParkComponents,
  PondComponents,
  RegionGraphComponents
}
import ru.yandex.realty.componenttest.data.offers.Offers
import ru.yandex.realty.message.ProtoIndexModificationMarshaller
import ru.yandex.realty.model.offer.Offer
import ru.yandex.realty.searcher.context.{SearchContext, SearchContextProvider}
import ru.yandex.realty.searcher.ng.processor.IndexUpdatesProcessor
import ru.yandex.realty.searcher.nrt.{NRTIndex, NRTIndexFactory}

trait ComponentTestSearchContextComponents {

  def searchContextProvider: SearchContextProvider[SearchContext]

}

trait DefaultComponentTestSearchContextComponents
  extends ComponentTestSearchContextComponents
  with DefaultComponentTestProtoLuceneDocumentBuilderComponents
  with DefaultOperationalProvider
  with AppConfigProvider
  with RegionGraphComponents
  with SitesServiceComponents
  with ExtSiteStatisticsComponents
  with CampaignComponents
  with PondComponents
  with ParkComponents {
  self: ComponentTestExtdataControllerProvider =>

  override lazy val searchContextProvider: SearchContextProvider[SearchContext] = {
    upsertOffers(Offers.all)
    readIndex()
  }

  private def upsertOffers(offers: Seq[Offer]): Unit = {
    val index = readIndex()
    val indexUpdateProcessor = new IndexUpdatesProcessor(index)
    val marshaller = new ProtoIndexModificationMarshaller(protoLuceneDocumentBuilder)
    offers.map(marshaller.upsertToMessage).foreach(indexUpdateProcessor.process)
    index.hardCommit()
    index.close()
  }

  private def readIndex(): NRTIndex = {
    NRTIndexFactory.createNRTIndexWithOps(Paths.get(s"$extDataPath/index"))(ops)
  }

}
