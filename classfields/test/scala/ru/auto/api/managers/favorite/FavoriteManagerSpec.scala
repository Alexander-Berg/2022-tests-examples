package ru.auto.api.managers.favorite

import java.time.ZonedDateTime
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.time.{Millis, Span}
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.BaseSpec
import ru.auto.api.ResponseModel.ResponseStatus
import ru.auto.api.auth.Application
import ru.auto.api.broker_events.BrokerEvents.FavoriteEvent
import ru.auto.api.features.FeatureManager
import ru.auto.api.geo.Tree
import ru.auto.api.managers.decay.{DecayManager, DecayOptions}
import ru.auto.api.managers.enrich.{EnrichManager, EnrichOptions}
import ru.auto.api.managers.events.StatEventsManager
import ru.auto.api.managers.fake.FakeManager
import ru.auto.api.managers.offers.OfferCardManager
import ru.auto.api.model.CategorySelector.Cars
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model._
import ru.auto.api.model.searcher.{OfferCardAdditionalParams, SearcherRequest}
import ru.auto.api.services.favorite.FavoriteClient
import ru.auto.api.services.recommender.RecommenderClient
import ru.auto.api.services.searcher.SearcherClient
import ru.auto.api.util.search.SearchMappings
import ru.auto.api.util.search.mappers.DefaultsMapper
import ru.auto.api.util.{Request, RequestImpl}
import ru.yandex.vertis.broker.client.simple.BrokerClient
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

/**
  * Created by ndmelentev on 27.04.17.
  */
class FavoriteManagerSpec
  extends BaseSpec
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with BeforeAndAfter
  with FavoriteSpecSupport {
  val favoriteClient: FavoriteClient = mock[FavoriteClient]
  val searcherClient: SearcherClient = mock[SearcherClient]
  val recommenderClient: RecommenderClient = mock[RecommenderClient]
  val watchManager: WatchManager = mock[WatchManager]
  val tree: Tree = mock[Tree]
  val statEventsManager: StatEventsManager = mock[StatEventsManager]
  val offerCardManager: OfferCardManager = mock[OfferCardManager]
  val favoritesHelper: FavoritesHelper = mock[FavoritesHelper]
  val recommendedHelper: RecommendedHelper = mock[RecommendedHelper]
  val enrichManager: EnrichManager = mock[EnrichManager]
  val decayManager: DecayManager = mock[DecayManager]
  val fakeManager: FakeManager = mock[FakeManager]
  val brokerClient: BrokerClient = mock[BrokerClient]
  val featureManager: FeatureManager = mock[FeatureManager]

  val favoriteManager: FavoriteManager =
    new FavoriteManager(favoriteClient, favoritesHelper, watchManager, statEventsManager, brokerClient, featureManager)
  when(brokerClient.send(any[String](), any[FavoriteEvent]())(?)).thenReturn(Future.unit)

  val feature: Feature[Boolean] = mock[Feature[Boolean]]
  when(feature.value).thenReturn(false)
  val defaultsMapper = new DefaultsMapper(featureManager)
  val searchMappings: SearchMappings = new SearchMappings(defaultsMapper, featureManager)
  val searcherParams: OfferCardAdditionalParams = OfferCardAdditionalParams.empty

  val favoriteListingManager: FavoriteListingManager =
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

  before {
    when(statEventsManager.logFavoriteOrNoteEvent(?, ?)(?)(?)).thenReturn(Future.unit)
  }

  after {
    reset(favoriteClient, statEventsManager, offerCardManager)
  }

  implicit val trace: Traced = Traced.empty

  implicit val request: Request = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Some(Gen.identifier.next)))
    r.setTrace(trace)
    //    r.setUser(UserInfoGen.next)
    r.setUser(ModelGenerators.PersonalUserRefGen.next)
    r.setApplication(Application.iosApp)
    r
  }

  implicit override def patienceConfig: PatienceConfig = PatienceConfig(interval = Span(400, Millis))

  "Favorites manager" should {
    "load favorite offers of user" in {
      val offers = OfferGen.values.take(5).toList
      val offer = offers.head.updated(_.setIsFavorite(true))
      val favorites = offers.map(offerToFavorite)
      val category = offer.category

      when(favoritesHelper.getFavoriteOffer(?, ?)(?)).thenReturnF(favorites)
      when(offerCardManager.getListingInternal(?, ?, ?)(?)).thenReturnF(offers)

      val result = favoriteListingManager
        .getFavoriteListing(
          category,
          request.user.personalRef,
          withOfferData = true,
          None,
          Paging.DefaultFavorites,
          searcherParams,
          withRecommended = false,
          withNotActiveOffersCount = false,
          None
        )(request)
        .futureValue

      result.getStatus shouldBe ResponseStatus.SUCCESS
    }

    "load offers with price changed" in {
      val listing = ListingResponseGen.filter(_.getOffersCount > 0).next
      val offers = listing.getOffersList.asScala.toSeq.map(_.updated { b =>
        b.setIsFavorite(true)
        b.setCategory(Category.CARS)
      })
      val favorites = offers.map(offerToFavorite)
      val counter = OfferCountResposeGen.next
      val time = ZonedDateTime.now()

      var searcherRequest: SearcherRequest = null

      when(favoritesHelper.getFavoriteOffer(?, ?)(?)).thenReturnF(favorites)

      stub(searcherClient.offersCount(_: SearcherRequest, ?)(_: Request)) {
        case (req, _) =>
          searcherRequest = req
          Future.successful(counter)
      }

      val result = favoriteListingManager
        .getFavoriteListing(
          CategorySelector.Cars,
          request.user.personalRef,
          withOfferData = false,
          Some(time),
          Paging.DefaultFavorites,
          searcherParams,
          withRecommended = false,
          withNotActiveOffersCount = false,
          None
        )(request)
        .futureValue

      result.getStatus shouldBe ResponseStatus.SUCCESS
      result.getOffersWithNewPriceCount shouldBe counter.getCount

      assert(searcherRequest ne null, "searcherClient.offersCount not invoked")
      searchMappings.fromSearcherToApi(searcherRequest).params.getLastPriceChangeDateFrom shouldBe time.toEpochSecond
    }

    "make offer favorite to user" in {
      forAll(ModelGenerators.PersonalUserRefGen, ModelGenerators.OfferIDGen) { (user, id) =>
        val category = Cars
        when(favoriteClient.countFavorite(?, ?)(?)).thenReturn(Future.successful(2))
        when(favoriteClient.upsertFavorite(?, ?, ?)(?)).thenReturn(Future.unit)
        when(watchManager.addWatchObject(?, ?)(?)).thenReturn(Future.unit)

        val result = favoriteManager.addFavorite(category, user, id).futureValue

        result.getStatus shouldBe ResponseStatus.SUCCESS
      // ToDo дождаться выполнения ассинхроного addWatch
      }
    }

    "send delete request" in {
      forAll(ModelGenerators.PersonalUserRefGen, ModelGenerators.OfferIDGen) { (user, id) =>
        val category = Cars
        when(favoriteClient.deleteFavorite(?, ?, ?)(?)).thenReturn(Future.unit)
        when(watchManager.removeWatchObject(?, ?)(?)).thenReturn(Future.unit)

        val result = favoriteManager.deleteFavorite(category, user, Seq(id)).futureValue

        result.getStatus shouldBe ResponseStatus.SUCCESS

      // ToDo дождаться выполнения ассинхроного addWatch
      }
    }

    "count favorite offers of user" in {
      forAll(ModelGenerators.PersonalUserRefGen) { (user) =>
        val category = Cars

        when(favoriteClient.countFavorite(?, ?)(?)).thenReturnF(2)

        val result = favoriteManager.countFavorite(category, user).futureValue

        result.getStatus shouldBe ResponseStatus.SUCCESS
        result.getCount shouldBe 2
      }
    }

    "get notes of a user" in {
      forAll(OfferGen) { offer =>
        val category = Cars
        val offerWithNote = offer.updated(_.setNote("hi1"))

        when(favoritesHelper.getNotesAndFavorites(?, ?)(?)).thenReturnF(Seq(offerWithNote))
        when(favoriteClient.moveNotes(?, ?)(?)).thenReturn(Future.unit)

        val result = favoriteManager.getNotes(category, request.user.personalRef)(request).futureValue

        result.getStatus shouldBe ResponseStatus.SUCCESS
        result.getOffers(0).getId shouldBe offerWithNote.getId
        result.getOffers(0).getNote shouldBe offerWithNote.getNote
      }
    }

    "add note to user" in {
      forAll(ModelGenerators.PersonalUserRefGen, ModelGenerators.OfferIDGen) { (user, id) =>
        val category = Cars
        val note = "hi"

        when(favoriteClient.addNote(?, ?, ?, ?)(?)).thenReturn(Future.unit)
        when(watchManager.addWatchObject(?, ?)(?)).thenReturn(Future.unit)

        val result = favoriteManager.addNote(category, user, id, note).futureValue

        result.getStatus shouldBe ResponseStatus.SUCCESS
      }
    }

    "upsert note to user" in {
      forAll(ModelGenerators.PersonalUserRefGen, ModelGenerators.OfferIDGen) { (user, id) =>
        val category = Cars

        when(favoriteClient.upsertNote(?, ?, ?, ?)(?)).thenReturn(Future.unit)
        when(watchManager.addWatchObject(?, ?)(?)).thenReturn(Future.unit)

        val result = favoriteManager.upsertNote(category, user, id, "note").futureValue

        result.getStatus shouldBe ResponseStatus.SUCCESS
      }
    }

    "send delete note request" in {
      forAll(ModelGenerators.PersonalUserRefGen, ModelGenerators.OfferIDGen) { (user, id) =>
        val category = Cars

        when(favoriteClient.deleteNote(?, ?, ?)(?)).thenReturn(Future.unit)
        when(watchManager.removeWatchObject(?, ?)(?)).thenReturn(Future.unit)

        val result = favoriteManager.deleteNote(category, user, Seq(id)).futureValue

        result.getStatus shouldBe ResponseStatus.SUCCESS
      }
    }

    "count notes of user" in {
      forAll(ModelGenerators.PersonalUserRefGen) { (user) =>
        val category = Cars

        when(favoriteClient.countNotes(?, ?)(?)).thenReturnF(2)

        val result = favoriteManager.countNotes(category, user).futureValue

        result.getStatus shouldBe ResponseStatus.SUCCESS
        result.getCount shouldBe 2
      }
    }

    "consider pagination" in {
      val offers = OfferGen.values.take(10).toList.map(_.updated(_.setIsFavorite(true)))
      val favorites = offers.map(offerToFavorite)
      val paging = Paging(2, 4)
      val pagedOffers = offers.slice(4, 8).map(offerToShortOffer)

      when(favoritesHelper.getFavoriteOffer(?, ?)(?)).thenReturnF(favorites)
      when(offerCardManager.getListingInternal(pagedOffers, EnrichOptions.ForFavorites, DecayOptions.ForFavorites))
        .thenReturnF(pagedOffers)

      val result = favoriteListingManager
        .getFavoriteListing(
          CategorySelector.Cars,
          request.user.personalRef,
          withOfferData = true,
          None,
          paging,
          searcherParams,
          withRecommended = false,
          withNotActiveOffersCount = false,
          None
        )(request)
        .futureValue

      result.getStatus shouldBe ResponseStatus.SUCCESS
      result.getOffersCount shouldBe 4
      result.getPagination.getPage shouldBe 2
      result.getPagination.getTotalPageCount shouldBe 3
      result.getPagination.getTotalOffersCount shouldBe 10
      result.getPagination.getPageSize shouldBe 4
    }
  }
}
