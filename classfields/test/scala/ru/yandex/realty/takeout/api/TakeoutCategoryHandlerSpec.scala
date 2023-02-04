package ru.yandex.realty.takeout.api

import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.model.headers.CustomHeader
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import org.junit.runner.RunWith
import org.scalatest.Assertion
import org.scalatestplus.junit.JUnitRunner
import realty.palma.mortgage_demand.MortgageDemand
import ru.yandex.realty.api.ProtoResponse.ApiUserNotesCountResponse
import ru.yandex.realty.api.handlers.SimpleApiRejectionHandler
import ru.yandex.realty.application.ng.palma.client.{
  PalmaClient,
  PalmaFilter,
  PalmaListing,
  PalmaPagination,
  PalmaSorting
}
import ru.yandex.realty.clients.personal.favorites.FavoritesClient
import ru.yandex.realty.clients.searcher.SearcherClient
import ru.yandex.realty.clients.searcher.SearcherResponseModel.CountResponse
import ru.yandex.realty.clients.subscription.SubscriptionsV3Client
import ru.yandex.realty.clients.vos.ng.{OffersFilter, VosClientNG}
import ru.yandex.realty.http.{HandlerSpecBase, XYaServiceTicketHeaderName, XYaUserTicketHeaderName}
import ru.yandex.realty.model.exception.UnauthorizedException
import ru.yandex.realty.model.user._
import ru.yandex.realty.model.util.Slice
import ru.yandex.realty.proto.api.v2.users.personalization.UserNotesCountResponseData
import ru.yandex.realty.proto.offer.vos.OfferResponse.{VosOfferListResponse, VosOfferResponse}
import ru.yandex.realty.takeout.TakeoutCategory.TakeoutCategoryState
import ru.yandex.realty.takeout.TakeoutResponseStatusTypeMessage.TakeoutResponseStatusType
import ru.yandex.realty.takeout.{
  TakeoutCategoriesDeleteRequest,
  TakeoutCategoriesDeleteResponse,
  TakeoutCategoriesResponse,
  TakeoutCategory
}
import ru.yandex.realty.takeout.api.v1.TakeoutCategoryHandler
import ru.yandex.realty.takeout.dao.TakeoutCategoryTaskDao
import ru.yandex.realty.takeout.managers.TakeoutCategoryManager
import ru.yandex.realty.takeout.model.TakeoutCategoryTask
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.tvm.TvmLibraryApi
import ru.yandex.vertis.protobuf.ProtobufUtils
import ru.yandex.vertis.subscriptions.api.ApiModel.Subscription

import scala.concurrent.Future
import scala.collection.JavaConverters._

case class TvmServiceTicketHeader(override val value: String) extends CustomHeader {
  def name: String = XYaServiceTicketHeaderName

  def renderInRequests: Boolean = false

  def renderInResponses: Boolean = false
}

case class TvmUserTicketHeader(override val value: String) extends CustomHeader {
  def name: String = XYaUserTicketHeaderName

  def renderInRequests: Boolean = false

  def renderInResponses: Boolean = false
}

@RunWith(classOf[JUnitRunner])
class TakeoutCategoryHandlerSpec extends HandlerSpecBase {
  val takeoutCategoryDeleteTaskDao = mock[TakeoutCategoryTaskDao]
  val favoriteOffersClient = mock[FavoritesClient[String]]
  val offersComparisonClient = mock[FavoritesClient[String]]
  val searcherClient = mock[SearcherClient]
  val palmaMortgageDemandClient = mock[PalmaClient[MortgageDemand]]
  val vosClientNG = mock[VosClientNG]
  val subscriptionClient = mock[SubscriptionsV3Client]

  val manager = new TakeoutCategoryManager(
    takeoutCategoryDeleteTaskDao,
    favoriteOffersClient,
    offersComparisonClient,
    searcherClient,
    palmaMortgageDemandClient,
    vosClientNG,
    subscriptionClient
  )
  val tvmLibraryApi = mock[TvmLibraryApi]
  val allowedServiceTvmId = 12412
  val allowedServiceTicket = "breberberberacfaw"
  val notAllowedServiceTvmId = 512512
  val notAllowedServiceTicket = "verbverbacawcaw"

  val uid = 234245
  val allowedUserTicket = "veawcvafawfawfa"
  val notAllowedUserTicket = "verwverbveswcvszcva"

  val serviceTicket2TvmId =
    Map(
      allowedServiceTicket -> allowedServiceTvmId,
      notAllowedServiceTicket -> notAllowedServiceTvmId
    )

  val request_id = 12412412

  val jsonType = ContentTypes.`application/json`

  override def routeUnderTest: Route =
    new TakeoutCategoryHandler(manager, tvmLibraryApi, Set(allowedServiceTvmId)).route

  override protected def exceptionHandler: ExceptionHandler = TakeoutCategoryExceptionHandler.handler

  override protected def rejectionHandler: RejectionHandler = SimpleApiRejectionHandler.handler

  "TakeoutCategoryHandler" when {
    "status" should {
      "return error if request has not tvm service ticket" in {
        mockTvm()

        Get(s"/takeout/status?request_id=$request_id") ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
            val response = entityAs[TakeoutCategoriesResponse]
            response.getErrorsCount > 0 should be(true)
            response.getStatus should be(TakeoutResponseStatusType.error)
          }
      }

      "return error if request tvm service ticket belongs to not allowed service" in {
        mockTvm()
        val serviceTicketHeader = TvmServiceTicketHeader(notAllowedServiceTicket)

        Get(s"/takeout/status?request_id=$request_id").withHeaders(serviceTicketHeader) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
            val response = entityAs[TakeoutCategoriesResponse]
            response.getErrorsCount > 0 should be(true)
            response.getStatus should be(TakeoutResponseStatusType.error)
          }
      }

      "return error if request has not tvm user ticket" in {
        mockTvm()
        val serviceTicketHeader = TvmServiceTicketHeader(allowedServiceTicket)

        Get(s"/takeout/status?request_id=$request_id").withHeaders(serviceTicketHeader) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
            val response = entityAs[TakeoutCategoriesResponse]
            response.getErrorsCount > 0 should be(true)
            response.getStatus should be(TakeoutResponseStatusType.error)
          }
      }

      "return error if request has not allowed user ticket" in {
        mockTvm()
        val serviceTicketHeader = TvmServiceTicketHeader(allowedServiceTicket)
        val userTicketHeader = TvmUserTicketHeader(notAllowedUserTicket)

        Get(s"/takeout/status?request_id=$request_id")
          .withHeaders(serviceTicketHeader, userTicketHeader) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
            val response = entityAs[TakeoutCategoriesResponse]
            response.getErrorsCount > 0 should be(true)
            response.getStatus should be(TakeoutResponseStatusType.error)
          }
      }

      "return status code OK if request has allowed tvm service and user tickets" in {
        mockTvm()
        mockDao(false, false)
        mockClients(true)
        val serviceTicketHeader = TvmServiceTicketHeader(allowedServiceTicket)
        val userTicketHeader = TvmUserTicketHeader(allowedUserTicket)

        Get(s"/takeout/status?request_id=$request_id")
          .withHeaders(serviceTicketHeader, userTicketHeader) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
            val response = entityAs[TakeoutCategoriesResponse]
            response.getStatus should be(TakeoutResponseStatusType.ok)
          }
      }

      "return empty categories if user hasn't got personal data" in {
        mockTvm()
        mockDao(false, false)
        mockClients(true)
        val serviceTicketHeader = TvmServiceTicketHeader(allowedServiceTicket)
        val userTicketHeader = TvmUserTicketHeader(allowedUserTicket)

        Get(s"/takeout/status?request_id=$request_id")
          .withHeaders(serviceTicketHeader, userTicketHeader) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
            val response = entityAs[TakeoutCategoriesResponse]
            val categories = response.getCategoriesList.asScala
            checkCategories(categories, TakeoutCategoryState.empty)
            response.getStatus should be(TakeoutResponseStatusType.ok)
          }
      }

      "return empty categories if user hasn't got personal data and there is finished task" in {
        mockTvm()
        mockDao(false, true)
        mockClients(true)
        val serviceTicketHeader = TvmServiceTicketHeader(allowedServiceTicket)
        val userTicketHeader = TvmUserTicketHeader(allowedUserTicket)

        Get(s"/takeout/status?request_id=$request_id")
          .withHeaders(serviceTicketHeader, userTicketHeader) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
            val response = entityAs[TakeoutCategoriesResponse]
            val categories = response.getCategoriesList.asScala
            checkCategories(categories, TakeoutCategoryState.empty)
            response.getStatus should be(TakeoutResponseStatusType.ok)
          }
      }

      "return delete in progress category status if there is queued task" in {
        mockTvm()
        mockDao(true, false)
        mockClients(false)
        val serviceTicketHeader = TvmServiceTicketHeader(allowedServiceTicket)
        val userTicketHeader = TvmUserTicketHeader(allowedUserTicket)

        Get(s"/takeout/status?request_id=$request_id")
          .withHeaders(serviceTicketHeader, userTicketHeader) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
            val response = entityAs[TakeoutCategoriesResponse]
            val categories = response.getCategoriesList.asScala
            checkCategories(categories, TakeoutCategoryState.delete_in_progress)
            response.getStatus should be(TakeoutResponseStatusType.ok)
          }
      }

      "return ready to delete category status if user has got personal data" in {
        mockTvm()
        mockDao(false, false)
        mockClients(false)
        val serviceTicketHeader = TvmServiceTicketHeader(allowedServiceTicket)
        val userTicketHeader = TvmUserTicketHeader(allowedUserTicket)

        Get(s"/takeout/status?request_id=$request_id")
          .withHeaders(serviceTicketHeader, userTicketHeader) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
            val response = entityAs[TakeoutCategoriesResponse]
            val categories = response.getCategoriesList.asScala
            checkCategories(categories, TakeoutCategoryState.ready_to_delete)
            response.getStatus should be(TakeoutResponseStatusType.ok)
          }
      }
    }

    "delete" should {
      "return error if request has not tvm service ticket" in {
        mockTvm()

        Post(s"/takeout/delete?request_id=$request_id") ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
            val response = entityAs[TakeoutCategoriesDeleteResponse]
            response.getErrorsCount > 0 should be(true)
            response.getStatus should be(TakeoutResponseStatusType.error)
          }
      }

      "return error if request tvm service ticket belongs to not allowed service" in {
        mockTvm()
        val serviceTicketHeader = TvmServiceTicketHeader(notAllowedServiceTicket)

        Post(s"/takeout/delete?request_id=$request_id").withHeaders(serviceTicketHeader) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
            val response = entityAs[TakeoutCategoriesDeleteResponse]
            response.getErrorsCount > 0 should be(true)
            response.getStatus should be(TakeoutResponseStatusType.error)
          }
      }

      "return error if request has not tvm user ticket" in {
        mockTvm()
        val serviceTicketHeader = TvmServiceTicketHeader(allowedServiceTicket)

        Post(s"/takeout/delete?request_id=$request_id").withHeaders(serviceTicketHeader) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
            val response = entityAs[TakeoutCategoriesDeleteResponse]
            response.getErrorsCount > 0 should be(true)
            response.getStatus should be(TakeoutResponseStatusType.error)
          }
      }

      "return error if request has not allowed user ticket" in {
        mockTvm()
        val serviceTicketHeader = TvmServiceTicketHeader(allowedServiceTicket)
        val userTicketHeader = TvmUserTicketHeader(notAllowedUserTicket)

        Post(s"/takeout/delete?request_id=$request_id")
          .withHeaders(serviceTicketHeader, userTicketHeader) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
            val response = entityAs[TakeoutCategoriesDeleteResponse]
            response.getErrorsCount > 0 should be(true)
            response.getStatus should be(TakeoutResponseStatusType.error)
          }
      }

      "return error if request has got not supported category id" in {
        mockTvm()
        val serviceTicketHeader = TvmServiceTicketHeader(allowedServiceTicket)
        val userTicketHeader = TvmUserTicketHeader(allowedUserTicket)
        val request = TakeoutCategoriesDeleteRequest.newBuilder().addId("10").build()

        Post(s"/takeout/delete?request_id=$request_id")
          .withHeaders(serviceTicketHeader, userTicketHeader)
          .withEntity(jsonType, ProtobufUtils.toJson(request, compact = false).getBytes) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
            val response = entityAs[TakeoutCategoriesDeleteResponse]
            response.getErrorsCount > 0 should be(true)
            response.getStatus should be(TakeoutResponseStatusType.error)
          }
      }

      "return OK if request has got supported category id" in {
        mockTvm()
        val serviceTicketHeader = TvmServiceTicketHeader(allowedServiceTicket)
        val userTicketHeader = TvmUserTicketHeader(allowedUserTicket)
        val request = TakeoutCategoriesDeleteRequest.newBuilder().addId("1").build()
        (takeoutCategoryDeleteTaskDao.addTask _)
          .expects(*)
          .once()
          .returning(Future.unit)

        Post(s"/takeout/delete?request_id=$request_id")
          .withHeaders(serviceTicketHeader, userTicketHeader)
          .withEntity(jsonType, ProtobufUtils.toJson(request, compact = false).getBytes) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
            val response = entityAs[TakeoutCategoriesDeleteResponse]
            response.getStatus should be(TakeoutResponseStatusType.ok)
          }
      }
    }
  }

  //scalastyle:off method.length
  private def mockTvm(): Unit = {
    (tvmLibraryApi
      .checkServiceTicket(_: String, _: Set[Int]))
      .expects(allowedServiceTicket, Set(allowedServiceTvmId))
      .anyNumberOfTimes()
      .returning(())

    (tvmLibraryApi
      .checkServiceTicket(_: String, _: Set[Int]))
      .expects(notAllowedServiceTicket, Set(allowedServiceTvmId))
      .anyNumberOfTimes()
      .throwing(new UnauthorizedException(""))

    (tvmLibraryApi.getUidFromUserTicket _)
      .expects(allowedUserTicket)
      .anyNumberOfTimes()
      .returning(uid)

    (tvmLibraryApi.getUidFromUserTicket _)
      .expects(notAllowedUserTicket)
      .anyNumberOfTimes()
      .throwing(new UnauthorizedException(""))
  }

  private def mockDao(isTaskQueued: Boolean, isTaskFinished: Boolean): Unit = {
    toMockFunction2(takeoutCategoryDeleteTaskDao.isTaskQueued(_: Long, _: Int))
      .expects(uid, *)
      .anyNumberOfTimes()
      .returning(Future.successful(isTaskQueued))

    for {
      categoryId <- Seq(1, 2, 3, 4, 5, 6, 7)
      lastFinishedTask = if (isTaskFinished) Some(TakeoutCategoryTask(uid = uid, categoryId = categoryId, shardKey = 1))
      else None
    } yield toMockFunction2(takeoutCategoryDeleteTaskDao.findLastFinishedTask(_: Long, _: Int))
      .expects(uid, categoryId)
      .anyNumberOfTimes()
      .returning(Future.successful(lastFinishedTask))
  }

  private def mockClients(empty: Boolean): Unit = {
    val (favorites, notesCount, hiddenCount, offers, subscriptions, mortgageDemands) =
      if (empty) {
        (
          Seq.empty,
          ApiUserNotesCountResponse.getDefaultInstance,
          CountResponse(0),
          VosOfferListResponse.getDefaultInstance,
          Seq.empty,
          PalmaListing[MortgageDemand](Seq.empty, "")
        )
      } else {
        (
          Seq("2412412412"),
          ApiUserNotesCountResponse
            .newBuilder()
            .setResponse(UserNotesCountResponseData.newBuilder().setTotal(3))
            .build(),
          CountResponse(5),
          VosOfferListResponse.newBuilder().addOffers(VosOfferResponse.getDefaultInstance).build(),
          Seq(Subscription.getDefaultInstance),
          PalmaListing[MortgageDemand](Seq(MortgageDemand.defaultInstance), "")
        )
      }

    toMockFunction2(favoriteOffersClient.getFavorites(_: UserRef)(_: Traced))
      .expects(PassportUser(uid), *)
      .anyNumberOfTimes()
      .returning(Future.successful(favorites))

    toMockFunction2(offersComparisonClient.getFavorites(_: UserRef)(_: Traced))
      .expects(PassportUser(uid), *)
      .anyNumberOfTimes()
      .returning(Future.successful(favorites))

    toMockFunction2(searcherClient.notesCount(_: String)(_: Traced))
      .expects(uid.toString, *)
      .anyNumberOfTimes()
      .returning(Future.successful(notesCount))

    toMockFunction2(searcherClient.getHiddenCount(_: String)(_: Traced))
      .expects(uid.toString, *)
      .anyNumberOfTimes()
      .returning(Future.successful(hiddenCount))

    toMockFunction4(vosClientNG.getOffers(_: String, _: OffersFilter, _: Slice)(_: Traced))
      .expects(uid.toString, *, *, *)
      .anyNumberOfTimes()
      .returning(Future.successful(offers))

    toMockFunction2(subscriptionClient.getSubscriptions(_: String)(_: Traced))
      .expects(PassportUser(uid).toPlain, *)
      .anyNumberOfTimes()
      .returning(Future.successful(subscriptions))

    toMockFunction4(
      palmaMortgageDemandClient.list(_: Seq[PalmaFilter], _: Option[PalmaPagination], _: Option[PalmaSorting])(
        _: Traced
      )
    ).expects(*, *, *, *)
      .anyNumberOfTimes()
      .returning(Future.successful(mortgageDemands))
  }

  //scalastyle:off cyclomatic.complexity
  private def checkCategories(categories: Seq[TakeoutCategory], state: TakeoutCategoryState): Assertion = {
    categories.size should be(7)

    categories.exists(
      category => category.getId == "1" && category.getSlug == "favorites" && category.getState == state
    ) should be(true)

    categories.exists(category => category.getId == "2" && category.getSlug == "notes" && category.getState == state) should be(
      true
    )
    categories.exists(
      category => category.getId == "3" && category.getSlug == "mortgage_demands" && category.getState == state
    ) should be(true)

    categories.exists(category => category.getId == "4" && category.getSlug == "offers" && category.getState == state) should be(
      true
    )
    categories.exists(
      category => category.getId == "5" && category.getSlug == "hidden_clusters" && category.getState == state
    ) should be(true)

    categories.exists(
      category => category.getId == "6" && category.getSlug == "subscriptions" && category.getState == state
    ) should be(true)

    categories.exists(
      category => category.getId == "7" && category.getSlug == "offers_comparison" && category.getState == state
    ) should be(true)
  }
}
