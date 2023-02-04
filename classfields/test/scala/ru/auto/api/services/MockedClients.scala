package ru.auto.api.services

import ru.auto.api.extdata.DataService
import ru.auto.api.geo.Tree
import ru.auto.api.managers.incite.InciteManager
import ru.auto.api.managers.lenta.LentaManager
import ru.auto.api.services.application.ApplicationClient
import ru.auto.api.services.auction.{AuctionAutoStrategyClient, AuctionClient, DealerCallsAuctionClient, PromoCampaignClient}
import ru.auto.api.services.autoparts.avito.AutoPartsAvitoClient
import ru.auto.api.services.bigbrother.BigBrotherClient
import ru.auto.api.services.billing._
import ru.auto.api.services.booking.BookingClient
import ru.auto.api.services.c2b.{AuctionApplicationsClient, InternalAuctionApplicationsClient, InternalLotusClient, LotusClient}
import ru.auto.api.services.cabinet.CabinetApiClient
import ru.auto.api.services.callkeeper.CallKeeperClient
import ru.auto.api.services.calltracking.CalltrackingClient
import ru.auto.api.services.carfax.CarfaxClient
import ru.auto.api.services.carpauction.CarpAuctionClient
import ru.auto.api.services.cartinder.CartinderClient
import ru.auto.api.services.catalog.CatalogClient
import ru.auto.api.services.chat.ChatClient
import ru.auto.api.services.chatbot.ChatBotClient
import ru.auto.api.services.cme.CmeWidgetApiClient
import ru.auto.api.services.comeback.ComebackClient
import ru.auto.api.services.compare.CompareClient
import ru.auto.api.services.complaints.ComplaintsClient
import ru.auto.api.services.counter.CounterClient
import ru.auto.api.services.dealer_aliases.DealerAliasesClient
import ru.auto.api.services.dealer_pony.DealerPonyClient
import ru.auto.api.services.dealer_stats.DealerStatsClient
import ru.auto.api.services.favorite.FavoriteClient
import ru.auto.api.services.feedprocessor.FeedprocessorClient
import ru.auto.api.services.garage.GarageClient
import ru.auto.api.services.geobase.GeobaseClient
import ru.auto.api.services.heh.HehClient
import ru.auto.api.services.history.HistoryClient
import ru.auto.api.services.hydra.HydraClient
import ru.auto.api.services.iskra.IskraClient
import ru.auto.api.services.journal.JournalClient
import ru.auto.api.services.matchmaker.MatchMakerClient
import ru.auto.api.services.mds.PrivateMdsClient
import ru.auto.api.services.multiposting.{InternalMultipostingClient, MultipostingClient}
import ru.auto.api.services.notification.NotificationClient
import ru.auto.api.services.octopus.OctopusClient
import ru.auto.api.services.panorama.PanoramaClient
import ru.auto.api.services.parsing.ParsingClient
import ru.auto.api.services.passport.PassportClient
import ru.auto.api.services.personal.PersonalFavoritesClient
import ru.auto.api.services.phone.SmsClient
import ru.auto.api.services.phpapi.PhpApiClient
import ru.auto.api.services.predict.PredictClient
import ru.auto.api.services.predictbuyer.PredictBuyerClient
import ru.auto.api.services.promocoder.PromocoderClient
import ru.auto.api.services.promodispenser.PromoDispenserGrpcClient
import ru.auto.api.services.pushnoy.PushnoyClient
import ru.auto.api.services.recalls.RecallsClient
import ru.auto.api.services.recommender.RecommenderClient
import ru.auto.api.services.review.{ReviewClient, ReviewsSearcherClient, VosReviewClient}
import ru.auto.api.services.safedeal.SafeDealClient
import ru.auto.api.services.salesman.{SalesmanClient, SalesmanUserClient}
import ru.auto.api.services.searcher.SearcherClient
import ru.auto.api.services.searchline.SearchlineClient
import ru.auto.api.services.sender.SenderClient
import ru.auto.api.services.settings.SettingsClient
import ru.auto.api.services.shark.SharkClient
import ru.auto.api.services.statist.StatistClient
import ru.auto.api.services.stats.StatsClient
import ru.auto.api.services.stories.StoryClient
import ru.auto.api.services.subscriptions.{SubscriptionClient, UnsubscribeClient, WatchClient}
import ru.auto.api.services.telepony.{TeleponyCallsClient, TeleponyClient}
import ru.auto.api.services.tradein.TradeInNotifierClient
import ru.auto.api.services.uaas.UaaSClient
import ru.auto.api.services.uploader.UploaderClient
import ru.auto.api.services.video.VideoClient
import ru.auto.api.services.vos.VosClient
import ru.auto.api.services.vox.VoxClient
import ru.auto.api.services.web.WebClient
import ru.yandex.vertis.broker.client.simple.BrokerClient
import ru.yandex.vertis.commons.userticket.UserTicketsProcessor
import ru.yandex.vertis.mockito.MockitoSupport

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 08.02.17
  */
trait MockedClients extends MockitoSupport {

  lazy val inciteManager: InciteManager = mock[InciteManager]
  lazy val vosClient: VosClient = mock[VosClient]
  lazy val promoDispenserGrpcClient: PromoDispenserGrpcClient = mock[PromoDispenserGrpcClient]
  lazy val searcherClient: SearcherClient = mock[SearcherClient]
  lazy val bigBrotherClient: BigBrotherClient = mock[BigBrotherClient]
  lazy val counterClient: CounterClient = mock[CounterClient]
  lazy val statistClient: StatistClient = mock[StatistClient]
  lazy val passportClient: PassportClient = mock[PassportClient]
  lazy val subscriptionClient: SubscriptionClient = mock[SubscriptionClient]
  lazy val teleponyClient: TeleponyClient = mock[TeleponyClient]
  lazy val teleponyCallsClient: TeleponyCallsClient = mock[TeleponyCallsClient]
  lazy val moishaClient: MoishaClient = mock[MoishaClient]
  lazy val watchClient: WatchClient = mock[WatchClient]
  lazy val unsubscribeClient: UnsubscribeClient = mock[UnsubscribeClient]
  lazy val favoriteClient: FavoriteClient = mock[FavoriteClient]
  lazy val personalFavoritesClient: PersonalFavoritesClient = mock[PersonalFavoritesClient]
  lazy val compareClient: CompareClient = mock[CompareClient]
  lazy val historyClient: HistoryClient = mock[HistoryClient]
  lazy val uploaderClient: UploaderClient = mock[UploaderClient]
  lazy val panoramaClient: PanoramaClient = mock[PanoramaClient]
  lazy val reviewClient: ReviewClient = mock[ReviewClient]
  lazy val cabinetClient: CabinetClient = mock[CabinetClient]
  lazy val settingsClient: SettingsClient = mock[SettingsClient]
  lazy val phpApiClient: PhpApiClient = mock[PhpApiClient]
  lazy val statsClient: StatsClient = mock[StatsClient]
  lazy val predictClient: PredictClient = mock[PredictClient]
  lazy val senderClient: SenderClient = mock[SenderClient]
  lazy val smsClient: SmsClient = mock[SmsClient]
  lazy val chatClient: ChatClient = mock[ChatClient]
  lazy val notificationClient: NotificationClient = mock[NotificationClient]
  lazy val chatBotClient: ChatBotClient = mock[ChatBotClient]
  lazy val octopusClient: OctopusClient = mock[OctopusClient]
  lazy val pushnoyClient: PushnoyClient = mock[PushnoyClient]
  lazy val geobaseClient: GeobaseClient = mock[GeobaseClient]
  lazy val bankerClient: BankerClient = mock[BankerClient]
  lazy val salesmanClient: SalesmanClient = mock[SalesmanClient]
  lazy val salesmanUserClient: SalesmanUserClient = mock[SalesmanUserClient]
  lazy val dealerPonyClient: DealerPonyClient = mock[DealerPonyClient]
  lazy val dealerAliasesClient: DealerAliasesClient = mock[DealerAliasesClient]
  lazy val carfaxClient: CarfaxClient = mock[CarfaxClient]
  lazy val cartinderClient: CartinderClient = mock[CartinderClient]
  lazy val garageClient: GarageClient = mock[GarageClient]
  lazy val comebackClient: ComebackClient = mock[ComebackClient]
  lazy val calltrackingClient: CalltrackingClient = mock[CalltrackingClient]
  lazy val dataService: DataService = mock[DataService]
  lazy val vosReviewClient: VosReviewClient = mock[VosReviewClient]
  lazy val reviewsSearcherClient: ReviewsSearcherClient = mock[ReviewsSearcherClient]
  lazy val hydraClient: HydraClient = mock[HydraClient]
  lazy val videoClient: VideoClient = mock[VideoClient]
  lazy val parsingClient: ParsingClient = mock[ParsingClient]
  lazy val cabinetApiClient: CabinetApiClient = mock[CabinetApiClient]
  lazy val autoPartsAvitoClient: AutoPartsAvitoClient = mock[AutoPartsAvitoClient]
  lazy val webClient: WebClient = mock[WebClient]
  lazy val privateMdsClient: PrivateMdsClient = mock[PrivateMdsClient]
  lazy val callKeeperClient: CallKeeperClient = mock[CallKeeperClient]
  lazy val promocoderClient: PromocoderClient = mock[PromocoderClient]
  lazy val complaintsClient: ComplaintsClient = mock[ComplaintsClient]
  lazy val vsBillingClient: VsBillingClient = mock[VsBillingClient]
  lazy val vsBillingInternalClient: VsBillingInternalClient = mock[VsBillingInternalClient]
  lazy val safeDealClient: SafeDealClient = mock[SafeDealClient]
  lazy val sharkClient: SharkClient = mock[SharkClient]
  lazy val journalClient: JournalClient = mock[JournalClient]
  lazy val feedprocessorClient: FeedprocessorClient = mock[FeedprocessorClient]
  lazy val searchlineClient: SearchlineClient = mock[SearchlineClient]
  lazy val catalogClient: CatalogClient = mock[CatalogClient]
  lazy val recallsClient: RecallsClient = mock[RecallsClient]
  lazy val storyClient: StoryClient = mock[StoryClient]
  lazy val uaasClient: UaaSClient = mock[UaaSClient]
  lazy val matchMakerClient: MatchMakerClient = mock[MatchMakerClient]
  lazy val panoramasClient: PanoramaClient = mock[PanoramaClient]
  lazy val predictBuyerClient: PredictBuyerClient = mock[PredictBuyerClient]
  lazy val brokerClient: BrokerClient = mock[BrokerClient]
  lazy val iskraClient: IskraClient = mock[IskraClient]
  lazy val bookingClient: BookingClient = mock[BookingClient]
  lazy val applicationClient: ApplicationClient = mock[ApplicationClient]
  lazy val recommenderClient: RecommenderClient = mock[RecommenderClient]
  lazy val userTicketsProcessor: UserTicketsProcessor = mock[UserTicketsProcessor]
  lazy val voxClient: VoxClient = mock[VoxClient]
  lazy val multipostingClient: MultipostingClient = mock[MultipostingClient]
  lazy val internalMultipostingClient: InternalMultipostingClient = mock[InternalMultipostingClient]
  lazy val auctionClient: AuctionClient = mock[AuctionClient]
  lazy val auctionAutoStrategyClient: AuctionAutoStrategyClient = mock[AuctionAutoStrategyClient]
  lazy val treeMock: Tree = mock[Tree]
  lazy val tradeInNotifierClient: TradeInNotifierClient = mock[TradeInNotifierClient]
  lazy val dealerStatsClient: DealerStatsClient = mock[DealerStatsClient]
  lazy val cmeWidgetApiClient: CmeWidgetApiClient = mock[CmeWidgetApiClient]
  lazy val auctionApplicationsClient: AuctionApplicationsClient = mock[AuctionApplicationsClient]
  lazy val carpAuctionApplicationsClient: CarpAuctionClient = mock[CarpAuctionClient]
  lazy val lotusClient: LotusClient = mock[LotusClient]
  lazy val internalLotusClient: InternalLotusClient = mock[InternalLotusClient]
  lazy val lentaManager: LentaManager = mock[LentaManager]
  lazy val dealerCallsAuctionClient: DealerCallsAuctionClient = mock[DealerCallsAuctionClient]
  lazy val promoCampaignClient: PromoCampaignClient = mock[PromoCampaignClient]
  lazy val hehClient: HehClient = mock[HehClient]

  lazy val internalAuctionApplicationsClient: InternalAuctionApplicationsClient =
    mock[InternalAuctionApplicationsClient]
}
