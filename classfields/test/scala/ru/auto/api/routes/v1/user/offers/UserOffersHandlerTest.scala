package ru.auto.api.routes.v1.user.offers

import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{MediaTypes, StatusCodes}
import com.google.protobuf.Timestamp
import org.mockito.Mockito._
import play.api.libs.json.Json
import ru.auto.api.ApiOfferModel.{Category, Offer, OfferStatus, Section}
import ru.auto.api.ApiSuite
import ru.auto.api.CounterModel.AggregatedCounter
import ru.auto.api.ResponseModel._
import ru.auto.api.TrucksModel.TruckCategory
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.enrich.enrichers.DailyCountersEnricher
import ru.auto.api.managers.enrich.enrichers.DailyCountersEnricher.DailyCountersParams
import ru.auto.api.managers.favorite.FavoritesHelper
import ru.auto.api.model.CategorySelector.All
import ru.auto.api.model.ModelGenerators.{dealerAccessGroupWithGrantGen, DealerSessionResultGen}
import ru.auto.api.model.{AutoruUser, DealerUserRoles, ModelGenerators, NoSorting, OfferID, Paging}
import ru.auto.api.services.cabinet.CheckAccessView
import ru.auto.api.services.{MockedClients, MockedOffersManager, MockedPassport}
import ru.auto.cabinet.AclResponse.{AccessGrants, AccessLevel, ResourceAlias}
import ru.yandex.vertis.feature.model.Feature

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 14.02.17
  */
class UserOffersHandlerTest extends ApiSuite with MockedClients with MockedPassport with MockedOffersManager {

  private val filters = Filters
    .newBuilder()
    .addTruckCategory(TruckCategory.TRAILER)
    .addTruckCategory(TruckCategory.ARTIC)
    .addMotoCategory("2")
    .addMotoCategory("1")
    .addStatus(OfferStatus.BANNED)
    .addStatus(OfferStatus.ACTIVE)
    .addService("4")
    .addService("3")
    .addTag("6")
    .addTag("5")
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

  private val checkAccessClientView = CheckAccessView(role = DealerUserRoles.Client)

  override lazy val favoritesHelper = mock[FavoritesHelper]
  override lazy val featureManager = mock[FeatureManager]

  when(featureManager.enrichDealerSessionWithGroup).thenReturn {
    new Feature[Boolean] {
      override def name: String = "enrich_dealer_session_with_group"
      override def value: Boolean = true
    }
  }

  private val queryParams: String = s"truck_category=trailer&truck_category=artic&" +
    s"moto_category=2&moto_category=1&" +
    s"status=banned&status=active&" +
    s"service=4&service=3&" +
    s"tag=6&tag=5&" +
    s"exclude_tag=10&exclude_tag=11&" +
    s"vin=7&vin=8&" +
    s"mark_model=9&" +
    s"price_from=500&price_to=1000&" +
    s"geobase_id=213&geobase_id=214&" +
    s"section=used&" +
    s"create_date_from=1970-01-01T00%3A16%3A40Z&create_date_to=1970-01-01T00%3A33%3A20Z&" +
    s"no_active_services=true"

  before {
    reset(passportManager)
    when(passportManager.getClientId(?)(?)).thenReturnF(None)
  }

  test("get listing") {
    val accessGroup = dealerAccessGroupWithGrantGen(ResourceAlias.OFFERS, AccessLevel.READ_ONLY).next
    val session = DealerSessionResultGen.next
    val sessionWithGrants = session.toBuilder
      .setAccess {
        AccessGrants
          .newBuilder()
          .setGroup(accessGroup.toBuilder.clearGrants())
          .addAllGrants(accessGroup.getGrantsList)
      }
      .build()

    val user = AutoruUser(sessionWithGrants.getUser.getId.toLong)
    val offers = ModelGenerators.listingResponseGen(ModelGenerators.offerGen(user)).next

    when(passportManager.getSession(?)(?)).thenReturnF(Some(sessionWithGrants))
    when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
    when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF(accessGroup)

    when(offersManager.getListing(?, ?, ?, ?, ?, ?, ?, ?)(?))
      .thenReturnF(offers)

    when(counterClient.getCounters(?)(?))
      .thenReturnF(Map.empty[OfferID, AggregatedCounter])

    when(searcherClient.breadcrumbs(any[Category](), ?)(?))
      .thenReturnF(BreadcrumbsResponse.getDefaultInstance)

    when(favoriteClient.checkIfOffersAreInFavorite(?, ?, ?)(?))
      .thenReturnF(Map.empty[OfferID, Boolean])

    when(favoritesHelper.getNotesAndFavorites(?, ?)(?))
      .thenReturnF(Seq.empty[Offer])

    when(compareClient.getCatalogCards(?, ?)(?))
      .thenReturnF(Seq.empty)

    Get(s"/1.0/user/offers/all") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader("x-session-id", sessionWithGrants.getSession.getId) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        status shouldBe StatusCodes.OK

        // TODO: проверить Filters и Sorting
        verify(offersManager).getListing(
          eq(All),
          eq(user),
          eq(Paging.Default),
          eq(Filters.getDefaultInstance),
          eq(NoSorting),
          eq(DailyCountersParams.Disabled),
          ?,
          ?
        )(?)
      }
  }

  test("get listing with filters") {
    val accessGroup = dealerAccessGroupWithGrantGen(ResourceAlias.OFFERS, AccessLevel.READ_ONLY).next
    val session = DealerSessionResultGen.next
    val sessionWithGrants = session.toBuilder
      .setAccess {
        AccessGrants
          .newBuilder()
          .setGroup(accessGroup.toBuilder.clearGrants())
          .addAllGrants(accessGroup.getGrantsList)
      }
      .build()

    val user = AutoruUser(sessionWithGrants.getUser.getId.toLong)
    val paging = ModelGenerators.PagingGen.next
    val offers = ModelGenerators.listingResponseGen(ModelGenerators.offerGen(user)).next

    when(passportManager.getSession(?)(?)).thenReturnF(Some(sessionWithGrants))
    when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
    when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF(accessGroup)

    when(offersManager.getListing(?, ?, ?, ?, ?, ?, ?, ?)(?))
      .thenReturnF(offers)

    when(counterClient.getCounters(?)(?))
      .thenReturnF(Map.empty[OfferID, AggregatedCounter])

    when(searcherClient.breadcrumbs(any[Category](), ?)(?))
      .thenReturnF(BreadcrumbsResponse.getDefaultInstance)

    when(favoriteClient.checkIfOffersAreInFavorite(?, ?, ?)(?))
      .thenReturnF(Map.empty[OfferID, Boolean])

    when(favoritesHelper.getNotesAndFavorites(?, ?)(?))
      .thenReturnF(Seq.empty[Offer])

    when(compareClient.getCatalogCards(?, ?)(?))
      .thenReturnF(Seq.empty)

    Get(s"/1.0/user/offers/all?page=${paging.page}&page_size=${paging.pageSize}&$queryParams") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader("x-session-id", sessionWithGrants.getSession.getId) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        status shouldBe StatusCodes.OK

        verify(offersManager).getListing(
          eq(All),
          eq(user),
          eq(paging),
          eq(filters),
          eq(NoSorting),
          eq(DailyCountersParams.Disabled),
          ?,
          ?
        )(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("get listing with paging") {
    val accessGroup = dealerAccessGroupWithGrantGen(ResourceAlias.OFFERS, AccessLevel.READ_ONLY).next
    val session = DealerSessionResultGen.next
    val sessionWithGrants = session.toBuilder
      .setAccess {
        AccessGrants
          .newBuilder()
          .setGroup(accessGroup.toBuilder.clearGrants())
          .addAllGrants(accessGroup.getGrantsList)
      }
      .build()

    val user = AutoruUser(sessionWithGrants.getUser.getId.toLong)
    val paging = ModelGenerators.PagingGen.next
    val offers = ModelGenerators.listingResponseGen(ModelGenerators.offerGen(user)).next

    when(passportManager.getSession(?)(?)).thenReturnF(Some(sessionWithGrants))
    when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
    when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF(accessGroup)

    when(offersManager.getListing(?, ?, ?, ?, ?, ?, ?, ?)(?))
      .thenReturnF(offers)

    when(counterClient.getCounters(?)(?))
      .thenReturnF(Map.empty[OfferID, AggregatedCounter])

    when(searcherClient.breadcrumbs(any[Category](), ?)(?))
      .thenReturnF(BreadcrumbsResponse.getDefaultInstance)

    when(favoriteClient.checkIfOffersAreInFavorite(?, ?, ?)(?))
      .thenReturnF(Map.empty[OfferID, Boolean])

    when(favoritesHelper.getNotesAndFavorites(?, ?)(?))
      .thenReturnF(Seq.empty[Offer])

    when(compareClient.getCatalogCards(?, ?)(?))
      .thenReturnF(Seq.empty)

    Get(s"/1.0/user/offers/all?page=${paging.page}&page_size=${paging.pageSize}") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader("x-session-id", sessionWithGrants.getSession.getId) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        status shouldBe StatusCodes.OK

        verify(offersManager).getListing(
          eq(All),
          eq(user),
          eq(paging),
          eq(Filters.getDefaultInstance),
          eq(NoSorting),
          eq(DailyCountersParams.Disabled),
          ?,
          ?
        )(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("get listing with daily counters") {
    val accessGroup = dealerAccessGroupWithGrantGen(ResourceAlias.OFFERS, AccessLevel.READ_ONLY).next
    val session = DealerSessionResultGen.next
    val sessionWithGrants = session.toBuilder
      .setAccess {
        AccessGrants
          .newBuilder()
          .setGroup(accessGroup.toBuilder.clearGrants())
          .addAllGrants(accessGroup.getGrantsList)
      }
      .build()

    val user = AutoruUser(sessionWithGrants.getUser.getId.toLong)
    val offers = ModelGenerators.listingResponseGen(ModelGenerators.carsOfferGen(user)).next

    when(passportManager.getSession(?)(?)).thenReturnF(Some(sessionWithGrants))
    when(cabinetApiClient.checkAccess(?, ?)(?)).thenReturnF(checkAccessClientView)
    when(cabinetApiClient.getAccessGroup(?)(?)).thenReturnF(accessGroup)

    when(offersManager.getListing(?, ?, ?, ?, ?, ?, ?, ?)(?))
      .thenReturnF(offers)

    Get(s"/1.0/user/offers/all?with_daily_counters=true") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader("x-session-id", sessionWithGrants.getSession.getId) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        status shouldBe StatusCodes.OK

        verify(offersManager).getListing(
          eq(All),
          eq(user),
          eq(Paging.Default),
          eq(Filters.getDefaultInstance),
          eq(NoSorting),
          eq(DailyCountersParams.Enabled(DailyCountersEnricher.DefaultDailyCountersPeriod)),
          ?,
          ?
        )(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("respond with 400 Bad Request on unknown category") {
    val user = ModelGenerators.PrivateUserRefGen.next

    Get(s"/1.0/user/offers/cars4") ~>
      addHeader("x-uid", user.uid.toString) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        status shouldBe StatusCodes.BadRequest
        responseAs[String] should matchJson("""{
            |  "error": "BAD_REQUEST",
            |  "status": "ERROR"
            |}""".stripMargin)

        (Json.parse(responseAs[String]) \ "detailed_error").as[String] should include(
          "Unknown category selector: [cars4]. Known values: cars, moto, trucks, all"
        )

        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("respond with 400 Bad Request on invalid `page`") {
    val user = ModelGenerators.PrivateUserRefGen.next

    Get(s"/1.0/user/offers/all?page=-1") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        status shouldBe StatusCodes.BadRequest
        responseAs[String] should matchJson("""{
            |  "error": "BAD_REQUEST",
            |  "status": "ERROR",
            |  "detailed_error": "requirement failed: page should be positive"
            |}""".stripMargin)

        verifyNoMoreInteractions(vosClient)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("respond with 400 Bad Request on invalid `page_size`") {
    val user = ModelGenerators.PrivateUserRefGen.next

    Get(s"/1.0/user/offers/all?page=1&page_size=-6") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        status shouldBe StatusCodes.BadRequest
        responseAs[String] should matchJson("""{
            |  "error": "BAD_REQUEST",
            |  "status": "ERROR",
            |  "detailed_error": "requirement failed: page_size should be positive"
            |}""".stripMargin)

        verifyNoMoreInteractions(vosClient)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("count offers") {
    val user = ModelGenerators.PrivateUserRefGen.next

    when(offersManager.countOffers(?, ?, ?)(?))
      .thenReturnF(OfferCountResponse.getDefaultInstance)

    Get(s"/1.0/user/offers/all/count") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        status shouldBe StatusCodes.OK

        verify(offersManager).countOffers(eq(All), eq(user), eq(Filters.getDefaultInstance))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("groupByBanReason") {
    val user = ModelGenerators.PrivateUserRefGen.next

    when(offersManager.groupedByBanReason(?, ?, ?)(?))
      .thenReturnF(OffersGroupedByBanReasonResponse.getDefaultInstance)

    Get(s"/1.0/user/offers/all/ban-reasons") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        status shouldBe StatusCodes.OK

        verify(offersManager).groupedByBanReason(eq(All), eq(user), ?)(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("mark-models") {
    val user = ModelGenerators.PrivateUserRefGen.next

    when(offersManager.markModels(?, ?, ?)(?))
      .thenReturnF(MarkModelsResponse.getDefaultInstance)

    Get("/1.0/user/offers/all/mark-models?" + queryParams) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        status shouldBe StatusCodes.OK

        verify(offersManager).markModels(eq(All), eq(user), eq(filters))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("truck-categories") {
    val user = ModelGenerators.PrivateUserRefGen.next

    when(offersManager.truckCategories(?, ?, ?)(?))
      .thenReturnF(TruckCategoriesResponse.getDefaultInstance)

    Get("/1.0/user/offers/all/truck-categories?" + queryParams) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        status shouldBe StatusCodes.OK

        verify(offersManager).truckCategories(eq(All), eq(user), eq(filters))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("moto-categories") {
    val user = ModelGenerators.PrivateUserRefGen.next

    when(offersManager.motoCategories(?, ?, ?)(?))
      .thenReturnF(MotoCategoriesResponse.getDefaultInstance)

    Get("/1.0/user/offers/all/moto-categories?" + queryParams) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        status shouldBe StatusCodes.OK

        verify(offersManager).motoCategories(eq(All), eq(user), eq(filters))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }
}
