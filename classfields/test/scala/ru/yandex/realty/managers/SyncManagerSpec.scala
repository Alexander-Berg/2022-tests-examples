package ru.yandex.realty.managers

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.personal.favorites.FavoritesClient
import ru.yandex.realty.clients.searcher.SearcherClient
import ru.yandex.realty.clients.subscription.SubscriptionsV3Client
import ru.yandex.realty.clients.watch.WatchClient
import ru.yandex.realty.errors.CommonError
import ru.yandex.realty.features.FeatureStub
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.model.user.UserRef
import ru.yandex.realty.saved.search.model.PersonalSavedSearch
import ru.yandex.vertis.subscriptions.api.ApiModel.{Deliveries, Watch}
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

/**
  * Specs on [[SyncManager]] behaviour.
  *
  * @author nstaroverova
  */
@RunWith(classOf[JUnitRunner])
class SyncManagerSpec extends AsyncSpecBase with RequestAware {

  private val favoritesClient = mock[FavoritesClient[String]]
  private val watchClient = mock[WatchClient]
  private val subscriptionClient = mock[SubscriptionsV3Client]
  private val savedSearchClient = mock[FavoritesClient[PersonalSavedSearch]]
  private val searcherClient = mock[SearcherClient]
  private val syncManager =
    new SyncManager(
      new FeatureStub(true),
      new FeatureStub(true),
      new FeatureStub(true),
      favoritesClient,
      watchClient,
      savedSearchClient,
      subscriptionClient,
      searcherClient,
      readRepairChance = 0
    )

  private val theFailure = Future.failed(CommonError("ERROR_CODE", "Error message"))

  "SyncManager" should {
    "fail syncFavorites() in case of PersonalApi error" in {
      val src = UserRef.app("device")
      val dst = UserRef.passport(1)

      (favoritesClient
        .moveFavorites(_: UserRef, _: UserRef)(_: Traced))
        .expects(src, dst, *)
        .returning(theFailure)

      (watchClient
        .syncWatch(_: UserRef, _: Option[UserRef], _: Set[String], _: Deliveries)(_: Traced))
        .expects(dst, Some(src), *, *, *)
        .never()

      interceptCause[CommonError] {
        withRequestContext(src) { implicit r =>
          syncManager
            .syncFavorites(dst)
            .futureValue
        }
      }
    }

    "perform syncFavorites() in case of PersonalApi success" in {
      val src = UserRef.app("device")
      val dst = UserRef.passport(1)
      val ids = Set("1", "2", "3")

      (favoritesClient
        .moveFavorites(_: UserRef, _: UserRef)(_: Traced))
        .expects(src, dst, *)
        .returning(Future.successful(ids.toSeq))

      (watchClient
        .syncWatch(_: UserRef, _: Option[UserRef], _: Set[String], _: Deliveries)(_: Traced))
        .expects(dst, Some(src), ids, *, *)
        .returning(Future.successful(Watch.newBuilder().build()))

      withRequestContext(src) { implicit r =>
        syncManager
          .syncFavorites(dst)
          .futureValue should be(())
      }
    }

    "perform syncAllUserData() in case of clients success" in {
      val src = UserRef.app("device")
      val dst = UserRef.passport(1)
      val ids = Set("1", "2", "3")

      (favoritesClient
        .moveFavorites(_: UserRef, _: UserRef)(_: Traced))
        .expects(src, dst, *)
        .returning(Future.successful(ids.toSeq))

      (watchClient
        .syncWatch(_: UserRef, _: Option[UserRef], _: Set[String], _: Deliveries)(_: Traced))
        .expects(dst, Some(src), ids, *, *)
        .returning(Future.successful(Watch.newBuilder().build()))

      (subscriptionClient
        .moveSubscriptions(_: UserRef, _: UserRef)(_: Traced))
        .expects(src, dst, *)
        .returning(Future.successful(Unit))

      (savedSearchClient
        .moveFavorites(_: UserRef, _: UserRef)(_: Traced))
        .expects(src, dst, *)
        .returning(Future.successful(Seq.empty))

      (searcherClient
        .moveNotes(_: String, _: String)(_: Traced))
        .expects(src.toPlain, dst.asPrivate.uid.toString, *)
        .returning(Future.successful(Unit))

      (searcherClient
        .moveDeletedClusters(_: String, _: String)(_: Traced))
        .expects(src.toPlain, dst.asPrivate.uid.toString, *)
        .returning(Future.successful(Unit))

      withRequestContext(src) { implicit r =>
        syncManager
          .syncAllUserData(dst)
          .futureValue should be(())
      }
    }

    "perform syncAllUserData() in case of clients failure" in {
      val src = UserRef.app("device")
      val dst = UserRef.passport(1)
      val ids = Set("1", "2", "3")

      (favoritesClient
        .moveFavorites(_: UserRef, _: UserRef)(_: Traced))
        .expects(src, dst, *)
        .returning(theFailure)

      (watchClient
        .syncWatch(_: UserRef, _: Option[UserRef], _: Set[String], _: Deliveries)(_: Traced))
        .expects(dst, Some(src), ids, *, *)
        .never()

      (subscriptionClient
        .moveSubscriptions(_: UserRef, _: UserRef)(_: Traced))
        .expects(src, dst, *)
        .returning(theFailure)

      (savedSearchClient
        .moveFavorites(_: UserRef, _: UserRef)(_: Traced))
        .expects(src, dst, *)
        .returning(theFailure)

      (searcherClient
        .moveNotes(_: String, _: String)(_: Traced))
        .expects(src.toPlain, dst.asPrivate.uid.toString, *)
        .returning(theFailure)

      (searcherClient
        .moveDeletedClusters(_: String, _: String)(_: Traced))
        .expects(src.toPlain, dst.asPrivate.uid.toString, *)
        .returning(theFailure)

      withRequestContext(src) { implicit r =>
        syncManager
          .syncAllUserData(dst)
          .futureValue should be(())
      }
    }

  }

}
