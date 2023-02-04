package ru.auto.api.managers.cartinder

import com.google.protobuf.util.Timestamps
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.{Offer, OfferId}
import ru.auto.api.{BaseSpec, ResponseModel}
import ru.auto.api.exceptions.OfferNotFoundException
import ru.auto.api.managers.TestRequest
import ru.auto.api.managers.decay.DecayManager
import ru.auto.api.managers.favorite.FavoriteManager
import ru.auto.api.managers.offers.EnrichedOfferLoader
import ru.auto.api.model.{CategorySelector, OfferID, Paging}
import ru.auto.api.model.ModelGenerators.{offerGen, RegisteredUserRefGen}
import ru.auto.api.model.searcher.{ApiSearchRequest, SearcherRequest}
import ru.auto.api.search.SearchModel.SearchRequestParameters
import ru.auto.api.services.cartinder.CartinderClient
import ru.auto.api.util.FutureMatchers.failWith
import ru.auto.api.util.search.SearchMappings
import ru.yandex.vertis.cartinder.proto.{DislikeResponse, LikeResponse, ListingResponse, Match, MatchesResponse}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

class CartinderManagerSpec extends BaseSpec with MockitoSupport with TestRequest with ScalaCheckPropertyChecks {

  val cartinderClient: CartinderClient = mock[CartinderClient]
  val offerLoader: EnrichedOfferLoader = mock[EnrichedOfferLoader]
  val searchMappings: SearchMappings = mock[SearchMappings]
  val decayManager: DecayManager = mock[DecayManager]
  val favoriteManager: FavoriteManager = mock[FavoriteManager]

  val cartinderManager: CartinderManager =
    new CartinderManager(cartinderClient, searchMappings, decayManager, offerLoader, favoriteManager)

  "CartinderManagerSpec" should {

    "listing success" in {
      val selfOffer = offerGen(Gen.oneOf(Seq(request.user.ref))).next
      val anotherOffer = offerGen(RegisteredUserRefGen).next
      val category = CategorySelector.Cars
      val response = ListingResponse.newBuilder().addOffers(anotherOffer).build()
      val params = Map.empty[String, Set[String]]
      val apiRequest = ApiSearchRequest(category, SearchRequestParameters.getDefaultInstance)

      when(searchMappings.fromApiToSearcher(?, ?, ?)(?)).thenReturn(SearcherRequest(category, params))
      when(offerLoader.findRawOffer(?, ?, ?, ?)(?)).thenReturn(Future.successful(selfOffer))
      when(
        cartinderClient.offerListing(
          eq(request.user.ref.toPlain),
          eq(category.`enum`),
          eq(params),
          eq(OfferId.newBuilder().setCategory(category.`enum`).setId(selfOffer.getId).build())
        )(?)
      ).thenReturn(Future.successful(response))
      when(decayManager.decay(any[Seq[Offer]](), ?)(?)).thenReturn(Future.successful(Seq(anotherOffer)))

      val result = cartinderManager.listing(apiRequest, selfOffer.getId).await

      result.getOffersCount shouldBe 1
      result.getOffersList should contain theSameElementsAs Seq(anotherOffer)
    }

    "like success" in {
      val selfOffer = offerGen(Gen.oneOf(Seq(request.user.ref))).next
      val anotherOffer = offerGen(RegisteredUserRefGen).next
      val category = CategorySelector.Cars

      when(offerLoader.findRawOffer(eq(category), eq(OfferID.parse(selfOffer.getId)), ?, ?)(?))
        .thenReturn(Future.successful(selfOffer))
      when(offerLoader.findRawOffer(eq(category), eq(OfferID.parse(anotherOffer.getId)), ?, ?)(?))
        .thenReturn(Future.successful(anotherOffer))
      when(
        cartinderClient.like(
          eq(request.user.ref.toPlain),
          eq(OfferId.newBuilder().setCategory(category.`enum`).setId(selfOffer.getId).build()),
          eq(anotherOffer.getUserRef),
          eq(OfferId.newBuilder().setCategory(category.`enum`).setId(anotherOffer.getId).build())
        )(?)
      ).thenReturn(Future.successful(LikeResponse.getDefaultInstance))
      when(decayManager.decay(any[Seq[Offer]](), ?)(?)).thenReturn(Future.successful(Seq(anotherOffer)))
      when(favoriteManager.addFavorite(?, ?, ?)(?))
        .thenReturn(Future.successful(ResponseModel.SuccessResponse.getDefaultInstance))

      val result = cartinderManager.like(category, selfOffer.getId, anotherOffer.getId).await

      result shouldBe LikeResponse.getDefaultInstance
    }

    "like return failure because of selfOffer from another user" in {
      val selfOffer = offerGen(RegisteredUserRefGen).next
      val anotherOffer = offerGen(RegisteredUserRefGen).next
      val category = CategorySelector.Cars

      when(offerLoader.findRawOffer(eq(category), eq(OfferID.parse(selfOffer.getId)), ?, ?)(?))
        .thenReturn(Future.successful(selfOffer))
      when(offerLoader.findRawOffer(eq(category), eq(OfferID.parse(anotherOffer.getId)), ?, ?)(?))
        .thenReturn(Future.successful(anotherOffer))
      when(cartinderClient.like(?, ?, ?, ?)(?)).thenReturn(Future.successful(LikeResponse.getDefaultInstance))
      when(decayManager.decay(any[Seq[Offer]](), ?)(?)).thenReturn(Future.successful(Seq(anotherOffer)))

      cartinderManager.like(category, selfOffer.getId, anotherOffer.getId) should
        failWith[IllegalAccessException]
    }

    "like return failure because of offerId not found" in {
      val selfOffer = offerGen(Gen.oneOf(Seq(request.user.ref))).next
      val anotherOffer = offerGen(RegisteredUserRefGen).next
      val category = CategorySelector.Cars

      when(offerLoader.findRawOffer(eq(category), eq(OfferID.parse(selfOffer.getId)), ?, ?)(?))
        .thenReturn(Future.successful(selfOffer))
      when(offerLoader.findRawOffer(eq(category), eq(OfferID.parse(anotherOffer.getId)), ?, ?)(?))
        .thenThrow(new OfferNotFoundException())
      when(cartinderClient.like(?, ?, ?, ?)(?)).thenReturn(Future.successful(LikeResponse.getDefaultInstance))
      when(decayManager.decay(any[Seq[Offer]](), ?)(?)).thenReturn(Future.successful(Seq(anotherOffer)))

      cartinderManager.like(category, selfOffer.getId, anotherOffer.getId) should
        failWith[OfferNotFoundException]
    }

    "dislike success" in {
      val selfOffer = offerGen(Gen.oneOf(Seq(request.user.ref))).next
      val anotherOffer = offerGen(RegisteredUserRefGen).next
      val category = CategorySelector.Cars

      when(offerLoader.findRawOffer(eq(category), eq(OfferID.parse(selfOffer.getId)), ?, ?)(?))
        .thenReturn(Future.successful(selfOffer))
      when(offerLoader.findRawOffer(eq(category), eq(OfferID.parse(anotherOffer.getId)), ?, ?)(?))
        .thenReturn(Future.successful(anotherOffer))
      when(
        cartinderClient.dislike(
          eq(request.user.ref.toPlain),
          eq(OfferId.newBuilder().setCategory(category.`enum`).setId(selfOffer.getId).build()),
          eq(anotherOffer.getUserRef),
          eq(OfferId.newBuilder().setCategory(category.`enum`).setId(anotherOffer.getId).build())
        )(?)
      ).thenReturn(Future.successful(DislikeResponse.getDefaultInstance))
      when(decayManager.decay(any[Seq[Offer]](), ?)(?)).thenReturn(Future.successful(Seq(anotherOffer)))

      val result =
        cartinderManager.dislike(category, selfOffer.getId, anotherOffer.getId).await

      result shouldBe DislikeResponse.getDefaultInstance
    }

    "matches success" in {
      val category = CategorySelector.Cars
      val anotherOffer = offerGen(RegisteredUserRefGen).next
      val oneMatch = Match
        .newBuilder()
        .setOfferId(OfferId.newBuilder().setCategory(category.`enum`).setId(anotherOffer.getId))
        .setUserId(anotherOffer.getUserRef)
        .setCreated(Timestamps.parse("2023-04-08T11:29:16+03:00"))
        .build()

      when(cartinderClient.matches(eq(request.user.userRef.toPlain), ?, ?)(?))
        .thenReturn(Future.successful(MatchesResponse.newBuilder().addMatches(oneMatch).build))

      val result = cartinderManager.matches(Paging.Default, Timestamps.parse("2023-07-08T11:29:16+03:00")).await

      result.getMatchesCount shouldBe 1
      result.getMatchesList should contain theSameElementsAs Seq(oneMatch)
    }
  }
}
