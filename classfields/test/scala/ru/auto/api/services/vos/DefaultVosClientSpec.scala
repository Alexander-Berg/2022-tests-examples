package ru.auto.api.services.vos

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes._
import com.google.protobuf.Timestamp
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.{OfferStatus, Section}
import ru.auto.api.ResponseModel._
import ru.auto.api.TrucksModel.TruckCategory
import ru.auto.api.auth.Application
import ru.auto.api.exceptions.{OfferNotFoundException, RealOfferUpdateAttemptException, UnknownDraftOriginException}
import ru.auto.api.http.AcceptJsonHeader
import ru.auto.api.model.CategorySelector.Cars
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model._
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.api.util.StringUtils._
import ru.auto.api.util.{RequestImpl, Resources}

import scala.concurrent.duration._

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 18.02.17
  */
class DefaultVosClientSpec extends HttpClientSpec with MockedHttpClient with ScalaCheckPropertyChecks {

  val vosClient =
    new DefaultVosClient(http, addTimeout = 10.seconds, rotateTimeout = 10.seconds, blurTimeout = 30.seconds)

  implicit private val req = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setApplication(Application.iosApp)
    r.setRequestParams(RequestParams.empty)
    r
  }

  private val filters = Filters
    .newBuilder()
    .addTruckCategory(TruckCategory.ARTIC)
    .addTruckCategory(TruckCategory.TRAILER)
    .addMotoCategory("1")
    .addMotoCategory("2")
    .addStatus(OfferStatus.ACTIVE)
    .addStatus(OfferStatus.BANNED)
    .addService("3")
    .addService("4")
    .addTag("5")
    .addTag("6")
    .addExcludeTag("10")
    .addExcludeTag("11")
    .addVin("7")
    .addVin("8")
    .addMarkModel("9")
    .setPriceFrom(500)
    .setPriceTo(1000)
    .addGeobaseId(213)
    .addGeobaseId(214)
    .setSection(Section.USED)
    .setCreateDateFrom(Timestamp.newBuilder().setSeconds(1000))
    .setCreateDateTo(Timestamp.newBuilder().setSeconds(2000))
    .setNoActiveServices(true)
    .build()

  "VosClient" should {
    "send force_telepony_info param" in {
      forAll(OfferGen, SelectorGen) { (offer, selector) =>
        http.expectUrl(
          url"/api/v1/offer/$selector/${offer.userRef}/${offer.id}?" +
            url"include_removed=false&force_telepony_info=true&owner=false"
        )
        http.respondWith(offer)

        val res = vosClient
          .getUserOffer(
            selector,
            offer.userRef.asRegistered,
            offer.id,
            forceTeleponyInfo = true
          )
          .await

        res shouldBe offer
      }
    }

    "send force_telepony_info param 2" in {
      forAll(OfferGen, SelectorGen) { (offer, selector) =>
        http.expectUrl(
          url"/api/v1/offer/$selector/${offer.id}?force_telepony_info=1"
        )
        http.respondWith(offer)

        val res = vosClient
          .getOffer(
            selector,
            offer.id,
            forceTeleponyInfo = true
          )
          .await

        res shouldBe offer
      }
    }

    "send enable classified request" in {
      forAll(SelectorGen, OfferGen, ClassifiedNameGen) { (selector, offer, classified) =>
        {
          val offerId = offer.id
          val user = offer.userRef.asRegistered

          http.expectUrl(PUT, url"/api/v1/offer/$selector/$user/$offerId/multiposting/$classified")
          http.respondWithStatus(OK)

          vosClient
            .enableClassified(
              selector,
              user,
              offerId,
              classified
            )
            .await
        }
      }
    }

    "send disable classified request" in {
      forAll(SelectorGen, OfferGen, ClassifiedNameGen) { (selector, offer, classified) =>
        {
          val offerId = offer.id
          val user = offer.userRef.asRegistered

          http.expectUrl(DELETE, url"/api/v1/offer/$selector/$user/$offerId/multiposting/$classified")
          http.respondWithStatus(OK)

          vosClient
            .disableClassified(
              selector,
              user,
              offerId,
              classified
            )
            .await
        }
      }
    }

    "send creation request" in {
      forAll(NewOfferGen, OfferIDGen) { (offer, offerId) =>
        val category = offer.category
        val user = offer.userRef.asRegistered

        http.expectUrl(POST, url"/api/v1/offers/$category/$user")
        http.expectProto(offer)

        http.respondWithJson(OK, s"""{"status": "SUCCESS", "offerId": "$offerId"}""")

        val id = vosClient.create(category, user, offer).futureValue
        id shouldBe offerId
      }
    }

    "read getOfferIds response without filters" in {
      forAll(RegisteredUserRefGen, HashedOfferIDGen, SelectorGen) {
        case (user, response, selector) =>
          http.expectUrl(
            url"/api/v1/offers/$selector/$user/id"
          )
          http.respondWithJson(OK, s"""["${response.toPlain}"]""")

          val vosResponse =
            vosClient.getOfferIds(selector, user, Filters.getDefaultInstance, includeRemoved = false).futureValue
          vosResponse shouldBe Set(response)
      }
    }

    "read getOfferIds response with filters and include removed" in {
      forAll(RegisteredUserRefGen, HashedOfferIDGen, SelectorGen) {
        case (user, response, selector) =>
          http.expectUrl(
            url"/api/v1/offers/$selector/$user/id?include_removed=1&section=new"
          )
          http.respondWithJson(OK, s"""["${response.toPlain}"]""")

          val filter = Filters.getDefaultInstance.toBuilder.setSection(Section.NEW).build()

          val vosResponse =
            vosClient.getOfferIds(selector, user, filter, includeRemoved = true).futureValue
          vosResponse shouldBe Set(response)
      }
    }

    "read listing response" in {
      forAll(UserListingResponseGen, SelectorGen, PagingGen) {
        case ((user, response), selector, paging) =>
          http.expectUrl(
            url"/api/v1/offers/$selector/$user?page=${paging.page}&page_size=${paging.pageSize}&" +
              s"truck_category=artic&truck_category=trailer&moto_category=1&moto_category=2&status=active&" +
              s"status=banned&service=3&service=4&tag=5&tag=6&exclude_tag=10&exclude_tag=11&vin=7&vin=8&mark_model=9&" +
              s"price_from=500&price_to=1000&geobase_id=213&geobase_id=214&section=used&" +
              s"create_date_from=1970-01-01T00%3A16%3A40Z&create_date_to=1970-01-01T00%3A33%3A20Z&no_active_services=1"
          )
          http.respondWith(OK, response)

          val vosResponse = vosClient.getListing(selector, user, paging, filters, NoSorting).futureValue
          vosResponse shouldBe response
      }
    }

    "read listing response including removed" in {
      forAll(UserListingResponseGen, SelectorGen, PagingGen) {
        case ((user, response), selector, paging) =>
          http.expectUrl(
            url"/api/v1/offers/$selector/$user?page=${paging.page}&page_size=${paging.pageSize}&" +
              s"include_removed=1&truck_category=artic&truck_category=trailer&moto_category=1&moto_category=2&" +
              s"status=active&status=banned&service=3&service=4&tag=5&tag=6&exclude_tag=10&exclude_tag=11&vin=7&vin=8&" +
              s"mark_model=9&price_from=500&price_to=1000&geobase_id=213&geobase_id=214&section=used&" +
              s"create_date_from=1970-01-01T00%3A16%3A40Z&create_date_to=1970-01-01T00%3A33%3A20Z&no_active_services=1"
          )
          http.respondWith(OK, response)

          val vosResponse =
            vosClient.getListing(selector, user, paging, filters, NoSorting, includeRemoved = true).futureValue
          vosResponse shouldBe response
      }
    }

    "read listing response (static)" in {
      http.expectUrl(url"/api/v1/offers/cars/dealer%3A15977?page=1&page_size=10")
      http.respondWithProtoFrom[OfferListingResponse]("/vos/listing_response.json")

      val response =
        vosClient.getListing(Cars, UserRef.dealer(15977L), Paging.Default, Filters.getDefaultInstance, NoSorting).await
      response shouldBe Resources.toProto[OfferListingResponse]("/vos/listing_response.json")
      //todo add checks
    }

    "read listing with filter by offer_i_ref" in {
      http.expectUrl(
        url"/api/v1/offers/cars/dealer%3A15977?page=1&page_size=10&offer_i_ref=1046086254&offer_i_ref=1046086252"
      )
      http.respondWithProtoFrom[OfferListingResponse]("/vos/two_offers_listing_response.json")

      val response = vosClient
        .getListing(
          Cars,
          UserRef.dealer(15977L),
          Paging.Default,
          Filters.newBuilder().addOfferIRef(1046086254).addOfferIRef(1046086252).build(),
          NoSorting
        )
        .await
      response shouldBe Resources.toProto[OfferListingResponse]("/vos/two_offers_listing_response.json")
    }

    "read listing with filter by include_removed" in {
      http.expectUrl(url"/api/v1/offers/cars/dealer%3A15977?page=1&page_size=10&include_removed=1")
      http.respondWithProtoFrom[OfferListingResponse]("/vos/two_offers_listing_response.json")

      val response = vosClient
        .getListing(
          Cars,
          UserRef.dealer(15977L),
          Paging.Default,
          Filters.newBuilder().build(),
          NoSorting,
          includeRemoved = true
        )
        .await
      response shouldBe Resources.toProto[OfferListingResponse]("/vos/two_offers_listing_response.json")
    }

    "send single offer request (version with user in path)" in {
      forAll(OfferGen, SelectorGen) { (offer, selector) =>
        http.expectUrl(
          url"/api/v1/offer/$selector/${offer.userRef}/${offer.id}?" +
            url"include_removed=false&force_telepony_info=false&owner=false"
        )
        http.respondWith(offer)

        val res = vosClient
          .getUserOffer(
            selector,
            offer.userRef.asRegistered,
            offer.id
          )
          .await

        res shouldBe offer
      }
    }

    "send single offer request with different boolean params" in {
      forAll(OfferGen, SelectorGen, Gen.oneOf(true, false), Gen.oneOf(true, false), Gen.oneOf(true, false)) {
        (offer, selector, includeRemoved, forceTeleponyInfo, executeOnMaster) =>
          http.expectUrl(
            url"/api/v1/offer/$selector/${offer.userRef}/${offer.id}" +
              s"?include_removed=$includeRemoved&force_telepony_info=$forceTeleponyInfo&owner=$executeOnMaster"
          )
          http.respondWith(offer)

          val res = vosClient
            .getUserOffer(
              selector,
              offer.userRef.asRegistered,
              offer.id,
              includeRemoved,
              forceTeleponyInfo,
              executeOnMaster
            )
            .await

          res shouldBe offer
      }
    }

    "send get offer orig photos request" in {
      forAll(OfferGen, SelectorGen, OfferOrigPhotoGen) { (offer, selector, origPhoto) =>
        http.expectUrl(
          url"/api/v1/offer/$selector/${offer.userRef}/${offer.id}/orig-photo"
        )
        http.respondWith(origPhoto)
        val req = {
          val r = new RequestImpl
          r.setTrace(trace)
          r.setUser(offer.userRef)
          r.setApplication(Application.iosApp)
          r.setRequestParams(RequestParams.construct("1.1.1.1"))
          r
        }
        val res = vosClient
          .getOfferOrigPhotos(
            selector,
            offer.id
          )(req)
          .await

        res shouldBe origPhoto
      }
    }

    "handle not found response for get offer orig photos request" in {
      forAll(SelectorGen, RegisteredUserRefGen, OfferIDGen) { (selector, user, id) =>
        http.expectUrl(
          url"/api/v1/offer/$selector/$user/$id/orig-photo"
        )

        http.respondWithJsonFrom(NotFound, "/vos/offer_not_found.json")
        val req = {
          val r = new RequestImpl
          r.setTrace(trace)
          r.setUser(user)
          r.setApplication(Application.iosApp)
          r.setRequestParams(RequestParams.construct("1.1.1.1"))
          r
        }
        intercept[OfferNotFoundException] {
          vosClient.getOfferOrigPhotos(selector, id)(req).await
        }
      }
    }

    "handle not found response" in {
      forAll(SelectorGen, RegisteredUserRefGen, OfferIDGen) { (selector, user, id) =>
        http.expectUrl(
          url"/api/v1/offer/$selector/$user/$id?include_removed=false" +
            url"&force_telepony_info=false&owner=false"
        )

        http.respondWithJsonFrom(NotFound, "/vos/offer_not_found.json")

        intercept[OfferNotFoundException] {
          vosClient.getUserOffer(selector, user, id).await
        }
      }
    }

    "handle unexpected response" in {
      val user = UserRef.user(123L)
      val offerId = OfferID.parse("123-bb")

      http.expectUrl(url"/api/v1/offer/cars/$user/$offerId")
      http.respondWith(InternalServerError, "Unexpected exception")

      intercept[RuntimeException] {
        vosClient.getUserOffer(Cars, user, offerId, includeRemoved = false).await
      }
    }

    "send countOffers request" in {
      forAll(RegisteredUserRefGen, SelectorGen) {
        case (user, selector) =>
          val response = OfferCountResponse.newBuilder().setCount(12).setStatus(ResponseStatus.SUCCESS).build()
          http.expectUrl(url"/api/v1/offers/$selector/$user/count")
          http.respondWith(OK, response)

          // TODO: генерировать фильтры и сортировку
          val vosResponse = vosClient.countOffers(selector, user, Filters.getDefaultInstance).futureValue
          vosResponse shouldBe response
      }
    }

    "send groupByReasonOfBan request" in {
      val Reason = "HelloWorldReason"
      forAll(RegisteredUserRefGen, SelectorGen) {
        case (user, selector) =>
          val response = OffersGroupedByBanReasonResponse
            .newBuilder()
            .addEntries(
              OffersGroupedByBanReasonResponse.Entry
                .newBuilder()
                .setCount(1)
                .setBanReason(Reason)
            )
            .setStatus(ResponseStatus.SUCCESS)
            .build()
          http.expectUrl(url"/api/v1/offers/$selector/$user/grouped-by-ban-reason?include_removed=false")
          http.respondWith(OK, response)
          val vosResponse = vosClient
            .groupedByBanReason(selector, user, Filters.newBuilder().build())
            .futureValue
          vosResponse shouldBe response
      }
    }

    "send markModels request" in {
      forAll(RegisteredUserRefGen, SelectorGen) {
        case (user, selector) =>
          http.expectUrl(
            url"/api/v1/offers/$selector/$user/index-info?index=mark_models&" +
              s"truck_category=artic&truck_category=trailer&moto_category=1&moto_category=2&status=active&" +
              s"status=banned&service=3&service=4&tag=5&tag=6&exclude_tag=10&exclude_tag=11&vin=7&vin=8&mark_model=9&" +
              s"price_from=500&price_to=1000&geobase_id=213&geobase_id=214&section=used&" +
              s"create_date_from=1970-01-01T00%3A16%3A40Z&create_date_to=1970-01-01T00%3A33%3A20Z&no_active_services=1"
          )
          http.respondWith(OK, MarkModelsResponse.getDefaultInstance)

          // TODO: генерировать фильтры и сортировку
          val vosResponse = vosClient.markModels(selector, user, filters).futureValue
          vosResponse shouldBe MarkModelsResponse.getDefaultInstance
      }
    }

    "send truckCategories request" in {
      forAll(RegisteredUserRefGen, SelectorGen) {
        case (user, selector) =>
          http.expectUrl(
            url"/api/v1/offers/$selector/$user/index-info?index=truck_categories&" +
              s"truck_category=artic&truck_category=trailer&moto_category=1&moto_category=2&status=active&" +
              s"status=banned&service=3&service=4&tag=5&tag=6&exclude_tag=10&exclude_tag=11&vin=7&vin=8&mark_model=9&" +
              s"price_from=500&price_to=1000&geobase_id=213&geobase_id=214&section=used&" +
              s"create_date_from=1970-01-01T00%3A16%3A40Z&create_date_to=1970-01-01T00%3A33%3A20Z&no_active_services=1"
          )
          http.respondWith(OK, TruckCategoriesResponse.getDefaultInstance)

          val vosResponse = vosClient.truckCategories(selector, user, filters).futureValue
          vosResponse shouldBe TruckCategoriesResponse.getDefaultInstance
      }
    }

    "send motoCategories request" in {
      forAll(RegisteredUserRefGen, SelectorGen) {
        case (user, selector) =>
          http.expectUrl(
            url"/api/v1/offers/$selector/$user/index-info?index=moto_categories&" +
              s"truck_category=artic&truck_category=trailer&moto_category=1&moto_category=2&status=active&" +
              s"status=banned&service=3&service=4&tag=5&tag=6&exclude_tag=10&exclude_tag=11&vin=7&vin=8&mark_model=9&" +
              s"price_from=500&price_to=1000&geobase_id=213&geobase_id=214&section=used&" +
              s"create_date_from=1970-01-01T00%3A16%3A40Z&create_date_to=1970-01-01T00%3A33%3A20Z&no_active_services=1"
          )
          http.respondWith(OK, MotoCategoriesResponse.getDefaultInstance)

          val vosResponse = vosClient.motoCategories(selector, user, filters).futureValue
          vosResponse shouldBe MotoCategoriesResponse.getDefaultInstance
      }
    }

    "get hashed id" in {
      val category = Cars
      val userId = 10591660
      val userRef = AutoruUser(userId)
      val offerIRef = 1043045004
      val hash = "977b3"

      http.expectUrl(GET, url"/api/v1/offer/cars/$userRef/i-ref/$offerIRef/hashed")
      http.expectHeader(AcceptJsonHeader)
      http.respondWithJson(OK, s"""{"id":"$offerIRef-$hash"}""")

      val response = vosClient.getHashedId(category, userRef, offerIRef).futureValue
      response shouldBe OfferID(offerIRef, Some(hash))
    }

    "check belong" in {
      forAll(RegisteredUserRefGen, Gen.nonEmptyListOf(OfferIDGen), BooleanGen) { (user, offerIds, result) =>
        http.expectUrl(GET, s"/api/v1/offers/$user/check-belong?" + offerIds.map("offer_id=" + _).mkString("&"))
        http.respondWith(OfferBelongResponse.newBuilder().setBelong(result).build())
        vosClient.checkBelong(user, offerIds).futureValue shouldBe result
      }
    }

    "insert_new=0 in draft publish for dealers" in {
      val category = Cars
      val user = PrivateUserRefGen.next
      val dealer = DealerUserRefGen.next
      val draftId = OfferIDGen.next

      val req = {
        val r = new RequestImpl
        r.setTrace(trace)
        r.setUser(user)
        r.setDealer(dealer)
        r.setApplication(Application.iosApp)
        r.setRequestParams(RequestParams.construct("1.1.1.1"))
        r
      }

      http.expectUrl(
        POST,
        url"/api/v1/draft/cars/$dealer/$draftId/publish?supports_panorama_editing=0&can_disable_chats=0&" +
          url"insert_new=0&can_create_redirect=0&no_license_plate_support=0&reseller=0"
      )
      http.expectHeader(AcceptJsonHeader)
      http.respondWithJson(
        Forbidden,
        s"""{"status":"ERROR","errors":[{"code":"UNKNOWN_DRAFT_ORIGIN",""" +
          """"description":"Cannot determine which offer must be updated from this draft"}]}"""
      )

      intercept[UnknownDraftOriginException] {
        vosClient
          .publishDraft(category, dealer, draftId, AdditionalDraftParams(None, None, None))(req)
          .await
      }
    }

    "send remote id and remote url as params in draft publish api" in {
      val category = Cars
      val user = RegisteredUserRefGen.next
      val draftId = OfferIDGen.next

      val remoteId = "avito|cars|1767143438"
      val remoteUrl = "https://www.avito.ru/miass/avtomobili/toyota_land_cruiser_prado_2013_1767143438"
      http.expectUrl(
        POST,
        url"/api/v1/draft/cars/$user/$draftId/publish?remote_id=$remoteId&remote_url=$remoteUrl&" +
          url"supports_panorama_editing=0&can_disable_chats=0&insert_new=1&can_create_redirect=0&" +
          url"no_license_plate_support=0&reseller=0"
      )
      http.expectHeader(AcceptJsonHeader)
      http.respondWithJson(
        Forbidden,
        s"""{"status":"ERROR","errors":[{"code":"UNKNOWN_DRAFT_ORIGIN",""" +
          """"description":"Cannot determine which offer must be updated from this draft"}]}"""
      )

      val req = {
        val r = new RequestImpl
        r.setTrace(trace)
        r.setUser(user)
        r.setApplication(Application.iosApp)
        r.setRequestParams(RequestParams.construct("1.1.1.1"))
        r
      }

      intercept[UnknownDraftOriginException] {
        vosClient
          .publishDraft(category, user, draftId, AdditionalDraftParams(None, Some(remoteId), Some(remoteUrl)))(req)
          .await
      }
    }

    "handle Forbidden responses from draft publish api" in {
      val category = Cars
      val user = RegisteredUserRefGen.next
      val draftId = OfferIDGen.next

      http.expectUrl(
        POST,
        url"/api/v1/draft/cars/$user/$draftId/publish?supports_panorama_editing=0&can_disable_chats=0&" +
          s"insert_new=1&can_create_redirect=0&no_license_plate_support=0&reseller=0"
      )
      http.expectHeader(AcceptJsonHeader)
      http.respondWithJson(
        Forbidden,
        s"""{"status":"ERROR","errors":[{"code":"UNKNOWN_DRAFT_ORIGIN",""" +
          """"description":"Cannot determine which offer must be updated from this draft"}]}"""
      )

      val req = {
        val r = new RequestImpl
        r.setTrace(trace)
        r.setUser(user)
        r.setApplication(Application.iosApp)
        r.setRequestParams(RequestParams.construct("1.1.1.1"))
        r
      }

      intercept[UnknownDraftOriginException] {
        vosClient.publishDraft(category, user, draftId, AdditionalDraftParams(None, None, None))(req).await
      }

      http.respondWithJson(
        Forbidden,
        s"""{"status":"ERROR","errors":[{"code":"REAL_OFFER_UPDATE_ATTEMPT",""" +
          """"description":"Tried to update a real offer, not a draft!"}]}"""
      )

      intercept[RealOfferUpdateAttemptException] {
        vosClient.publishDraft(category, user, draftId, AdditionalDraftParams(None, None, None))(req).await
      }
    }

    "update delivery info" in {
      forAll(DeliveryInfoGen, OfferIDGen, StrictCategoryGen, DealerUserRefGen) {
        (deliveryInfo, offerId, category, dealer) =>
          http.expectUrl(PUT, url"/api/v1/offer/$category/$dealer/$offerId/delivery")
          http.expectProto(deliveryInfo)
          http.respondWithStatus(OK)
          vosClient.updateDelivery(category, dealer, offerId, deliveryInfo)
      }
    }

    "activate multi posting" in {
      forAll(StrictCategoryGen, RegisteredUserRefGen, OfferIDGen) { (category, user, offerId) =>
        http.expectUrl(PUT, url"/api/v1/offer/$category/$user/$offerId/multiposting/status/active")
        http.respondWithStatus(OK)
        vosClient.activateMultiPosting(category, user, offerId)
      }
    }

    "hide multi posting" in {
      forAll(StrictCategoryGen, RegisteredUserRefGen, OfferIDGen) { (category, user, offerId) =>
        http.expectUrl(PUT, url"/api/v1/offer/$category/$user/$offerId/multiposting/status/inactive")
        http.respondWithStatus(OK)
        vosClient.hideMultiPosting(category, user, offerId)
      }
    }

    "archive multi posting" in {
      forAll(StrictCategoryGen, RegisteredUserRefGen, OfferIDGen) { (category, user, offerId) =>
        http.expectUrl(PUT, url"/api/v1/offer/$category/$user/$offerId/multiposting/status/removed")
        http.respondWithStatus(OK)
        vosClient.archiveMultiPosting(category, user, offerId)
      }
    }
  }
}
