package ru.auto.api.managers.dealer.calls.auction

import org.scalatest.OptionValues
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.api.BaseSpec
import ru.auto.api.auth.Application
import ru.auto.api.exceptions.DealerCallsAuctionIsNotAvailableException
import ru.auto.api.extdata.DataService
import ru.auto.api.geo.Tree
import ru.auto.api.managers.aliases.AliasesManager
import ru.auto.api.managers.auction.DefaultDealerCallsAuctionManager
import ru.auto.api.model.ModelGenerators.{dealerSessionResultWithAccessGen, DealerOfferGen, OfferIDGen, RegionGen}
import ru.auto.api.model.{AutoruDealer, RequestParams, UserRef}
import ru.auto.api.services.auction.DealerCallsAuctionClient
import ru.auto.api.services.salesman.SalesmanClient
import ru.auto.api.services.vos.VosClient
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.cabinet.AclResponse.{AccessLevel, ResourceAlias}
import ru.auto.dealer_aliases.proto.v2.DealerAliasesService.{Alias, AliasTag, AliasType, Source}
import ru.auto.dealer_calls_auction.proto.ApiModel.AuctionCurrentState
import ru.auto.dealer_calls_auction.proto.ApiModel.AuctionCurrentState.Segment
import ru.auto.dealer_calls_auction.proto.OfferAuctionServiceOuterClass.GetAuctionCurrentStateBatchResponse
import ru.auto.dealer_calls_auction.proto.OfferAuctionServiceOuterClass.GetAuctionCurrentStateBatchResponse.ResponseItem
import ru.auto.salesman.calls.CallsTariffResponse.{CallTariff, CallTariffResponse}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

class DealerCallsAuctionManagerSpec extends BaseSpec with MockitoSupport with OptionValues {
  val auctionClient = mock[DealerCallsAuctionClient]
  val vosClient = mock[VosClient]
  val dataService = mock[DataService]
  val treeMock = mock[Tree]
  val aliasesManager = mock[AliasesManager]
  val salesmanClient = mock[SalesmanClient]

  val auctionManager = new DefaultDealerCallsAuctionManager(
    auctionClient,
    vosClient,
    dataService,
    salesmanClient
  )

  val clientId = 42

  def requestFunc(userId: Long, extId: Option[String], access: Map[ResourceAlias, AccessLevel]): Request = {
    val r = new RequestImpl
    r.setTrace(Traced.empty)
    r.setSession(dealerSessionResultWithAccessGen(access).next)
    r.setApplication(Application.web)
    r.setDealer(AutoruDealer(userId))
    r.setUser(UserRef.user(userId))
    r.setRequestParams(RequestParams.construct("1.1.1.1", externalDealerId = extId))
    r
  }

  "DealerCallsAuctionManager.getCurrentState()" should {
    "get current state" in {
      val offerID = OfferIDGen.next
      val offer = DealerOfferGen.next.toBuilder
        .setId(offerID.toPlain)
        .setCategory(Category.CARS)
        .setSection(Section.USED)
        .build()
      val autoruDealer = AutoruDealer(clientId)
      val access = Map(ResourceAlias.TARIFFS -> AccessLevel.READ_ONLY)
      val request = requestFunc(userId = clientId, extId = None, access)
      val regionId = offer.getSeller.getLocation.getGeobaseId
      val region = RegionGen.next.copy(id = regionId)
      val callTariffs = List(
        CallTariffResponse
          .newBuilder()
          .setCategory(Category.CARS)
          .setSection(Section.USED)
          .setCallTariff(CallTariff.SINGLE_WITH_CALLS)
          .build()
      )
      val state = AuctionCurrentState
        .newBuilder()
        .addSegments(
          Segment
            .newBuilder()
            .setMaxBid(100000)
            .setMinBid(50000)
            .setCurrent(true)
            .setPercent(5)
        )
      val response = GetAuctionCurrentStateBatchResponse
        .newBuilder()
        .addStates(
          ResponseItem
            .newBuilder()
            .setState(state)
        )
        .build

      when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
      when(salesmanClient.getAvailableCallTariffs(?)(?)).thenReturnF(callTariffs)
      when(dataService.tree).thenReturn(treeMock)
      when(treeMock.unsafeFederalSubject(?)).thenReturn(region)
      when(auctionClient.getCurrentStateBatch(?)(?)).thenReturnF(response)

      val res = auctionManager.getCurrentState(offerID, autoruDealer)(request).futureValue

      res.get.getSegments(0).getMinBid shouldBe state.getSegments(0).getMinBid
      res.get.getSegments(0).getMaxBid shouldBe state.getSegments(0).getMaxBid
      res.get.getSegments(0).getPercent shouldBe state.getSegments(0).getPercent
      res.get.getSegments(0).getCurrent shouldBe state.getSegments(0).getCurrent
    }

    "get error while getting current state" in {
      val offerID = OfferIDGen.next
      val offer = DealerOfferGen.next.toBuilder
        .setId(offerID.toPlain)
        .setCategory(Category.CARS)
        .setSection(Section.NEW)
        .build()
      val access = Map(ResourceAlias.TARIFFS -> AccessLevel.READ_ONLY)
      val request = requestFunc(userId = 42, extId = None, access)
      val regionId = offer.getSeller.getLocation.getGeobaseId
      val region = RegionGen.next.copy(id = regionId)
      val callTariffs = List(
        CallTariffResponse
          .newBuilder()
          .setCategory(Category.CARS)
          .setSection(Section.USED)
          .setCallTariff(CallTariff.SINGLE_WITH_CALLS)
          .build()
      )
      val state = AuctionCurrentState
        .newBuilder()
        .addSegments(
          Segment
            .newBuilder()
            .setMaxBid(100000)
            .setMinBid(50000)
            .setCurrent(true)
            .setPercent(5)
        )
      val response = GetAuctionCurrentStateBatchResponse
        .newBuilder()
        .addStates(
          ResponseItem
            .newBuilder()
            .setState(state)
        )
        .build

      when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
      when(salesmanClient.getAvailableCallTariffs(?)(?)).thenReturnF(callTariffs)
      when(dataService.tree).thenReturn(treeMock)
      when(treeMock.unsafeFederalSubject(?)).thenReturn(region)
      when(auctionClient.getCurrentStateBatch(?)(?)).thenReturnF(response)

      intercept[DealerCallsAuctionIsNotAvailableException] {
        auctionManager.getCurrentState(offerID, AutoruDealer(clientId))(request).await
      }
    }
    "get current state CME" in {
      val offerID = OfferIDGen.next
      val offer = DealerOfferGen.next.toBuilder
        .setId(offerID.toPlain)
        .setCategory(Category.CARS)
        .setSection(Section.USED)
        .build()
      val access = Map(ResourceAlias.TARIFFS -> AccessLevel.READ_ONLY)
      val request = requestFunc(userId = 42, extId = Some("43"), access)
      val regionId = offer.getSeller.getLocation.getGeobaseId
      val region = RegionGen.next.copy(id = regionId)
      val callTariffs = List(
        CallTariffResponse
          .newBuilder()
          .setCategory(Category.CARS)
          .setSection(Section.USED)
          .setCallTariff(CallTariff.SINGLE_WITH_CALLS)
          .build()
      )
      val state = AuctionCurrentState
        .newBuilder()
        .addSegments(
          Segment
            .newBuilder()
            .setMaxBid(100000)
            .setMinBid(50000)
            .setCurrent(true)
            .setPercent(5)
        )
      val response = GetAuctionCurrentStateBatchResponse
        .newBuilder()
        .addStates(
          ResponseItem
            .newBuilder()
            .setState(state)
        )
        .build

      val alias = Alias
        .newBuilder()
        .setAliasTag(AliasTag.Auction)
        .setAliasType(AliasType.External)
        .setSource(Source.CMExpert)
        .setExternalId("43")
        .build()

      when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
      when(salesmanClient.getAvailableCallTariffs(?)(?)).thenReturnF(callTariffs)
      when(dataService.tree).thenReturn(treeMock)
      when(treeMock.unsafeFederalSubject(?)).thenReturn(region)
      when(auctionClient.getCurrentStateBatch(?)(?)).thenReturnF(response)
      when(aliasesManager.lookupRoutes(?, ?)(?)).thenReturnF(List(alias))

      val res = auctionManager.getCurrentState(offerID, AutoruDealer(clientId))(request).futureValue

      res.get.getSegments(0).getMinBid shouldBe state.getSegments(0).getMinBid
      res.get.getSegments(0).getMaxBid shouldBe state.getSegments(0).getMaxBid
      res.get.getSegments(0).getPercent shouldBe state.getSegments(0).getPercent
      res.get.getSegments(0).getCurrent shouldBe state.getSegments(0).getCurrent
    }
  }

  "DealerCallsAuctionManager.getCurrentStateBatch()".should {
    "return batch auction state" in {
      val offer = DealerOfferGen.next.toBuilder
        .setUserRef("dealer:42")
        .setCategory(Category.CARS)
        .setSection(Section.USED)
        .build()

      val access = Map(ResourceAlias.TARIFFS -> AccessLevel.READ_ONLY)
      val request = requestFunc(userId = 42, extId = None, access)
      val regionId = offer.getSeller.getLocation.getGeobaseId
      val region = RegionGen.next.copy(id = regionId)
      val callTariffs = List(
        CallTariffResponse
          .newBuilder()
          .setCategory(Category.CARS)
          .setSection(Section.USED)
          .setCallTariff(CallTariff.SINGLE_WITH_CALLS)
          .build()
      )
      val state = AuctionCurrentState
        .newBuilder()
        .addSegments(
          Segment
            .newBuilder()
            .setMaxBid(100000)
            .setMinBid(50000)
            .setCurrent(true)
            .setPercent(5)
        )
      val response = GetAuctionCurrentStateBatchResponse
        .newBuilder()
        .addStates(
          ResponseItem
            .newBuilder()
            .setState(state)
        )
        .build

      when(salesmanClient.getAvailableCallTariffs(?)(?)).thenReturnF(callTariffs)
      when(dataService.tree).thenReturn(treeMock)
      when(treeMock.unsafeFederalSubject(?)).thenReturn(region)
      when(auctionClient.getCurrentStateBatch(?)(?)).thenReturnF(response)

      val res = auctionManager.getCurrentStateBatch(List(offer), AutoruDealer(clientId))(request).futureValue

      res.values.head.getSegments(0).getMinBid shouldBe state.getSegments(0).getMinBid
      res.values.head.getSegments(0).getMaxBid shouldBe state.getSegments(0).getMaxBid
      res.values.head.getSegments(0).getPercent shouldBe state.getSegments(0).getPercent
      res.values.head.getSegments(0).getCurrent shouldBe state.getSegments(0).getCurrent
    }

    "return filtered offers while getting batch auction state" in {
      val offerBuilder1 = DealerOfferGen.next.toBuilder
        .setUserRef("dealer:42")
        .setCategory(Category.CARS)
        .setSection(Section.NEW)

      val offerBuilder2 = DealerOfferGen.next.toBuilder
        .setUserRef("dealer:42")
        .setCategory(Category.CARS)
        .setSection(Section.USED)

      val offer = offerBuilder1.build()
      val access = Map(ResourceAlias.TARIFFS -> AccessLevel.READ_ONLY)
      val request = requestFunc(userId = 42, extId = None, access)
      val regionId = offer.getSeller.getLocation.getGeobaseId
      val region = RegionGen.next.copy(id = regionId)
      val callTariffs = List(
        CallTariffResponse
          .newBuilder()
          .setCategory(Category.CARS)
          .setSection(Section.USED)
          .setCallTariff(CallTariff.SINGLE_WITH_CALLS)
          .build()
      )
      val state = AuctionCurrentState
        .newBuilder()
        .addSegments(
          Segment
            .newBuilder()
            .setMaxBid(100000)
            .setMinBid(50000)
            .setCurrent(true)
            .setPercent(5)
        )
      val response = GetAuctionCurrentStateBatchResponse
        .newBuilder()
        .addStates(
          ResponseItem
            .newBuilder()
            .setState(state)
        )
        .build

      when(salesmanClient.getAvailableCallTariffs(?)(?)).thenReturnF(callTariffs)
      when(dataService.tree).thenReturn(treeMock)
      when(treeMock.unsafeFederalSubject(?)).thenReturn(region)
      when(auctionClient.getCurrentStateBatch(?)(?)).thenReturnF(response)

      val res = auctionManager
        .getCurrentStateBatch(List(offer, offerBuilder2.build()), AutoruDealer(clientId))(request)
        .futureValue

      res.size shouldBe 1
      res.values.head.getSegmentsList.size() shouldBe 1
      res.values.head.getSegments(0).getMinBid shouldBe state.getSegments(0).getMinBid
      res.values.head.getSegments(0).getMaxBid shouldBe state.getSegments(0).getMaxBid
      res.values.head.getSegments(0).getPercent shouldBe state.getSegments(0).getPercent
      res.values.head.getSegments(0).getCurrent shouldBe state.getSegments(0).getCurrent
    }

    "get batch current state CME" in {
      val offer1 = DealerOfferGen.next.toBuilder
        .setId("111-aaa")
        .setUserRef("dealer:42")
        .setCategory(Category.CARS)
        .setSection(Section.USED)
        .build()
      val offer2 = DealerOfferGen.next.toBuilder
        .setId("222-bbb")
        .setUserRef("dealer:42")
        .setCategory(Category.CARS)
        .setSection(Section.USED)
        .build()
      val access = Map(ResourceAlias.TARIFFS -> AccessLevel.READ_ONLY)
      val request = requestFunc(userId = 42, extId = None, access)
      val regionId = offer1.getSeller.getLocation.getGeobaseId
      val region = RegionGen.next.copy(id = regionId)
      val callTariffs = List(
        CallTariffResponse
          .newBuilder()
          .setCategory(Category.CARS)
          .setSection(Section.USED)
          .setCallTariff(CallTariff.SINGLE_WITH_CALLS)
          .build()
      )
      val state = AuctionCurrentState
        .newBuilder()
        .addSegments(
          Segment
            .newBuilder()
            .setMaxBid(100000)
            .setMinBid(50000)
            .setCurrent(true)
            .setPercent(5)
        )
      val response = GetAuctionCurrentStateBatchResponse
        .newBuilder()
        .addStates(
          ResponseItem
            .newBuilder()
            .setState(state)
        )
        .addStates(
          ResponseItem
            .newBuilder()
            .setState(state)
        )
        .build

      val alias = Alias
        .newBuilder()
        .setAliasTag(AliasTag.Auction)
        .setAliasType(AliasType.External)
        .setSource(Source.CMExpert)
        .setExternalId("43")
        .setDealerId(43)
        .build()

      when(salesmanClient.getAvailableCallTariffs(?)(?)).thenReturnF(callTariffs)
      when(dataService.tree).thenReturn(treeMock)
      when(treeMock.unsafeFederalSubject(?)).thenReturn(region)
      when(auctionClient.getCurrentStateBatch(?)(?)).thenReturnF(response)
      when(aliasesManager.lookupRoutes(?, ?)(?)).thenReturnF(List(alias))

      val res = auctionManager.getCurrentStateBatch(List(offer1, offer2), AutoruDealer(clientId))(request).futureValue

      res.size shouldBe 1
      res.head._2.getSegmentsList.size() shouldBe 1
      res.head._2.getSegments(0).getMinBid shouldBe state.getSegments(0).getMinBid
      res.head._2.getSegments(0).getMaxBid shouldBe state.getSegments(0).getMaxBid
      res.head._2.getSegments(0).getPercent shouldBe state.getSegments(0).getPercent
      res.head._2.getSegments(0).getCurrent shouldBe state.getSegments(0).getCurrent
    }
  }
}
