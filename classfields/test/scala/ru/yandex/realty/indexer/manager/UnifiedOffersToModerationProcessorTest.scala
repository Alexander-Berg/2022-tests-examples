package ru.yandex.realty.indexer.manager

import org.mockito.Mockito.{never, reset, verify}
import org.scalatest.{AsyncFunSuite, BeforeAndAfter}
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.buildinginfo.storage.BuildingStorage
import ru.yandex.realty.bunker.BunkerResources
import ru.yandex.realty.clients.moderation.ModerationClient
import ru.yandex.realty.clients.vos.ng.VosClientNG
import ru.yandex.realty.graph.{MutableRegionGraph, RegionGraph}
import ru.yandex.realty.indexer.events.IndexerEvents.UnifiedOfferEvent
import ru.yandex.realty.indexer.processor.UnifiedOffersToModerationProcessor
import ru.yandex.realty.model.offer.SalesAgentCategory
import ru.yandex.realty.proto.offer.vos.OfferResponse.VosOfferResponse
import ru.yandex.realty.services.moderation.OfferToModerationConverter
import ru.yandex.realty.storage.CurrencyStorage
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.moderation.proto.Model.InstanceSource

import scala.concurrent.Future

class UnifiedOffersToModerationProcessorTest extends AsyncFunSuite with MockitoSupport with BeforeAndAfter {

  implicit private val traced: Traced = Traced.empty

  val moderationClient = mock[ModerationClient]
  val vosClientNG = mock[VosClientNG]
  val currencyStorageProvider = mock[Provider[CurrencyStorage]]
  val bunkerResourcesProvider = mock[Provider[BunkerResources]]
  val regionGraphProvider = mock[Provider[RegionGraph]]
  val buildingStorage = mock[BuildingStorage]
  val offerToModerationConverter = mock[OfferToModerationConverter]

  val unifiedOffersToModerationProcessor = new UnifiedOffersToModerationProcessor(
    moderationClient,
    vosClientNG,
    bunkerResourcesProvider,
    regionGraphProvider,
    buildingStorage,
    offerToModerationConverter
  )

  val unifiedOfferEvent = UnifiedOfferEvent
    .newBuilder()
    .build()

  before {
    reset(offerToModerationConverter)
    reset(vosClientNG)
  }

  test("nothing to do") {
    when(offerToModerationConverter.getCategoryForOffer(?)).thenReturn(Some(SalesAgentCategory.VERIFIER))

    unifiedOffersToModerationProcessor.process(unifiedOfferEvent).map { _ =>
      verify(vosClientNG, never()).getOffer(?, ?)(?)
      succeed
    }
  }

  test("process") {
    when(offerToModerationConverter.getCategoryForOffer(?)).thenReturn(Some(SalesAgentCategory.AGENCY))
    when(vosClientNG.getOffer(?, ?)(?)).thenReturn(Future.successful(Some(VosOfferResponse.newBuilder().build())))
    when(regionGraphProvider.get()).thenReturn(MutableRegionGraph.createEmptyRegionGraphWithAllFeatures)
    when(bunkerResourcesProvider.get())
      .thenReturn(BunkerResources(Map.empty, Set.empty, Set.empty, Set.empty, Set.empty, Set.empty, Map.empty, 0))
    when(offerToModerationConverter.toInstance(?, ?, ?, ?, ?)).thenReturn {
      val builder = InstanceSource.newBuilder().setVersion(1)
      builder.getExternalIdBuilder
        .setVersion(1)
        .getUserBuilder
        .setVersion(1)
        .setAutoruUser("123456")
      builder.build()
    }
    when(moderationClient.push(?)(?)).thenReturn(Future.unit)

    unifiedOffersToModerationProcessor.process(unifiedOfferEvent).map { _ =>
      verify(moderationClient).push(?)(?)
      succeed
    }
  }
}
