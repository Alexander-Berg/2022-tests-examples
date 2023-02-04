package ru.auto.api.managers.sync

import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.{AsyncTasksSupport, BaseSpec, DummyOperationalSupport}
import ru.auto.api.auth.Application
import ru.auto.api.managers.favorite.{FavoriteManager, PersonalSavedSearch, SavedSearchesManager, WatchManager}
import ru.auto.api.managers.review.ReviewManager
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model._
import ru.auto.api.model.favorite._
import ru.auto.api.model.gen.DateTimeGenerators
import ru.auto.api.model.history.HistoryEntity
import ru.auto.api.model.subscriptions.AutoSubscriptionsDomain
import ru.auto.api.services.favorite.FavoriteClient
import ru.auto.api.services.history.HistoryClient
import ru.auto.api.services.subscriptions.SubscriptionClient
import ru.auto.api.util.{RandomUtils, Request, RequestImpl}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.subscriptions.Model.RequestSource
import ru.yandex.vertis.subscriptions.api.ApiModel
import ru.yandex.vertis.tracing.Traced
import scala.jdk.CollectionConverters._
import scala.concurrent.Future

/**
  * Created by mcsim-gr on 27.10.17.
  */
class SyncManagerSpec
  extends BaseSpec
  with MockitoSupport
  with BeforeAndAfter
  with AsyncTasksSupport
  with DummyOperationalSupport {
  val favoriteManager: FavoriteManager = mock[FavoriteManager]
  val favoriteClient: FavoriteClient = mock[FavoriteClient]
  val savedSearchesManager: SavedSearchesManager = mock[SavedSearchesManager]
  val historyClient: HistoryClient = mock[HistoryClient]
  val watchManager: WatchManager = mock[WatchManager]
  val subscriptionsClient: SubscriptionClient = mock[SubscriptionClient]
  val reviewManager: ReviewManager = mock[ReviewManager]

  val syncManager = new SyncManager(
    prometheusRegistryDummy,
    favoriteManager,
    favoriteClient,
    savedSearchesManager,
    historyClient,
    watchManager,
    subscriptionsClient,
    reviewManager,
    readRepairProbability = 1
  )

  implicit val trace: Traced = Traced.empty

  implicit val request: Request = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Some(Gen.identifier.filter(_.nonEmpty).next)))
    r.setTrace(trace)
    r.setUser(ModelGenerators.PrivateUserRefGen.next)
    r.setApplication(Application.iosApp)
    r
  }

  private val PreparedSavedSearches: Seq[PersonalSavedSearch] = Seq(
    ModelGenerators.personalSavedSearchGen(OfferSearchesDomain).next,
    ModelGenerators.personalSavedSearchGen(OfferSearchesDomain).next
  )

  private val PreparedSubscriptions: Seq[ApiModel.Subscription] = Seq(
    {
      val b = ModelGenerators.SubscriptionGen.next.toBuilder
      PreparedSavedSearches.head.qualifier.foreach(b.setQualifier)
      val requestSource = RequestSource.newBuilder().setHttpQuery(PreparedSavedSearches.head.queryString)
      b.setRequest(requestSource)
      b.setDeliveries(PreparedSavedSearches.head.deliveries.get)
      b.build()
    },
    ModelGenerators.SubscriptionGen.next
  )

  when(savedSearchesManager.move(?, ?, ?)(?)).thenReturnF(())
  when(subscriptionsClient.moveSubscriptions(?, ?, ?)(?)).thenReturnF(())
  when(favoriteClient.getUserSavedSearches(?, ?)(?)).thenReturnF(PreparedSavedSearches)
  when(subscriptionsClient.getUserSubscriptions(?, ?)(?)).thenReturnF(PreparedSubscriptions)
  when(subscriptionsClient.upsertSubscription(?, ?, ?)(?)).thenReturnF(PreparedSubscriptions.head)
  when(subscriptionsClient.deleteSubscription(?, ?, ?)(?)).thenReturnF(())

  before {
    when(favoriteManager.moveFavorite(?, ?)(?)).thenReturn(Future.unit)
    when(favoriteManager.moveNotes(?, ?)(?)).thenReturn(Future.unit)
    when(historyClient.moveHistory(?, ?)(?)).thenReturn(Future.unit)
    when(historyClient.moveSearchHistory(?, ?)(?)).thenReturn(Future.unit)
    when(watchManager.moveWatchObjects(?, ?)(?)).thenReturn(Future.unit)
    when(reviewManager.moveCurrentDraftToUser(?, ?, ?)(?)).thenReturn(Future.unit)
  }

  after {
    reset(favoriteManager, historyClient, watchManager)
  }

  "Sync manager" should {
    "sync user data" in {
      val privateOffers: Seq[Offer] = favoriteAndNotesListing()
      val anonOffers: Seq[Offer] = favoriteAndNotesListing()

      stub(favoriteManager.getAllFavoriteAndNotes(_: PersonalUserRef)(_: Request)) {
        case (user, _) =>
          user match {
            case AutoruUser(_) => Future.successful(privateOffers)
            case AnonymousUser(_) => Future.successful(anonOffers)
          }
      }

      val anonHistory = historyEntities(3, 10)
      val privateHistory = historyEntities(3, 10)
      var patch = Set.empty[OfferID]

      stub(historyClient.getHistory(_: PersonalUserRef)(_: Traced)) {
        case (user, _) =>
          user match {
            case AnonymousUser(_) => Future.successful(anonHistory)
            case AutoruUser(_) => Future.successful(privateHistory)
          }
      }

      stub(watchManager.syncWatchObjects(_: PersonalUserRef, _: Set[OfferID])(_: Request)) {
        case (_, watchPatch, _) =>
          patch = watchPatch
          Future.unit
      }

      syncManager.syncUserData(request.user.personalRef).await

      verify(favoriteManager).getAllFavoriteAndNotes(request.user.privateRef)(request)
      verify(favoriteManager).getAllFavoriteAndNotes(request.user.anonRef)(request)
      if (anonOffers.exists(_.getIsFavorite)) {
        verify(favoriteManager).moveFavorite(request.user.anonRef, request.user.privateRef)(trace)
      }
      if (anonOffers.exists(_.getNote.nonEmpty)) {
        verify(favoriteManager).moveNotes(request.user.anonRef, request.user.privateRef)(trace)
      }

      verify(historyClient).getHistory(request.user.privateRef)(trace)
      verify(historyClient).getHistory(request.user.anonRef)(trace)
      verify(historyClient).moveHistory(request.user.anonRef, request.user.privateRef)(trace)
      verify(historyClient).moveSearchHistory(request.user.anonRef, request.user.privateRef)(trace)
      verify(savedSearchesManager)
        .move(AllSearchesDomain, request.user.anonRef, request.user.privateRef)(trace)
      verify(subscriptionsClient).moveSubscriptions(
        request.user.anonRef,
        request.user.privateRef,
        AutoSubscriptionsDomain
      )(trace)

      verify(watchManager).moveWatchObjects(request.user.anonRef, request.user.privateRef)(request)
      verify(subscriptionsClient).deleteSubscription(
        request.user.personalRef,
        PreparedSubscriptions(1).getId,
        AutoSubscriptionsDomain
      )(trace)
      verify(subscriptionsClient).upsertSubscription(eq(request.user.personalRef), ?, ?)(
        eq(trace)
      )

      val result = (privateOffers.favorites ++ anonOffers.favorites).map(_.id) ++
        (privateHistory ++ anonHistory).take(100).filter(_.viewCount >= SyncManager.MinViewsToWatch).map(_.entityId)
      patch shouldEqual result.toSet
    }

    "sync user data without history move" in {
      val privateOffers: Seq[Offer] = favoriteAndNotesListing()
      val anonOffers: Seq[Offer] = favoriteAndNotesListing()

      stub(favoriteManager.getAllFavoriteAndNotes(_: PersonalUserRef)(_: Request)) {
        case (user, _) =>
          user match {
            case AutoruUser(_) => Future.successful(privateOffers)
            case AnonymousUser(_) => Future.successful(anonOffers)
          }
      }

      val anonHistory = historyEntities(1, 2)
      val privateHistory = historyEntities(3, 10)
      var patch = Set.empty[OfferID]

      stub(historyClient.getHistory(_: PersonalUserRef)(_: Traced)) {
        case (user, _) =>
          user match {
            case AnonymousUser(_) => Future.successful(anonHistory)
            case AutoruUser(_) => Future.successful(privateHistory)
          }
      }

      stub(watchManager.syncWatchObjects(_: PersonalUserRef, _: Set[OfferID])(_: Request)) {
        case (_, watchPatch, _) =>
          patch = watchPatch
          Future.unit
      }

      syncManager.syncUserData(request.user.personalRef).await

      verify(historyClient).getHistory(request.user.privateRef)(trace)
      verify(historyClient).getHistory(request.user.anonRef)(trace)
      verify(historyClient).moveSearchHistory(request.user.anonRef, request.user.privateRef)(trace)
      verifyNoMoreInteractions(historyClient)
    }
  }

  private def favoriteAndNotesListing(): Seq[Offer] = {
    ModelGenerators.ListingResponseGen.filter(_.getOffersCount > 0).next.getOffersList.asScala.toSeq.map {
      _.updated { b =>
        if (RandomUtils.nextBoolean(0.5)) {
          b.setIsFavorite(true)
        } else {
          b.setNote(Gen.identifier.next)
        }
      }
    }
  }

  private def historyEntities(minAddCount: Int, maxAddCount: Int): Seq[HistoryEntity] = {
    val entityGen = for {
      entityId <- ModelGenerators.OfferIDGen
      visitTimestamp <- DateTimeGenerators.instantInPast
      addCount <- Gen.choose(minAddCount, maxAddCount)
    } yield HistoryEntity(entityId, visitTimestamp.toEpochMilli, Some(addCount))

    Seq.fill(Gen.choose(1, 25).next)(entityGen.next)
  }
}
