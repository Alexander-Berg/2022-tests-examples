package ru.auto.api

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection
import org.mockito.stubbing.OngoingStubbing
import org.scalatest._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.auto.api.ResponseModel.RawVinReportResponse
import ru.auto.api.app.Environment
import ru.auto.api.app.redis.DefaultRedisCache
import ru.auto.api.billing.BillingManager
import ru.auto.api.billing.booking.BookingBillingManager
import ru.auto.api.directives.RequestDirectives.wrapRequest
import ru.auto.api.event.{VertisEventProtoSender, VertisEventSender}
import ru.auto.api.extdata.DataService
import ru.auto.api.features.{CustomFeatureTypes, FeatureManager}
import ru.auto.api.managers.aliases.AliasesManager
import ru.auto.api.managers.antiparser.AntiParserManager
import ru.auto.api.managers.app2app.{App2AppHandleCryptoImpl, App2AppManager}
import ru.auto.api.managers.apple.AppleDeviceCheckManager
import ru.auto.api.managers.auction._
import ru.auto.api.managers.autoparts.AutoPartsManager
import ru.auto.api.managers.autoparts.avito.AutoPartsAvitoManager
import ru.auto.api.managers.autostrategies.AutostrategiesManager
import ru.auto.api.managers.balance.BalanceManager
import ru.auto.api.managers.billing.BankerManager
import ru.auto.api.managers.billing.schedule.ScheduleManager
import ru.auto.api.managers.billing.subscription.{SubscriptionManager => BillingSubscriptionsManager}
import ru.auto.api.managers.booking.BookingManager
import ru.auto.api.managers.broker.BrokerManager
import ru.auto.api.managers.c2b.carpauction.CarpAuctionManager
import ru.auto.api.managers.c2b.lotus.LotusManager
import ru.auto.api.managers.c2b.{AuctionApplicationsManager, AuctionCatalogWrapper, CanApplyLogger}
import ru.auto.api.managers.cabinet.{CabinetManager, CustomerAuthManager}
import ru.auto.api.managers.callback.PhoneCallbackManager.{CallKeeperApiKey, CallKeeperApiKeyConfig}
import ru.auto.api.managers.callback.{PhoneCallbackManager, PhoneCallbackMessageBuilder, PhoneCallbackOfferLoader}
import ru.auto.api.managers.calltracking.CalltrackingManager
import ru.auto.api.managers.carfax._
import ru.auto.api.managers.carfax.offer.{CarfaxOfferLoader, CarfaxOfferReportManager}
import ru.auto.api.managers.carfax.orders.CarfaxOrdersManager
import ru.auto.api.managers.carfax.report._
import ru.auto.api.managers.cartinder.CartinderManager
import ru.auto.api.managers.catalog.{CatalogDecayManager, CatalogManager}
import ru.auto.api.managers.chat.ChatManager
import ru.auto.api.managers.cme.CmeManager
import ru.auto.api.managers.comeback.ComebackManager
import ru.auto.api.managers.compare.{CompareManager, DictionaryCodeConverters, ModelsCompareBuilder, OffersCompareBuilder}
import ru.auto.api.managers.complaints.ComplaintsManager
import ru.auto.api.managers.counters.{CountersManager, StatistCountersManager}
import ru.auto.api.managers.dealer.{DealerManager, DealerStatsManager, DealerTariffManager, DefaultDealerBadgeManager, RequisitesManager}
import ru.auto.api.managers.decay.DecayManager
import ru.auto.api.managers.deeplink.DeeplinkManager
import ru.auto.api.managers.delivery.{DeliveryManager, DeliveryRegionProductPriceManager}
import ru.auto.api.managers.dictionaries.DictionariesManager
import ru.auto.api.managers.easysearch.EasySearchManager
import ru.auto.api.managers.enrich.enrichers.DealerServiceEnricher
import ru.auto.api.managers.enrich.{EnrichManager, ReviewsEnrichManager, SearchItemManager}
import ru.auto.api.managers.events.broker.c2b.C2bModelEventBuilder
import ru.auto.api.managers.events.broker.vas.VasModelEventBuilder
import ru.auto.api.managers.events.broker.{ModelEventBuilder, ModelEventChecker}
import ru.auto.api.managers.events.{BrokerEventsManager, StatEventsManager, VertisEventsManager}
import ru.auto.api.managers.fake.FakeManager
import ru.auto.api.managers.favorite._
import ru.auto.api.managers.features.AppsFeaturesManager
import ru.auto.api.managers.feedback.FeedbackManager
import ru.auto.api.managers.feeds.FeedManager
import ru.auto.api.managers.garage.enricher.{GarageUploaderConfig, GarageUploaderManager, PartnerPromosManager}
import ru.auto.api.managers.garage.{GarageCardEnrichManager, GarageDecayManager, GarageManager, GaragePromoDispenserManager}
import ru.auto.api.managers.geo.GeoManager
import ru.auto.api.managers.hello.{HelloManager, HelloManagerImpl}
import ru.auto.api.managers.history.HistoryManager
import ru.auto.api.managers.incite.InciteManager
import ru.auto.api.managers.iskra.IskraManager
import ru.auto.api.managers.lenta.LentaManager
import ru.auto.api.managers.magazine.{ArticleEnrichManager, MagazineManager}
import ru.auto.api.managers.matchapplications.MatchApplicationsManager
import ru.auto.api.managers.metrics.MetricsManager
import ru.auto.api.managers.notifications.NotificationManager
import ru.auto.api.managers.offers._
import ru.auto.api.managers.panorama.PanoramaManager
import ru.auto.api.managers.parsing.{DraftHandleCryptoImpl, ParsingManager}
import ru.auto.api.managers.passport.PassportManager
import ru.auto.api.managers.personalization.PersonalizationManager
import ru.auto.api.managers.photo.PhotosManager
import ru.auto.api.managers.price._
import ru.auto.api.managers.product.ProductManager
import ru.auto.api.managers.promo.PromoLandingManager
import ru.auto.api.managers.promocoder.PromocoderManager
import ru.auto.api.managers.ratelimit.RateLimitManager
import ru.auto.api.managers.recalls.{UserCardsManager, VehicleManager}
import ru.auto.api.managers.recommender.RecommenderManager
import ru.auto.api.managers.redemption.RedemptionManager
import ru.auto.api.managers.review.{CommentsManager, ReviewBrokerEventsManager, ReviewManager}
import ru.auto.api.managers.safedeal.SafeDealManager
import ru.auto.api.managers.salesman.CampaignManager
import ru.auto.api.managers.searcher.SearcherManager
import ru.auto.api.managers.searchline.{SearchlineHistoryManager, SearchlineManager}
import ru.auto.api.managers.shark.sberbank.SberbankIntegrationManager
import ru.auto.api.managers.shark.vtb.VtbIntegrationManager
import ru.auto.api.managers.shark.{SharkDecayManager, SharkManager}
import ru.auto.api.managers.stats.{NotificationStatsManager, StatsManager, ViewedNotificationsManager}
import ru.auto.api.managers.story.StoryManager
import ru.auto.api.managers.subscriptions.{DeviceSubscriptionsManager, SubscriptionsManager}
import ru.auto.api.managers.sync.SyncManager
import ru.auto.api.managers.tradein.{TradeInManager, TradeInSubscriptionsManager}
import ru.auto.api.managers.user.UserManager
import ru.auto.api.managers.validation.ValidationManager
import ru.auto.api.managers.video.VideoManager
import ru.auto.api.managers.vin.{HistoryReportPriceManager, VinResolutionManager, VinResolutionWalletManager}
import ru.auto.api.managers.wallet.WalletManager
import ru.auto.api.model.AutoruUser
import ru.auto.api.model.bunker.fake.FakePhonesList
import ru.auto.api.model.favorite.SavedSearchFactoryProvider
import ru.auto.api.model.moderation.Placeholders
import ru.auto.api.routes.v1.aliases.AliasesHandler
import ru.auto.api.routes.v1.auth.AuthHandler
import ru.auto.api.routes.v1.autoparts.AutoPartsHandler
import ru.auto.api.routes.v1.autoparts.avito.AutoPartsAvitoHandler
import ru.auto.api.routes.v1.autoservices.AutoServicesHandler
import ru.auto.api.routes.v1.autostrategies.AutostrategiesHandler
import ru.auto.api.routes.v1.billing.BillingHandler
import ru.auto.api.routes.v1.billing.schedules.SchedulesHandler
import ru.auto.api.routes.v1.billing.services.ServicesHandler
import ru.auto.api.routes.v1.billing.subscriptions.{SubscriptionsHandler => BillingSubscriptionsHandler}
import ru.auto.api.routes.v1.booking.BookingHandler
import ru.auto.api.routes.v1.c2bAuction.application.AuctionApplicationsHandler
import ru.auto.api.routes.v1.c2bAuction.carpauction.CarpAuctionHandler
import ru.auto.api.routes.v1.c2bAuction.lotus.LotusHandler
import ru.auto.api.routes.v1.cabinet.CabinetHandler
import ru.auto.api.routes.v1.calltracking.CalltrackingHandler
import ru.auto.api.routes.v1.carfax.CarfaxHandler
import ru.auto.api.routes.v1.cartinder.CartinderHandler
import ru.auto.api.routes.v1.chat.ChatHandler
import ru.auto.api.routes.v1.cme.CmeHandler
import ru.auto.api.routes.v1.comeback.ComebackHandler
import ru.auto.api.routes.v1.comments.CommentsHandler
import ru.auto.api.routes.v1.credits.CreditsHandler
import ru.auto.api.routes.v1.dealer.DealerHandler
import ru.auto.api.routes.v1.dealer.auction.calls.{DealerCallsAuctionHandler, PromoCampaignHandler}
import ru.auto.api.routes.v1.dealer.requisites.RequisitesHandler
import ru.auto.api.routes.v1.easysearch.EasySearchHandler
import ru.auto.api.routes.v1.electro.PromoLandingHandler
import ru.auto.api.routes.v1.events.EventsHandler
import ru.auto.api.routes.v1.feature.FeatureHandler
import ru.auto.api.routes.v1.feedback.FeedbackHandler
import ru.auto.api.routes.v1.feeds.FeedHandler
import ru.auto.api.routes.v1.garage.GarageHandler
import ru.auto.api.routes.v1.geo.GeoHandler
import ru.auto.api.routes.v1.grantgroup.GrantGroupHandler
import ru.auto.api.routes.v1.hello.DeviceHandler
import ru.auto.api.routes.v1.history.HistoryHandler
import ru.auto.api.routes.v1.incite.InciteHandler
import ru.auto.api.routes.v1.iskra.CarSubscriptionHandler
import ru.auto.api.routes.v1.lenta.LentaHandler
import ru.auto.api.routes.v1.magazine.MagazineHandler
import ru.auto.api.routes.v1.matchapplications.MatchApplicationsHandler
import ru.auto.api.routes.v1.notification.NotificationHandler
import ru.auto.api.routes.v1.offer.OfferCardHandler
import ru.auto.api.routes.v1.panorama.{ExteriorPanoramaHandler, InteriorPanoramaHandler}
import ru.auto.api.routes.v1.parsing.ParsingHandler
import ru.auto.api.routes.v1.personalization.PersonalizationHandler
import ru.auto.api.routes.v1.photo.PhotosHandler
import ru.auto.api.routes.v1.postingscripts.PostingScriptsIntegrationHandler
import ru.auto.api.routes.v1.products.ProductsHandler
import ru.auto.api.routes.v1.promocode.PromocodeHandler
import ru.auto.api.routes.v1.recalls.{NavigatorHandler, RecallsHandler}
import ru.auto.api.routes.v1.reference.catalog.CatalogHandler
import ru.auto.api.routes.v1.review.ReviewHandler
import ru.auto.api.routes.v1.safedeal.SafeDealHandler
import ru.auto.api.routes.v1.salon.SalonHandler
import ru.auto.api.routes.v1.search.SearchHandler
import ru.auto.api.routes.v1.searchline.SearchlineHandler
import ru.auto.api.routes.v1.sessions.SessionsHandler
import ru.auto.api.routes.v1.shark.SharkHandler
import ru.auto.api.routes.v1.stats.StatsHandler
import ru.auto.api.routes.v1.story.StoryHandler
import ru.auto.api.routes.v1.subscriptions.{DeviceSubscriptionsHandler, SubscriptionsHandler}
import ru.auto.api.routes.v1.targeting.TargetingHandler
import ru.auto.api.routes.v1.taxi.TaxiPromoHandler
import ru.auto.api.routes.v1.tradein.TradeInHandler
import ru.auto.api.routes.v1.unification.UnificationHandler
import ru.auto.api.routes.v1.user.app2app.App2AppHandler
import ru.auto.api.routes.v1.user.compare.CompareHandler
import ru.auto.api.routes.v1.user.confirm.UserConfirmationHandler
import ru.auto.api.routes.v1.user.draft.DraftHandler
import ru.auto.api.routes.v1.user.draft.photos.DraftPhotosHandler
import ru.auto.api.routes.v1.user.email.EmailHandler
import ru.auto.api.routes.v1.user.favorite.FavoriteHandler
import ru.auto.api.routes.v1.user.moderation.ModerationHandler
import ru.auto.api.routes.v1.user.notes.NotesHandler
import ru.auto.api.routes.v1.user.offers.{SingleOfferHandler, UserOffersHandler}
import ru.auto.api.routes.v1.user.password.PasswordHandler
import ru.auto.api.routes.v1.user.phones.PhonesHandler
import ru.auto.api.routes.v1.user.profile.UserProfileHandler
import ru.auto.api.routes.v1.user.reviews.UserReviewsHandler
import ru.auto.api.routes.v1.user.social.SocialProfilesHandler
import ru.auto.api.routes.v1.user.transaction.TransactionHandler
import ru.auto.api.routes.v1.video.VideoHandler
import ru.auto.api.routes.v1.vox.VoxHandler
import ru.auto.api.routes.{ApiHandlerBuilder, DocumentationHandler}
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
import ru.auto.api.services.keys.{TokenService, TokenServiceImpl}
import ru.auto.api.services.matchmaker.MatchMakerClient
import ru.auto.api.services.mds.PrivateMdsClient
import ru.auto.api.services.multiposting.{InternalMultipostingClient, MultipostingClient}
import ru.auto.api.services.notification.NotificationClient
import ru.auto.api.services.octopus.OctopusClient
import ru.auto.api.services.paging.offerPagingStreamer
import ru.auto.api.services.panorama.PanoramaClient
import ru.auto.api.services.parsing.ParsingClient
import ru.auto.api.services.passport.PassportClient
import ru.auto.api.services.passport.util.UserProfileStubsProvider
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
import ru.auto.api.services.settings.{RemoteConfigManager, SettingsClient}
import ru.auto.api.services.shark.{SharkClient, SharkTinkoffBankStatusUpdater}
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
import ru.auto.api.services.vox.{VoxClient, VoxManager}
import ru.auto.api.services.web.WebClient
import ru.auto.api.testkit.{TestData, TestEnvironment}
import ru.auto.api.util.TimeUtils.DefaultTimeProvider
import ru.auto.api.util._
import ru.auto.api.util.concurrency.FutureTimeoutHandler
import ru.auto.api.util.crypt.TypedCrypto
import ru.auto.api.util.crypt.aes256.{Aes256Crypto, Aes256KeyGen}
import ru.auto.api.util.crypt.base64.Base64
import ru.auto.api.util.crypt.blowfish.{BlowfishCrypto, BlowfishKeyGen}
import ru.auto.api.util.search.mappers.DefaultsMapper
import ru.auto.api.util.search.{SearchDiffUtils, SearchMappings, SearcherRequestMapper}
import ru.auto.api.util.time.{DefaultTimeService, TimeService}
import ru.yandex.vertis.broker.client.simple.BrokerClient
import ru.yandex.vertis.commons.userticket.UserTicketsProcessor
import ru.yandex.vertis.feature.impl.{BasicFeatureTypes, CompositeFeatureTypes, InMemoryFeatureRegistry}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 08.02.17
  */
// scalastyle:off number.of.methods
trait ApiSuiteBase
  extends SuiteMixin
  with ScalatestRouteTest
  with BeforeAndAfter
  with Matchers
  with JsonMatchers //with ParallelTestExecution
  with MockitoSupport
  with GeneratorUtils
  with ProtobufSupport
  with DummyOperationalSupport
  with Logging {
  suite: Suite =>

  lazy val tskvLogWriter: TskvLogWriter = new TskvLogWriter

  //noinspection ScalaStyle
  implicit protected val td = TildeArrow.injectIntoRoute

  def testAuthorizationHeader: HttpHeader = TokenServiceImpl.swagger.asHeader

  def xAuthorizationHeader: RequestTransformer = addHeader(testAuthorizationHeader)

  def vosClient: VosClient

  def promoDispenserGrpcClient: PromoDispenserGrpcClient

  def searcherClient: SearcherClient

  def bigBrotherClient: BigBrotherClient

  def vosReviewClient: VosReviewClient

  def favoriteClient: FavoriteClient

  def personalFavoritesClient: PersonalFavoritesClient

  def historyClient: HistoryClient

  def subscriptionClient: SubscriptionClient

  def watchClient: WatchClient

  def unsubscribeClient: UnsubscribeClient

  def compareClient: CompareClient

  def passportClient: PassportClient

  def recommenderClient: RecommenderClient

  def hehClient: HehClient

  def statsClient: StatsClient

  def predictClient: PredictClient

  def teleponyClient: TeleponyClient

  def teleponyCallsClient: TeleponyCallsClient

  def moishaClient: MoishaClient

  def uploaderClient: UploaderClient

  def panoramaClient: PanoramaClient

  def cabinetClient: CabinetClient

  def reviewClient: ReviewClient

  def reviewsSearcherClient: ReviewsSearcherClient

  def settingsClient: SettingsClient

  def senderClient: SenderClient

  def smsClient: SmsClient

  def chatClient: ChatClient

  def notificationClient: NotificationClient

  def chatBotClient: ChatBotClient

  def octopusClient: OctopusClient

  def geobaseClient: GeobaseClient

  def phpApiClient: PhpApiClient

  def bankerClient: BankerClient

  def salesmanUserClient: SalesmanUserClient

  def carfaxClient: CarfaxClient

  def cartinderClient: CartinderClient

  def garageClient: GarageClient

  def comebackClient: ComebackClient

  def calltrackingClient: CalltrackingClient

  def dataService: DataService

  def hydraClient: HydraClient

  def pushnoyClient: PushnoyClient

  def videoClient: VideoClient

  def parsingClient: ParsingClient

  def cabinetApiClient: CabinetApiClient

  def storyClient: StoryClient

  def autoPartsAvitoClient: AutoPartsAvitoClient

  def webClient: WebClient

  def salesmanClient: SalesmanClient

  def dealerPonyClient: DealerPonyClient

  def dealerAliasesClient: DealerAliasesClient

  def dealerStatsClient: DealerStatsClient

  def dealerCallsAuctionClient: DealerCallsAuctionClient

  def promoCampaignClient: PromoCampaignClient

  def tradeInNotifierClient: TradeInNotifierClient

  def applicationClient: ApplicationClient

  def auctionApplicationsClient: AuctionApplicationsClient

  def carpAuctionApplicationsClient: CarpAuctionClient

  def lotusClient: LotusClient

  def internalLotusClient: InternalLotusClient

  def internalAuctionApplicationsClient: InternalAuctionApplicationsClient

  def privateMdsClient: PrivateMdsClient

  def promocoderClient: PromocoderClient

  def callKeeperClient: CallKeeperClient

  def complaintsClient: ComplaintsClient

  def vsBillingClient: VsBillingClient

  def vsBillingInternalClient: VsBillingInternalClient

  def statistClient: StatistClient

  def safeDealClient: SafeDealClient

  def sharkClient: SharkClient

  def journalClient: JournalClient

  def feedprocessorClient: FeedprocessorClient

  def searchlineClient: SearchlineClient

  def catalogClient: CatalogClient

  def recallsClient: RecallsClient

  def uaasClient: UaaSClient

  def matchMakerClient: MatchMakerClient

  def predictBuyerClient: PredictBuyerClient

  def brokerClient: BrokerClient

  def iskraClient: IskraClient

  def bookingClient: BookingClient

  def voxClient: VoxClient

  def multipostingClient: MultipostingClient

  def internalMultipostingClient: InternalMultipostingClient

  def auctionClient: AuctionClient

  def auctionAutoStrategyClient: AuctionAutoStrategyClient

  def timeService: TimeService = new DefaultTimeService

  def userTicketsProcessor: UserTicketsProcessor

  def inciteManager: InciteManager

  def cmeWidgetApiClient: CmeWidgetApiClient

  def lentaManager: LentaManager

  lazy val featureRegistry = new InMemoryFeatureRegistry(
    new CompositeFeatureTypes(Seq(BasicFeatureTypes, CustomFeatureTypes))
  )

  lazy val featureManager = new FeatureManager(featureRegistry)

  lazy val defaultsMapper: DefaultsMapper = new DefaultsMapper(featureManager)
  lazy val searchMappings: SearchMappings = new SearchMappings(defaultsMapper, featureManager)
  lazy val searchDiffUtils: SearchDiffUtils = new SearchDiffUtils(searchMappings, defaultsMapper)
  lazy val savedSearchFactoryProvider = new SavedSearchFactoryProvider(searchMappings)

  lazy val antiParserManager = new AntiParserManager(settingsClient, hydraClient, featureManager)

  lazy val ownershipChecker = new OwnershipChecker(vosClient)

  lazy val remoteConfigManager = new RemoteConfigManager(settingsClient)

  lazy val appleDeviceCheckManager: AppleDeviceCheckManager = new AppleDeviceCheckManager(passportClient)

  lazy val helloManager: HelloManager =
    new HelloManagerImpl(
      pushnoyClient,
      uaasClient,
      antiParserManager,
      statEventsManager,
      remoteConfigManager,
      settingsClient,
      appleDeviceCheckManager,
      featureManager,
      TestData.forceUpdateVersions,
      environment
    )

  lazy val deviceSubscriptionsManager = new DeviceSubscriptionsManager(pushnoyClient)

  lazy val subscriptionsManager = new SubscriptionsManager(unsubscribeClient)

  lazy val urlBuilder =
    new UrlBuilder("https://test.avto.ru", "https://m.test.avto.ru", "https://parts.test.avto.ru", cryptoUserId)

  lazy val placeholders = new Placeholders(urlBuilder)

  lazy val selfAddress = "http://localhost:2600"
  lazy val publicAddress = selfAddress

  lazy val mdsHost = "avatars.mdst.yandex.net"

  lazy val optImageTtl = None

  lazy val userProfileStubs = UserProfileStubsProvider.Empty

  lazy val dictionariesManager = new DictionariesManager(TestData)

  lazy val decayManager =
    new DecayManager(passportClient, vinResolutionWalletManager, TestData.tree, featureManager)

  lazy val dealerPriceManager = new DealerPriceManager(moishaClient, vosClient, TestData)

  lazy val countersManager: CountersManager = new StatistCountersManager(statistClient)

  lazy val dealerServiceEnricher = new DealerServiceEnricher()

  lazy val catalogManager: CatalogManager =
    new CatalogManager(catalogClient, searcherClient, featureManager, dictionariesManager)

  lazy val dictionaryCodeConverters = new DictionaryCodeConverters(dictionariesManager)

  lazy val recommenderManager: RecommenderManager =
    new RecommenderManager(recommenderClient, compareManager, catalogManager, dictionaryCodeConverters)

  lazy val futureTimeoutHandler = new FutureTimeoutHandler

  lazy val fakeManager =
    new FakeManager(
      featureManager,
      catalogManager,
      TestOperationalSupport,
      FakePhonesList(Set.empty),
      urlBuilder,
      "testSalt"
    )

  lazy val enrichManager: EnrichManager = new EnrichManager(
    () => calltrackingManager,
    salesmanUserPriceManager,
    dealerPriceManager,
    dealerCallsAuctionManager,
    countersManager,
    searcherClient,
    favoriteManager,
    phoneRedirectManager,
    billingSchedulesManager,
    salesmanClient,
    dealerPonyClient,
    salesmanUserClient,
    urlBuilder,
    placeholders,
    passportClient,
    TestData,
    catalogManager,
    featureManager,
    productPriceManager,
    geobaseClient,
    new EmptyEnrichesCompletionMeter(),
    sharkClient,
    safeDealClient,
    matchApplicationsManager,
    dealerAliasesManager,
    teleponyClient,
    futureTimeoutHandler,
    fakeManager,
    inciteManager,
    apiWebAddress,
    cryptoUserId = cryptoUserId
  )

  lazy val reviewsEnrichManager = new ReviewsEnrichManager(
    passportClient,
    phpApiClient,
    vosReviewClient,
    catalogManager,
    hydraClient,
    TestData.reviewBanReasons,
    urlBuilder,
    userProfileStubs
  )

  lazy val phoneRedirectManager = new PhoneRedirectManager(
    teleponyClient,
    teleponyCallsClient,
    geobaseClient,
    dealerPonyClient,
    settingsClient,
    featureManager,
    TestData.tree,
    ttl = 1.day,
    minAvailableCount = 5,
    TestData.phoneRedirectInfo,
    urlBuilder,
    fakeManager
  )

  lazy val appsFeaturesManager = new AppsFeaturesManager(pushnoyClient, settingsClient)

  lazy val validationManager = new ValidationManager(carfaxClient, vosClient, phoneRedirectManager, appsFeaturesManager)

  lazy val offersManager: OffersManager =
    new OffersManager(
      vosClient,
      salesmanUserPriceManager,
      cabinetApiClient,
      decayManager,
      enrichManager,
      fakeManager,
      predictBuyerClient,
      brokerManager,
      teleponyClient,
      selfAddress,
      publicAddress,
      uploaderClient,
      salesmanClient,
      featureManager,
      passportManager,
      optImageTtl
    )

  lazy val brokerManager = new BrokerManager(brokerClient, vosClient)

  lazy val searchHistoryManager = new SearchHistoryManager(historyClient, searchMappings, searchItemManager)

  lazy val searchRequestMapper = new SearcherRequestMapper(appsFeaturesManager, featureManager)

  lazy val catalogDecayManager = new CatalogDecayManager

  lazy val promoLandingManager = new PromoLandingManager(
    searcherClient,
    decayManager,
    reviewsSearcherClient,
    catalogManager,
    garageClient,
    journalClient,
    compareManager,
    TestData.electroPromoLandingInfo,
    urlBuilder
  )

  lazy val searcherManager = new SearcherManager(
    searcherClient,
    enrichManager,
    decayManager,
    fakeManager,
    savedSearchesManager,
    offerPagingStreamer,
    searchRequestMapper,
    searchMappings,
    savedSearchFactoryProvider.offerSavedSearchFactory,
    matchApplicationsManager,
    recommenderClient,
    catalogManager,
    bigBrotherClient,
    brokerClient,
    TestData.tree,
    featureManager,
    catalogDecayManager,
    TestData.electroPromoLandingInfo
  )

  lazy val easySarcherManager = new EasySearchManager(
    searcherClient,
    searchMappings,
    decayManager,
    reviewClient
  )

  lazy val syncManager = new SyncManager(
    prometheusRegistryDummy,
    favoriteManager,
    favoriteClient,
    savedSearchesManager,
    historyClient,
    watchManager,
    subscriptionClient,
    reviewManager,
    0
  )

  lazy val passportManager =
    new PassportManager(passportClient, pushnoyClient, syncManager, appsFeaturesManager, fakeManager)

  lazy val app2AppManager = new App2AppManager(
    passportClient,
    offerLoader,
    enrichManager,
    settingsClient,
    featureManager,
    urlBuilder,
    app2appHandleCrypto
  )

  lazy val personalizationManager =
    new PersonalizationManager(
      searcherClient,
      bigBrotherClient,
      brokerClient,
      enrichManager,
      decayManager,
      recommenderClient,
      searchMappings,
      hehClient,
      geobaseClient,
      TestData.tree,
      featureManager
    )

  lazy val statsManager = new StatsManager(statsClient, vosClient, predictClient, catalogClient)

  private lazy val crypto = {
    val key = "pc269QNr0qzpYkpou5QvtY3ySCVMdpYBoXzLs/629u8="
    new Aes256Crypto(Aes256KeyGen.fromBase64Str(key))
  }

  protected lazy val cryptoUserId = {
    val key = "Im0QL3c6TVxkvkPnFZcz5w=="
    TypedCrypto.userId(
      new BlowfishCrypto(BlowfishKeyGen.fromBase64Str(key), Base64.UrlSafe)
    )
  }

  lazy val statEventsManager = new StatEventsManager(
    offerLoader,
    reviewsSearcherClient,
    vosReviewClient,
    geobaseClient,
    tskvLogWriter,
    remoteConfigManager,
    crypto,
    prometheusRegistryDummy,
    featureManager
  )

  private lazy val modelEventChecker = new ModelEventChecker(TestData.tree)

  lazy val brokerEventBuilder =
    new ModelEventBuilder(
      offerLoader,
      reviewClient,
      geobaseClient,
      pushnoyClient,
      dealerPonyClient,
      TestData.tree,
      journalClient,
      videoClient
    )

  private lazy val brokerVasEventBuilder =
    new VasModelEventBuilder(
      offerLoader,
      geobaseClient,
      pushnoyClient,
      TestData.tree,
      reviewClient,
      journalClient,
      videoClient
    )

  private lazy val c2bAuctionEventBuilder =
    new C2bModelEventBuilder(
      offerLoader,
      geobaseClient,
      pushnoyClient,
      TestData.tree,
      reviewClient,
      journalClient,
      videoClient,
      auctionApplicationsManager,
      draftsManager
    )

  lazy val brokerEventsManager =
    new BrokerEventsManager(
      brokerEventBuilder,
      brokerVasEventBuilder,
      c2bAuctionEventBuilder,
      modelEventChecker,
      brokerClient,
      prometheusRegistryDummy,
      featureManager,
      crypto,
      reviewsSearcherClient,
      reviewClient
    )

  implicit private val environment: Environment = TestEnvironment
  private val vertisEventProtoSender: VertisEventProtoSender = new VertisEventProtoSender(brokerClient)

  private val vertisEventSender: VertisEventSender =
    new VertisEventSender(vertisEventProtoSender, pushnoyClient)
  lazy val vertisEventsManager: VertisEventsManager = new VertisEventsManager(vertisEventSender)

  lazy val searchItemManager = new SearchItemManager(
    appsFeaturesManager,
    catalogManager,
    searcherClient,
    TestData.tree,
    searchDiffUtils,
    searchMappings
  )

  lazy val savedSearchesManager = new SavedSearchesManager(
    statEventsManager,
    searchItemManager,
    favoriteClient,
    subscriptionClient,
    searcherClient,
    pushnoyClient,
    savedSearchFactoryProvider,
    catalogManager
  )

  lazy val salesmanUserPriceManager = new SalesmanUserPriceManager(salesmanUserClient, passportClient)

  lazy val draftsManager =
    new DraftsManager(
      DefaultTimeProvider,
      vosClient,
      dealerPriceManager,
      salesmanUserPriceManager,
      uploaderClient,
      cabinetClient,
      cabinetApiClient,
      settingsClient,
      passportClient,
      tradeInNotifierClient,
      statEventsManager,
      phoneRedirectManager,
      validationManager,
      geobaseClient,
      TestData.tree,
      selfAddress,
      decayManager,
      enrichManager,
      imageTtl = None,
      featureManager,
      carfaxDraftsManager,
      garageManager,
      futureTimeoutHandler,
      draftHandleCrypto,
      brokerClient,
      "https://test.avto.ru",
      salesmanClient
    )

  private lazy val auctionCatalogWrapper = new AuctionCatalogWrapper(catalogManager)

  private lazy val auctionApplicationsManager =
    new AuctionApplicationsManager(
      auctionApplicationsClient,
      vosClient,
      internalAuctionApplicationsClient,
      settingsClient,
      draftsManager,
      dataService,
      enrichManager,
      log,
      carfaxManager,
      essentialsReportManager,
      passportClient,
      passportManager,
      catalogClient,
      auctionCatalogWrapper,
      new CanApplyLogger(brokerClient, timeService, log)
    )

  private lazy val carpAuctionApplicationsManager =
    new CarpAuctionManager(carpAuctionApplicationsClient, vosClient, draftsManager, settingsClient, enrichManager, log)

  private lazy val lotusManager = new LotusManager(lotusClient, internalLotusClient)

  lazy val enrichedOfferLoader =
    new EnrichedOfferLoader(vosClient, searcherClient, enrichManager, decayManager, fakeManager, settingsClient)

  lazy val offerLoader = new OfferLoader(vosClient, searcherClient, settingsClient)

  lazy val offersCompareBuilder =
    new OffersCompareBuilder(offerLoader, catalogManager, carfaxClient, dictionaryCodeConverters)

  lazy val modelsCompareBuilder =
    new ModelsCompareBuilder(catalogManager, searcherClient, searchMappings, dictionaryCodeConverters)

  lazy val compareManager =
    new CompareManager(
      compareClient,
      offerLoader,
      offersCompareBuilder,
      modelsCompareBuilder,
      catalogManager,
      dataService,
      geoManager,
      decayManager
    )

  private lazy val app2appHandleCrypto = {
    val key = "6iLd2Fo6oet9u62Zpr8DJw=="
    new App2AppHandleCryptoImpl(new BlowfishCrypto(BlowfishKeyGen.fromBase64Str(key)))
  }

  private lazy val draftHandleCrypto = {
    val key = "gloSmh7KdacII/UNruN9Rg=="
    new DraftHandleCryptoImpl(new BlowfishCrypto(BlowfishKeyGen.fromBase64Str(key), base64 = Base64.UrlSafe2))
  }

  private lazy val phoneViewNeedAuthManager =
    new PhoneViewNeedAuthManager(
      TestData.tree,
      TestData.phoneViewWithAuth,
      passportClient,
      hydraClient,
      prometheusRegistryDummy,
      redisCache,
      featureManager
    )

  lazy val offerCardManager = new OfferCardManager(
    enrichedOfferLoader,
    searcherClient,
    historyClient,
    decayManager,
    enrichManager,
    fakeManager,
    phoneRedirectManager,
    antiParserManager,
    statEventsManager,
    TestData.tree,
    watchManager,
    featureManager,
    recommenderClient,
    bigBrotherClient,
    brokerClient,
    app2appHandleCrypto,
    countersManager,
    phoneViewNeedAuthManager,
    hehClient,
    geobaseClient
  )

  lazy val cmeManager = new CmeManager(
    cmeWidgetAddress,
    cmeWidgetApiClient,
    dealerAliasesManager,
    phoneRedirectManager,
    featureManager,
    metricsManager,
    countersManager,
    offerCardManager,
    DefaultTimeProvider
  )

  lazy val draftPhotosManager = new DraftPhotosManager(vosClient)

  lazy val watchManager = new WatchManager(watchClient, brokerClient)

  lazy val favoritesHelper = new FavoritesHelper(personalFavoritesClient)

  lazy val recommendedHelper =
    new RecommendedHelper(searcherClient, recommenderClient, geobaseClient, bigBrotherClient, brokerClient)

  lazy val favoriteManager =
    new FavoriteManager(favoriteClient, favoritesHelper, watchManager, statEventsManager, brokerClient, featureManager)

  lazy val favoriteManagerWrapper = new FavoriteManagerWrapper(favoriteManager)

  lazy val favoriteListingManager =
    new FavoriteListingManager(
      favoritesHelper,
      recommendedHelper,
      offerCardManager,
      searcherClient,
      enrichManager,
      decayManager,
      fakeManager,
      favoriteManager
    )

  lazy val searchesManager = new SearchesManager(favoriteClient, subscriptionClient, passportClient)

  lazy val offerStatManager =
    new OfferStatManager(countersManager, vosClient, teleponyClient, hydraClient, featureManager, app2AppManager)

  private lazy val reviewBrokerEventsManager: ReviewBrokerEventsManager = new ReviewBrokerEventsManager(
    reviewsSearcherClient,
    reviewClient,
    brokerEventBuilder,
    brokerClient
  )

  lazy val reviewManager = new ReviewManager(
    statEventsManager,
    reviewsSearcherClient,
    phpApiClient,
    reviewClient,
    uploaderClient,
    enrichedOfferLoader,
    privateMdsClient,
    reviewsEnrichManager,
    selfAddress,
    mdsHost,
    optImageTtl,
    reviewBrokerEventsManager,
    featureManager
  )

  lazy val articleEnrichManager = new ArticleEnrichManager(catalogManager)

  lazy val magazineManager = new MagazineManager(statEventsManager, reviewsSearcherClient, articleEnrichManager)

  lazy val commentManager: CommentsManager =
    new CommentsManager(statEventsManager, phpApiClient, userProfileStubs, featureManager, reviewBrokerEventsManager)

  lazy val notificationManager: NotificationManager = new NotificationManager(notificationClient)

  lazy val chatManager: ChatManager =
    new ChatManager(
      chatClient,
      chatBotClient,
      enrichedOfferLoader,
      passportClient,
      pushnoyClient,
      senderClient,
      uploaderClient,
      TestData.chatBotInfo,
      selfAddress,
      mdsHost,
      optImageTtl,
      appsFeaturesManager,
      featureManager,
      app2appHandleCrypto
    )

  lazy val bankerManager = new BankerManager(bankerClient, passportClient)

  lazy val promocoderManager = new PromocoderManager(promocoderClient)

  lazy val bookingBillingManager = new BookingBillingManager(offerLoader, bookingClient, bankerClient)

  lazy val billingManager = new BillingManager(
    offersManager,
    bankerManager,
    bankerClient,
    salesmanUserClient,
    billingSubscriptionsManager,
    bookingBillingManager,
    garageManager,
    featureManager,
    fakeManager,
    uaasClient
  )

  val billingSchedulesManager = new ScheduleManager(salesmanClient, salesmanUserClient, ownershipChecker)

  val billingSubscriptionsManager = new BillingSubscriptionsManager(salesmanUserClient)

  lazy val rateLimitManager = new RateLimitManager(hydraClient, "statistic_api")

  lazy val videoManager = new VideoManager(videoClient, TestData.videoSearchBlackList, catalogManager)

  lazy val autoPartsManager = new AutoPartsManager()

  lazy val autoPartsAvitoManager = new AutoPartsAvitoManager(autoPartsAvitoClient)

  lazy val parsingManager =
    new ParsingManager(draftsManager, parsingClient, passportManager, draftHandleCrypto, vosClient)

  lazy val autostrategiesManager = new AutostrategiesManager(salesmanClient, ownershipChecker)

  lazy val photosManager = new PhotosManager(uploaderClient, privateMdsClient, searcherClient)

  lazy val panoramaManager =
    new PanoramaManager(uploaderClient, panoramaClient, selfAddress, privateMdsClient)

  lazy val geoManager = new GeoManager(geobaseClient, searcherClient, TestData.tree, TestData.geoSuggestListing)

  lazy val deeplinkManager = new DeeplinkManager(
    webClient,
    searchItemManager,
    offerCardManager,
    TestData.tree,
    geoManager,
    "https://m.test.avto.ru",
    searchMappings
  )

  lazy val productManager =
    new ProductManager(
      vosClient,
      cabinetClient,
      cabinetApiClient,
      salesmanClient,
      salesmanUserClient,
      multipostingClient,
      offersManager,
      featureManager,
      TestData.tree
    )

  val phoneCallbackOfferLoader = new PhoneCallbackOfferLoader(offerLoader, enrichManager)

  lazy val phoneCallbackMessageBuilder = new PhoneCallbackMessageBuilder(
    dataService,
    defaultMessage = "test text"
  )

  lazy val phoneCallbackManager = new PhoneCallbackManager(
    callKeeperClient,
    teleponyClient,
    dealerPonyClient,
    phoneCallbackOfferLoader,
    TestData.tree,
    CallKeeperApiKeyConfig(Map.empty, CallKeeperApiKey("", "")),
    phoneRedirectManager,
    searcherClient,
    vosClient,
    phoneCallbackMessageBuilder
  )

  lazy val campaignManager =
    new CampaignManager(salesmanClient, cabinetApiClient, vsBillingClient, vsBillingInternalClient, moishaClient)

  lazy val complaintsManager = new ComplaintsManager(vosClient, complaintsClient)

  lazy val balanceManager = new BalanceManager(cabinetApiClient)

  lazy val metricsManager = new MetricsManager(dealerStatsClient)

  private lazy val carfaxWalletManager = new CarfaxWalletManager(
    billingSubscriptionsManager,
    salesmanClient
  )

  private lazy val historyReportPriceManager =
    new HistoryReportPriceManager(moishaClient, billingSubscriptionsManager, geobaseClient)

  val carfaxOfferLoader = new CarfaxOfferLoader(offerLoader, enrichManager)

  private lazy val essentialsReportManager = new EssentialsReportManager(
    carfaxClient,
    carfaxOfferLoader
  )

  lazy val carfaxManager =
    new CarfaxManager(
      carfaxClient,
      carfaxWalletManager,
      essentialsReportManager
    )

  lazy val cartinderManager =
    new CartinderManager(
      cartinderClient,
      searchMappings,
      decayManager,
      offerLoader,
      favoriteManager
    )

  lazy val easySearchManager =
    new EasySearchManager(
      searcherClient,
      searchMappings,
      decayManager,
      reviewClient
    )

  lazy val garageImageUploaderConfig = {
    val host = Try(environment.config.getString("autoru.api.garage.host")).getOrElse("")
    val port = Try(environment.config.getString("autoru.api.garage.port")).getOrElse("")
    val garageUrl = s"http://$host:$port"
    GarageUploaderConfig(optImageTtl, garageUrl)
  }

  lazy val promoDispenserManager: GaragePromoDispenserManager =
    new GaragePromoDispenserManager(promoDispenserGrpcClient)

  private lazy val partnerPromosManager = new PartnerPromosManager(featureManager, promoDispenserManager)

  lazy val garageEnrichManager =
    new GarageCardEnrichManager(
      magazineManager,
      statsManager,
      carfaxManager,
      rawCarfaxReportManager,
      geobaseClient,
      searcherManager,
      searchItemManager,
      reviewManager,
      userCardsManager,
      catalogManager,
      partnerPromosManager,
      TestData.tree,
      garageClient,
      featureManager,
      sharkManager
    )

  lazy val garageDecayManager = new GarageDecayManager(appsFeaturesManager, featureManager)

  private lazy val garageUploader = new GarageUploaderManager(uploaderClient, garageImageUploaderConfig)

  lazy val garageManager: GarageManager = {
    implicit val m = TestOperationalSupport
    new GarageManager(
      garageClient,
      vosClient,
      garageEnrichManager,
      carfaxManager,
      partnerPromosManager,
      rawCarfaxReportManager,
      garageDecayManager,
      garageUploader,
      geoManager,
      featureManager
    )
  }

  lazy val carfaxCommentsManager: CarfaxCommentsManager = {
    val host = Try(environment.config.getString("autoru.api.vin-decoder.host")).getOrElse("")
    val port = Try(environment.config.getString("autoru.api.vin-decoder.port")).getOrElse("")
    val carfaxUrl = s"http://$host:$port"
    new CarfaxCommentsManager(carfaxClient, offersManager, uploaderClient, carfaxUrl, optImageTtl)
  }

  lazy val carfaxDraftsManager = new CarfaxDraftsManager(carfaxClient, featureManager)

  lazy val rawReportLoader = new RawReportLoader(carfaxClient, featureManager)

  lazy val carfaxDecayManager =
    new CarfaxDecayManager(
      carfaxWalletManager,
      vinResolutionWalletManager,
      featureManager,
      TestData.tree,
      TestData.resellersWithFreeReportAccess
    )

  lazy val rawCarfaxReportManager =
    new CarfaxReportManager(
      rawReportLoader,
      carfaxManager,
      carfaxDecayManager,
      carfaxWalletManager,
      vinResolutionWalletManager,
      historyReportPriceManager,
      featureManager,
      fakeManager
    )

  lazy val userCardsManager = new UserCardsManager(recallsClient, carfaxManager, passportClient)

  lazy val vehicleManager = new VehicleManager(recallsClient, carfaxManager)

  lazy val historyManager = new HistoryManager(historyClient, enrichedOfferLoader)

  lazy val storyManager = {
    new StoryManager(storyClient, geobaseClient, historyManager)
  }

  lazy val walletManager = new WalletManager(
    vsBillingClient,
    dealerStatsManager,
    balanceManager,
    enrichedOfferLoader,
    DefaultTimeProvider
  )

  lazy val dealerManager = new DealerManager(
    cabinetApiClient,
    searcherClient,
    salesmanClient,
    dealerPonyClient,
    phoneRedirectManager,
    vsBillingClient,
    passportClient,
    dataService,
    enrichedOfferLoader,
    calltrackingClient,
    promocoderClient,
    vosClient,
    fakeManager
  )

  lazy val customerAuthManager = new CustomerAuthManager(cabinetApiClient)

  lazy val cabinetManager = new CabinetManager(cabinetApiClient, settingsClient)

  lazy val productPriceManager =
    new DeliveryRegionProductPriceManager(
      offerLoader,
      moishaClient,
      geobaseClient,
      dataService,
      salesmanClient,
      featureManager
    )

  lazy val redisCache =
    new DefaultRedisCache(
      ViewedNotificationsManager.RedisPrefix,
      mock[StatefulRedisMasterReplicaConnection[Array[Byte], Array[Byte]]]
    )

  lazy val viewedNotificationsManager = new ViewedNotificationsManager(redisCache)

  lazy val notificationsManager = new NotificationStatsManager(
    DefaultTimeProvider,
    offerStatManager,
    viewedNotificationsManager,
    statEventsManager
  )

  lazy val vinResolutionWalletManager = new VinResolutionWalletManager(
    billingSubscriptionsManager,
    salesmanClient
  )

  lazy val vinResolutionManager = new VinResolutionManager(
    appsFeaturesManager,
    offerLoader,
    carfaxClient,
    vosClient,
    DefaultTimeProvider
  )

  lazy val rawCarfaxOfferReportManager = new CarfaxOfferReportManager(
    rawReportLoader,
    carfaxManager,
    carfaxWalletManager,
    vinResolutionWalletManager,
    historyReportPriceManager,
    carfaxDecayManager,
    carfaxOfferLoader,
    vosClient,
    DefaultTimeProvider,
    featureManager,
    fakeManager,
    favoriteManagerWrapper
  )

  lazy val carfaxBoughtReportsManager = new CarfaxBoughtReportsManager(
    carfaxWalletManager,
    rawCarfaxReportManager,
    rawCarfaxOfferReportManager,
    favoriteManagerWrapper,
    carfaxOrdersManager
  )

  lazy val carfaxOrdersManager = new CarfaxOrdersManager(carfaxClient, carfaxOfferLoader)

  lazy val feedbackManager = new FeedbackManager(senderClient, passportClient, TimeUtils.DefaultTimeProvider)

  lazy val safeDealAddress = {
    val host = Try(environment.config.getString("autoru.api.safe-deal.host")).getOrElse("")
    val port = Try(environment.config.getString("autoru.api.safe-deal.port")).getOrElse("")
    s"http://$host:$port"
  }

  lazy val apiWebAddress = {
    val host = environment.config.getString("autoru.api.web.host")
    val scheme = environment.config.getString("autoru.api.web.scheme")
    s"$scheme://$host"
  }

  lazy val cmeWidgetAddress = Try(environment.config.getString("autoru.api.cme.widget.address")).getOrElse("")

  lazy val safeDealManager =
    new SafeDealManager(safeDealClient, safeDealAddress, apiWebAddress, offerLoader, uploaderClient, favoriteManager)

  lazy val sharkDecayManager = new SharkDecayManager()

  lazy val sharkManager = new SharkManager(
    sharkClient,
    enrichedOfferLoader,
    settingsClient,
    sharkDecayManager,
    featureManager,
    dataService
  )

  lazy val vtbIntegrationManager = new VtbIntegrationManager(
    sharkClient,
    brokerClient,
    environment.config.getString("autoru.api.integration.vtb.product-name")
  )

  lazy val sberbankIntegrationManager = new SberbankIntegrationManager(
    sharkClient,
    brokerClient
  )

  private lazy val sharkTinkoffBankStatusUpdater = new SharkTinkoffBankStatusUpdater(sharkClient)

  lazy val dealerBadgeManager =
    new DefaultDealerBadgeManager(
      cabinetApiClient,
      moishaClient,
      DefaultTimeProvider,
      vosClient,
      TestData,
      searcherManager
    )

  lazy val dealerTariffManager =
    new DealerTariffManager(
      salesmanClient,
      cabinetApiClient,
      searcherClient,
      dealerBadgeManager,
      featureManager
    )

  lazy val dealerStatsManager = new DealerStatsManager(dealerStatsClient)

  lazy val redemptionManager = new RedemptionManager(phoneCallbackManager, cabinetApiClient)

  lazy val tradeInManager =
    new TradeInManager(
      salesmanClient,
      vosClient,
      enrichManager,
      phoneCallbackManager,
      decayManager
    )

  lazy val matchApplicationsManager =
    new MatchApplicationsManager(
      salesmanClient,
      matchMakerClient,
      catalogManager,
      passportManager,
      featureManager,
      searcherClient,
      TestData.tree
    )

  lazy val deliveryManager = new DeliveryManager(vosClient, offerLoader, geobaseClient, TestData.tree)

  lazy val comebackManager = new ComebackManager(
    comebackClient,
    carfaxClient,
    enrichedOfferLoader
  )

  lazy val dealerAliasesManager = new AliasesManager(dealerAliasesClient)

  lazy val calltrackingManager =
    new CalltrackingManager(
      calltrackingClient,
      enrichedOfferLoader,
      salesmanClient,
      balanceManager,
      vsBillingClient,
      teleponyCallsClient,
      teleponyClient,
      dealerPonyClient,
      dealerAliasesManager
    )

  lazy val iskraManager =
    new IskraManager(iskraClient)

  lazy val bookingManager = new BookingManager(bookingClient, enrichedOfferLoader)

  lazy val voxManager: VoxManager =
    new VoxManager(passportClient, voxClient, settingsClient, featureManager, "test.yavert-test.n4")

  lazy val auctionProtoConverter =
    new AuctionProtoConverterImpl(catalogManager = catalogManager, vosClient = vosClient, dataService = dataService)

  lazy val callAuctionManager =
    new CallAuctionManager(
      vosClient,
      searcherClient,
      searchMappings,
      auctionClient,
      auctionAutoStrategyClient,
      dataService,
      catalogManager,
      featureManager,
      auctionProtoConverter
    )

  lazy val callAuctionAutoStrategyManager = new CallAuctionAutoStrategyManager(
    auctionAutoStrategyClient,
    auctionProtoConverter
  )

  lazy val tradeInSubscriptionsManager = new TradeInSubscriptionsManager(
    statEventsManager,
    subscriptionClient,
    searchItemManager,
    savedSearchFactoryProvider
  )

  lazy val requisitesManager = new RequisitesManager(vsBillingClient)

  lazy val dealerCallsAuctionManager =
    new DefaultDealerCallsAuctionManager(
      dealerCallsAuctionClient,
      vosClient,
      dataService,
      salesmanClient
    )

  lazy val promoCampaignManager = new DefaultPromoCampaignManager(promoCampaignClient, dealerCallsAuctionClient)

  lazy val userManager =
    new UserManager(
      passportManager,
      bankerManager,
      bankerClient,
      promocoderManager,
      dealerManager,
      TestData.favoriteResellerList,
      searcherClient,
      vosClient,
      cryptoUserId = cryptoUserId,
      urlBuilder = urlBuilder
    )

  private lazy val innerRoute = new ApiHandlerBuilder {
    override def operational: OperationalSupport = TestOperationalSupport

    override def environment: Environment = TestEnvironment

    override def documentationHandler: DocumentationHandler = new DocumentationHandler

    override def deviceHandler: DeviceHandler = new DeviceHandler(helloManager, deeplinkManager)

    override def deviceSubscriptionsHandler: DeviceSubscriptionsHandler =
      new DeviceSubscriptionsHandler(deviceSubscriptionsManager)

    override def subscriptionsHandler: SubscriptionsHandler =
      new SubscriptionsHandler(subscriptionsManager)

    override def promoLandingHandler: PromoLandingHandler = new PromoLandingHandler(promoLandingManager)

    override def searchHandler =
      new SearchHandler(searcherManager, catalogManager, personalizationManager, searchHistoryManager)

    override def offerCardHandler =
      new OfferCardHandler(
        offerCardManager,
        offerStatManager,
        phoneCallbackManager,
        autoPartsManager,
        complaintsManager,
        notificationsManager,
        vinResolutionManager,
        tradeInManager,
        dealerManager,
        deliveryManager,
        productPriceManager,
        chatManager
      )

    override def userOffersHandler =
      new UserOffersHandler(offersManager, dealerAliasesManager, dealerCallsAuctionManager)

    override def draftHandler =
      new DraftHandler(draftsManager, auctionApplicationsManager, carpAuctionApplicationsManager, featureManager)

    override def draftPhotosHandler = new DraftPhotosHandler(draftPhotosManager, featureManager)

    override def auctionApplicationsHandler = new AuctionApplicationsHandler(auctionApplicationsManager)

    override def carpAuctionApplicationHandler: CarpAuctionHandler =
      new CarpAuctionHandler(carpAuctionApplicationsManager)

    override def lotusHandler: LotusHandler = new LotusHandler(lotusManager)

    override def favoriteHandler =
      new FavoriteHandler(favoriteManager, favoriteListingManager, searchesManager, savedSearchesManager)

    override def notesHandler = new NotesHandler(favoriteManager)

    override def compareHandler = new CompareHandler(compareManager)

    override def singleOfferHandler =
      new SingleOfferHandler(
        offersManager,
        offerStatManager,
        productManager,
        cmeManager,
        featureManager,
        auctionApplicationsManager
      )

    override def unificationHandler: UnificationHandler = new UnificationHandler(searcherClient)

    override def authHandler: AuthHandler = new AuthHandler(passportManager)

    override def sessionsHandler: SessionsHandler = new SessionsHandler(passportManager)

    override def tokenService: TokenService = new TokenServiceImpl(dataService)

    override def appFeatureManager: FeatureManager = featureManager

    override def passportManager: PassportManager = suite.passportManager

    override def app2AppManager: App2AppManager = suite.app2AppManager

    override def syncManager: SyncManager = suite.syncManager

    override def reviewHandler: ReviewHandler = new ReviewHandler(reviewManager, commentManager)

    override def magazineHandler: MagazineHandler = new MagazineHandler(magazineManager)

    override def phonesHandler: PhonesHandler = new PhonesHandler(suite.passportManager)

    override def app2AppHandler: App2AppHandler = new App2AppHandler(suite.app2AppManager)

    override def socialProfilesHandler: SocialProfilesHandler = new SocialProfilesHandler(suite.passportManager)

    override def catalogHandler: CatalogHandler =
      new CatalogHandler(suite.searcherManager, dictionariesManager, catalogManager, recommenderManager)

    override def statsHandler: StatsHandler = new StatsHandler(suite.statsManager)

    override def eventsHandler: EventsHandler =
      new EventsHandler(statEventsManager, vertisEventsManager, brokerEventsManager)

    override def chatHandler: ChatHandler = new ChatHandler(chatManager, featureManager)

    override def notificationHandler: NotificationHandler = new NotificationHandler(notificationManager)

    override def billingSchedulesHandler: SchedulesHandler =
      new SchedulesHandler(billingSchedulesManager, featureManager)

    override def billingSubscriptionsHandler: BillingSubscriptionsHandler =
      new BillingSubscriptionsHandler(billingSubscriptionsManager)

    override def profileHandler: UserProfileHandler = new UserProfileHandler(passportManager)

    override def confirmHandler: UserConfirmationHandler = new UserConfirmationHandler(passportManager)

    override def passwordHandler: PasswordHandler = new PasswordHandler(passportManager)

    override def emailHandler: EmailHandler = new EmailHandler(passportManager)

    override def rateLimitManager: RateLimitManager = suite.rateLimitManager

    override def autoServicesHandler: AutoServicesHandler = new AutoServicesHandler(complaintsManager)

    override def videoHandler: VideoHandler = new VideoHandler(videoManager)

    override def parsingHandler: ParsingHandler = new ParsingHandler(parsingManager)

    override def cabinetApiClient: CabinetApiClient = suite.cabinetApiClient

    override def dealerAliasesManager: AliasesManager = suite.dealerAliasesManager

    override def storyClient: StoryClient = suite.storyClient

    override def autoPartsHandler: AutoPartsHandler = new AutoPartsHandler(autoPartsManager)

    override def autoPartsAvitoHandler: AutoPartsAvitoHandler = new AutoPartsAvitoHandler(autoPartsAvitoManager)

    override def userManager = ApiSuiteBase.this.userManager

    override def userReviewsHandler: UserReviewsHandler = new UserReviewsHandler(reviewManager, savedSearchesManager)

    override def autostrategiesHandler: AutostrategiesHandler =
      new AutostrategiesHandler(autostrategiesManager, featureManager)

    override def photosHandler: PhotosHandler = new PhotosHandler(photosManager)

    override def exteriorPanoramaHandler: ExteriorPanoramaHandler = new ExteriorPanoramaHandler(panoramaManager)

    override def interiorPanoramaHandler: InteriorPanoramaHandler = new InteriorPanoramaHandler(panoramaManager)

    override def dealerHandler: DealerHandler =
      new DealerHandler(
        cabinetApiClient,
        campaignManager,
        walletManager,
        dealerManager,
        dealerPonyClient,
        dealerTariffManager,
        dealerStatsManager,
        redemptionManager,
        tradeInManager,
        countersManager,
        featureManager,
        callAuctionManager,
        multipostingClient,
        tradeInSubscriptionsManager,
        callAuctionAutoStrategyManager,
        metricsManager,
        requisitesHandler,
        dealerCallsAuctionHandler,
        promoCampaignHandler
      )

    override def cabinetHandler: CabinetHandler =
      new CabinetHandler(customerAuthManager, cabinetManager, metricsManager)

    override def aliasesHandler: AliasesHandler = new AliasesHandler(dealerAliasesManager)

    override def geoHandler: GeoHandler = new GeoHandler(geoManager)

    override def transactionHandler: TransactionHandler = new TransactionHandler(productManager)

    override def salonHandler: SalonHandler =
      new SalonHandler(phoneCallbackManager, dealerManager, searcherManager)

    override def moderationHandler: ModerationHandler = new ModerationHandler(passportManager)

    override def feedbackHandler: FeedbackHandler = new FeedbackHandler(feedbackManager)

    override def commentsHandler: CommentsHandler = new CommentsHandler(commentManager)

    override def creditsHandler: CreditsHandler = new CreditsHandler(sharkTinkoffBankStatusUpdater)

    override def sharkHandler: SharkHandler =
      new SharkHandler(sharkManager, vtbIntegrationManager, sberbankIntegrationManager)

    override def safeDealHandler: SafeDealHandler = new SafeDealHandler(safeDealManager)

    override def feedManager: FeedManager = new FeedManager(feedprocessorClient, enrichedOfferLoader)

    override def feedsHandler: FeedHandler = new FeedHandler(feedManager, featureManager)

    def featureHandler: FeatureHandler = new FeatureHandler(featureRegistry)

    override def targetingHandler: TargetingHandler = new TargetingHandler(searcherManager)

    override def searchlineManager: SearchlineManager =
      new SearchlineManager(searchlineClient, searchItemManager, searchMappings)

    override def searchlineHistoryManager: SearchlineHistoryManager = mock[SearchlineHistoryManager]

    override def searchlineHandler: SearchlineHandler =
      new SearchlineHandler(searchlineManager, searchlineHistoryManager)

    override def carfaxManager: CarfaxManager = suite.carfaxManager

    override def cartinderManager: CartinderManager = suite.cartinderManager

    override def easySearchManager: EasySearchManager = suite.easySearchManager

    override def carfaxCommentsManager: CarfaxCommentsManager = suite.carfaxCommentsManager

    override def carfaxDecayManager: CarfaxDecayManager = suite.carfaxDecayManager

    override def garageManager: GarageManager = suite.garageManager

    override def rawCarfaxReportManager: CarfaxReportManager[RawVinReportResponse] =
      suite.rawCarfaxReportManager

    override def carfaxHandler: CarfaxHandler =
      new CarfaxHandler(
        carfaxManager,
        carfaxCommentsManager,
        rawCarfaxReportManager,
        rawCarfaxOfferReportManager,
        carfaxBoughtReportsManager,
        carfaxOrdersManager
      )

    override def cartinderHandler: CartinderHandler = new CartinderHandler(cartinderManager)

    override def easySearchHandler: EasySearchHandler = new EasySearchHandler(easySearchManager)

    override def garageHandler: GarageHandler = new GarageHandler(garageManager, promoDispenserManager)

    override def userCardsManager: UserCardsManager = suite.userCardsManager

    override def vehicleManager: VehicleManager = suite.vehicleManager

    override def recallsHandler: RecallsHandler = new RecallsHandler(userCardsManager)

    override def navigatorHandler: NavigatorHandler = new NavigatorHandler(vehicleManager)

    override def billingHandler: BillingHandler = new BillingHandler(
      billingSchedulesHandler,
      billingSubscriptionsHandler,
      billingManager,
      servicesHandler
    )

    override def taxiPromoHandler: TaxiPromoHandler = new TaxiPromoHandler()

    override def storyHandler: StoryHandler = new StoryHandler(storyManager, featureManager)

    override def historyHandler: HistoryHandler = new HistoryHandler(historyManager)

    override def servicesHandler: ServicesHandler = new ServicesHandler(salesmanUserClient)

    override def comebackHandler: ComebackHandler =
      new ComebackHandler(comebackManager, featureManager)

    override def calltrackingHandler: CalltrackingHandler = new CalltrackingHandler(calltrackingManager)

    override def matchApplicationsHandler: MatchApplicationsHandler =
      new MatchApplicationsHandler(matchApplicationsManager)

    override def carSubscriptionHandler: CarSubscriptionHandler =
      new CarSubscriptionHandler(iskraManager)

    override def bookingHandler: BookingHandler =
      new BookingHandler(bookingManager)

    override def personalizationHandler: PersonalizationHandler =
      new PersonalizationHandler(personalizationManager)

    override def productsHandler: ProductsHandler =
      new ProductsHandler(salesmanClient, applicationClient)

    override def grantGroupHandler = new GrantGroupHandler()

    override def userTicketsProcessor: UserTicketsProcessor = suite.userTicketsProcessor

    override def voxHandler: VoxHandler = new VoxHandler(voxManager)

    override def promocodeHandler: PromocodeHandler = new PromocodeHandler(promocoderManager, salesmanUserClient)

    override def tradeInHandler: TradeInHandler = new TradeInHandler(tradeInNotifierClient)

    override def inciteHandler: InciteHandler = new InciteHandler(inciteManager)

    override def cmeHandler: CmeHandler =
      new CmeHandler(offerCardManager, cmeManager, dealerManager, salesmanClient, calltrackingManager, walletManager)

    override def postingScriptsIntegrationHandler: PostingScriptsIntegrationHandler =
      new PostingScriptsIntegrationHandler(cabinetApiClient, internalMultipostingClient)

    override def offersManager: OffersManager = ApiSuiteBase.this.offersManager

    override def cryptoUserId: TypedCrypto[AutoruUser] = ApiSuiteBase.this.cryptoUserId

    override def requisitesHandler: RequisitesHandler = new RequisitesHandler(requisitesManager)

    override def dealerCallsAuctionHandler: DealerCallsAuctionHandler =
      new DealerCallsAuctionHandler(dealerCallsAuctionManager, dealerAliasesManager)

    override def lentaHandler: LentaHandler = new LentaHandler(lentaManager)

    override def promoCampaignHandler: PromoCampaignHandler = new PromoCampaignHandler(promoCampaignManager)
  }.route

  lazy val route: Route = wrapRequest {
    innerRoute
  }
}

trait ApiSuite extends AnyFunSuite with ApiSuiteBase {

  implicit class RichOngoingStub[T](stub: org.mockito.stubbing.OngoingStubbing[Future[T]]) {
    def thenReturnF(result: T): OngoingStubbing[Future[T]] = stub.thenReturn(Future.successful(result))

    def thenThrowF(t: Throwable): OngoingStubbing[Future[T]] = stub.thenReturn(Future.failed(t))
  }

}

trait ApiSpec extends BaseSpec with ApiSuiteBase
