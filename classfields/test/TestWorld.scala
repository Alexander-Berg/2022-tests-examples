package ru.yandex.vertis.general.gateway.app.test

import common.clients.telepony.TeleponyClient
import common.zio.grpc.client.GrpcClient
import common.zio.tvm.InvalidTicket
import general.bonsai.public_api.PublicBonsaiServiceGrpc
import general.classifiers.add_form_classifier_api.AddFormClassifierServiceGrpc
import general.classifiers.query_classifier_api.QueryClassifierServiceGrpc
import general.darkroom.api.DarkroomServiceGrpc
import general.event.api.EventLoggerServiceGrpc
import general.favorites.favorites_api.FavoritesGrpc
import general.favorites.saved_notes_api.SavedNotesGrpc
import general.favorites.saved_offers_api.SavedOffersGrpc
import general.favorites.saved_search_api.SavedSearchesGrpc
import general.favorites.saved_sellers_api.SavedSellersGrpc
import general.feed.api.FeedServiceGrpc
import general.globe.api.GeoServiceGrpc
import general.gost.counters_api.CountersServiceGrpc
import general.gost.offer_api.OfferServiceGrpc
import general.personal.recommendation_api.PersonalRecommendationServiceGrpc
import general.personal.search_history_api.PersonalSearchHistoryServiceGrpc
import general.rubick.add_form_api.AddFormServiceGrpc
import general.rubick.edit_form_api.EditFormServiceGrpc
import general.search.api.SearchServiceGrpc
import general.search.recommendation_api.RecommendationServiceGrpc
import general.snatcher.profiles_api.ProfilesServiceGrpc
import general.users.api.UserServiceGrpc
import general.vasabi.api.VasesServiceGrpc
import general.wisp.chat_api.ChatServiceGrpc
import ru.yandex.vertis.general.clients.router.RouterClient
import ru.yandex.vertis.general.clients.suggest.SuggestClient
import ru.yandex.vertis.general.common.context.ContextPayload
import ru.yandex.vertis.general.common.ab.UaasExperiments
import ru.yandex.vertis.general.gateway.clients.complaints.ComplaintsClient
import common.clients.statist._
import general.aglomerat.api.AglomeratServiceGrpc
import general.personal.bigb_api.PersonalBigBServiceGrpc
import general.search.universal_search_api.UniversalSearchServiceGrpc
import ru.yandex.vertis.general.gateway.context.RequestContext
import ru.yandex.vertis.general.gateway.datasources.world.World
import ru.yandex.vertis.spamalot.service.NotificationServiceGrpc
import common.zio.logging.Logging
import vertis.scoring.api.UserScoringServiceGrpc
import zio._
import zio.clock.Clock

import scala.concurrent.duration.FiniteDuration

class TestWorld extends World.Service {
  override def clock: Clock.Service = ???
  override def logging: Logging.Service = ???
  override def warmup: UIO[Unit] = UIO.unit

  override def aglomerat: GrpcClient.Service[AglomeratServiceGrpc.AglomeratService] = ???
  override def bonsai: GrpcClient.Service[PublicBonsaiServiceGrpc.PublicBonsaiService] = ???
  override def globe: GrpcClient.Service[GeoServiceGrpc.GeoService] = ???
  override def user: GrpcClient.Service[UserServiceGrpc.UserService] = ???
  override def feed: GrpcClient.Service[FeedServiceGrpc.FeedService] = ???
  override def rubickAddForm: GrpcClient.Service[AddFormServiceGrpc.AddFormService] = ???
  override def rubickEditForm: GrpcClient.Service[EditFormServiceGrpc.EditFormService] = ???
  override def gost: GrpcClient.Service[OfferServiceGrpc.OfferService] = ???
  override def gostCounters: GrpcClient.Service[CountersServiceGrpc.CountersService] = ???
  override def search: GrpcClient.Service[SearchServiceGrpc.SearchService] = ???
  override def recommendations: GrpcClient.Service[RecommendationServiceGrpc.RecommendationService] = ???
  override def suggest: SuggestClient.Service = ???
  override def statist: StatistClient.Service = ???
  override def wisp: GrpcClient.Service[ChatServiceGrpc.ChatService] = ???
  override def savedOffers: GrpcClient.Service[SavedOffersGrpc.SavedOffers] = ???
  override def savedSellers: GrpcClient.Service[SavedSellersGrpc.SavedSellers] = ???
  override def savedSearches: GrpcClient.Service[SavedSearchesGrpc.SavedSearches] = ???
  override def savedNotes: GrpcClient.Service[SavedNotesGrpc.SavedNotes] = ???
  override def favorites: GrpcClient.Service[FavoritesGrpc.Favorites] = ???
  override def eventLogger: GrpcClient.Service[EventLoggerServiceGrpc.EventLoggerService] = ???
  override def router: RouterClient.Service = ???
  override def notifications: GrpcClient.Service[NotificationServiceGrpc.NotificationService] = ???
  override def avatarsHost: String = ???
  override def withContext(context: ContextPayload): IO[InvalidTicket, World.Service] = ???
  override def complaints: ComplaintsClient.Service = ???
  override def darkroom: GrpcClient.Service[DarkroomServiceGrpc.DarkroomService] = ???
  override def personal: GrpcClient.Service[PersonalRecommendationServiceGrpc.PersonalRecommendationService] = ???
  override def personalHistory: GrpcClient.Service[PersonalSearchHistoryServiceGrpc.PersonalSearchHistoryService] = ???
  override def personalBigBInfo: GrpcClient.Service[PersonalBigBServiceGrpc.PersonalBigBService] = ???
  override def scoring: GrpcClient.Service[UserScoringServiceGrpc.UserScoringService] = ???
  override def addFormClassifier: GrpcClient.Service[AddFormClassifierServiceGrpc.AddFormClassifierService] = ???
  override def telepony: TeleponyClient.Service = ???
  override def queryClassifier: GrpcClient.Service[QueryClassifierServiceGrpc.QueryClassifierService] = ???
  override def snatcher: GrpcClient.Service[ProfilesServiceGrpc.ProfilesService] = ???

  override def requestContext: RequestContext = ???
  override def withDeadlineAfter(duration: FiniteDuration): World.Service = ???

  override def vasabi: GrpcClient.Service[VasesServiceGrpc.VasesService] = ???

  override def withExperiments(experiments: UaasExperiments): World.Service = ???

  override def universalSearch: GrpcClient.Service[UniversalSearchServiceGrpc.UniversalSearchService] = ???

  override def updateLoggingEntries(update: Map[String, String]): UIO[Unit] = ???
}
