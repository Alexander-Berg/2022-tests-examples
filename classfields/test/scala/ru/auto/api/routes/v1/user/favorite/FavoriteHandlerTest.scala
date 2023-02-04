package ru.auto.api.routes.v1.user.favorite

import java.time.Instant
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{MediaTypes, StatusCodes}
import org.mockito.Mockito.{reset, verify, verifyNoMoreInteractions}
import org.scalatest.OptionValues
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.ApiSuite
import ru.auto.api.BreadcrumbsModel.{Entity, MarkEntity}
import ru.auto.api.broker_events.BrokerEvents.FavoriteEvent
import ru.auto.api.managers.decay.DecayOptions
import ru.auto.api.managers.enrich.EnrichOptions
import ru.auto.api.managers.events.StatEventsManager
import ru.auto.api.managers.favorite.FavoritesHelper
import ru.auto.api.managers.favorite.FavoritesHelper.FavoriteOffer
import ru.auto.api.managers.offers.OfferCardManager
import ru.auto.api.model.CategorySelector.{Cars, Trucks}
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model.favorite.OfferSearchesDomain
import ru.auto.api.model.{CategorySelector, ModelGenerators}
import ru.auto.api.services.catalog.CatalogClient
import ru.auto.api.services.{MockedClients, MockedPassport}
import ru.auto.api.util.FavoriteUtils._
import ru.auto.catalog.model.api.ApiModel.{MarkCard, RawCatalog}

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

/**
  * Author Nikita Melentev (ndmelentev@yandex-team.ru)
  * Created: 27.04.2017
  */
class FavoriteHandlerTest extends ApiSuite with MockedClients with MockedPassport with OptionValues {

  override lazy val offerCardManager: OfferCardManager = mock[OfferCardManager]
  override lazy val statEventsManager: StatEventsManager = mock[StatEventsManager]
  override lazy val favoritesHelper: FavoritesHelper = mock[FavoritesHelper]
  override lazy val catalogClient: CatalogClient = mock[CatalogClient]

  when(brokerClient.send(any[String](), any[FavoriteEvent]())(?)).thenReturn(Future.unit)
  before {
    reset(passportManager, offerCardManager, watchClient)
    when(passportManager.getClientId(?)(?)).thenReturnF(None)
    when(statEventsManager.logFavoriteOrNoteEvent(?, ?)(?)(?)).thenReturn(Future.unit)
  }

  after {
    verifyNoMoreInteractions(passportManager, offerCardManager)
  }

  private def offerToFavorite(offer: Offer): FavoriteOffer = {
    val create = Instant.now().toEpochMilli
    FavoriteOffer(offer.getId, CategorySelector.from(offer.getCategory), create, create)
  }

  private def favoriteToShortOffer(favorite: FavoriteOffer): Offer = {
    Offer
      .newBuilder()
      .setCategory(favorite.category.enum)
      .setId(favorite.id)
      .setIsFavorite(true)
      .build()
  }

  test("get listing") {
    val user = ModelGenerators.PrivateUserRefGen.next
    val offer = ModelGenerators.OfferGen.next
    val favorite = offerToFavorite(offer)

    when(favoritesHelper.getFavoriteOffer(?, ?)(?))
      .thenReturnF(Seq(favorite))

    Get(s"/1.0/user/favorites/cars/") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
        }
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("get listing with data") {
    val (user, listing) = ModelGenerators.PrivateUserListingResponseGen.next
    val favorites = listing.getOffersList.asScala.toSeq.filter(_.getIsFavorite).map(offerToFavorite)
    val pageOffers = favorites.map(favoriteToShortOffer)

    when(favoritesHelper.getFavoriteOffer(?, ?)(?)).thenReturnF(favorites)

    if (pageOffers.nonEmpty) {
      when(offerCardManager.getListingInternal(?, ?, ?)(?)).thenReturnF(listing.getOffersList.asScala.toSeq)
    }

    Get(s"/1.0/user/favorites/cars/?with_data=true") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
        }
        verify(passportManager).getClientId(eq(user))(?)
        if (pageOffers.nonEmpty) {
          verify(offerCardManager).getListingInternal(
            eq(pageOffers),
            eq(EnrichOptions.ForFavorites),
            eq(DecayOptions.ForFavorites)
          )(?)
        }
      }
  }

  test("count favorite offers") {
    val user = ModelGenerators.PrivateUserRefGen.next
    val domain = getFavDomain(Cars)

    when(favoriteClient.countFavorite(?, ?)(?))
      .thenReturnF(2)

    Get(s"/1.0/user/favorites/cars/count") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
        }
        verify(favoriteClient).countFavorite(eq(domain), eq(user))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("add favorite offer") {
    val user = ModelGenerators.PrivateUserRefGen.next
    val domain = getFavDomain(Cars)
    val offer1 = ModelGenerators.OfferGen.next

    when(favoriteClient.upsertFavorite(?, ?, ?)(?))
      .thenReturn(Future.unit)
    when(watchClient.patchWatch(?, ?, ?)(?)).thenReturn(Future.unit)

    Post(s"/1.0/user/favorites/cars/${offer1.getId}") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        val response = responseAs[String]
        withClue(response) {
          status shouldBe StatusCodes.OK
        }
        verify(favoriteClient).upsertFavorite(eq(domain), eq(user), eq(offer1.id))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("delete favorite offer") {
    val user = ModelGenerators.PrivateUserRefGen.next
    val domain = getFavDomain(Cars)
    val offer1 = ModelGenerators.OfferGen.next

    when(favoriteClient.deleteFavorite(?, ?, ?)(?))
      .thenReturn(Future.unit)
    when(watchClient.patchWatch(?, ?, ?)(?)).thenReturn(Future.unit)

    Delete(s"/1.0/user/favorites/cars/${offer1.getId}") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        val response = responseAs[String]
        withClue(response) {
          status shouldBe StatusCodes.OK
        }
        verify(favoriteClient).deleteFavorite(eq(domain), eq(user), eq(Seq(offer1.id)))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("get subscription listing") {
    val user = ModelGenerators.PrivateUserRefGen.next
    val personalSS = ModelGenerators.personalSavedSearchGen(OfferSearchesDomain, optCategory = Some(Trucks)).next
    val subscription = ModelGenerators.SubscriptionGen.next
    val mark = personalSS.optFilter.value.getMarkModelNameplate(0)
    val catalogRespone = {
      val catalog = RawCatalog.newBuilder()
      val marks = List(mark).map { mark =>
        mark -> MarkCard
          .newBuilder()
          .setEntity(
            Entity
              .newBuilder()
              .setId(mark)
              .setName(mark)
              .setMark(MarkEntity.getDefaultInstance)
          )
          .build()
      }.toMap
      catalog.putAllMark(marks.asJava)
      catalog.build()
    }

    when(favoriteClient.getUserSavedSearches(?, ?)(?))
      .thenReturnF(Seq(personalSS))
    when(subscriptionClient.getUserSubscriptions(?, ?)(?))
      .thenReturnF(Seq(subscription))
    when(settingsClient.getSettings(?, ?)(?))
      .thenReturnF(Map.empty)
    when(catalogClient.filter(?, ?)(?))
      .thenReturnF(catalogRespone)

    Get(s"/1.0/user/favorites/all/subscriptions/") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
        }
        verify(favoriteClient).getUserSavedSearches(eq(user), ?)(?)
        verify(subscriptionClient).getUserSubscriptions(eq(user), ?)(?)
        verify(passportManager).getClientId(eq(user))(?)
        verify(catalogClient).filter(?, ?)(?)
      }
  }
}
