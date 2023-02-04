package ru.yandex.realty.api.routes.v1.handlers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.adsource.AdSource
import ru.yandex.realty.proto.api.error.{Error, ErrorCode}
import ru.yandex.realty.api.ProtoResponse.SiteOfferStatResponse
import ru.yandex.realty.api.routes.{defaultExceptionHandler, defaultRejectionHandler, RichRequest, SiteOfferStatHandler}
import ru.yandex.realty.errors.CommonError
import ru.yandex.realty.http.HandlerSpecBase
import ru.yandex.realty.managers.offerstat.SiteOfferStatManager
import ru.yandex.realty.model.user.UserRefGenerators
import ru.yandex.realty.offerstat.SiteOfferStatFetchTypes
import ru.yandex.realty.offerstat.SiteOfferStatFetchTypes.SiteOfferStatFetchType
import ru.yandex.realty.proto.search.{ApartmentFilter, SiteOfferStat}
import ru.yandex.realty.request.Request
import ru.yandex.realty.search.offerstat.SiteOfferStatQuery

import scala.language.postfixOps
import scala.collection.JavaConverters._
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class SiteOfferStatHandlerSpec extends HandlerSpecBase {
  override def routeUnderTest: Route = new SiteOfferStatHandler(manager).route
  override protected val exceptionHandler: ExceptionHandler = defaultExceptionHandler
  override protected val rejectionHandler: RejectionHandler = defaultRejectionHandler

  private val manager: SiteOfferStatManager = mock[SiteOfferStatManager]

  private val offerStatFetch: SiteOfferStatFetchType = SiteOfferStatFetchTypes.FetchTypeEverything

  private val passportUser = UserRefGenerators.passportUserGen.next
  private val noUser = UserRefGenerators.noUserGen.next
  private val loginOpt = Some(passportUser.toPlainSpecial)

  val siteId200Ok = 15L
  val request200Ok = Get(s"/site/$siteId200Ok/offerStat?siteOfferStatFetch=$offerStatFetch")
  val request401Unauthorized = request200Ok
  val siteId404NotFound = 175689L
  val request404NotFound = Get(s"/site/$siteId404NotFound/offerStat?siteOfferStatFetch=$offerStatFetch")

  val psQueryTo200Ok = SiteOfferStatQuery(siteId = siteId200Ok, primarySale = None)
  val psQueryTo404NotFound = SiteOfferStatQuery(siteId = siteId404NotFound, primarySale = None)

  val result200OkBuilder: SiteOfferStatResponse.Builder = SiteOfferStatResponse
    .newBuilder()
    .setResponse(
      SiteOfferStat
        .newBuilder()
        .setAllFilters(ApartmentFilter.newBuilder().addAllHouseId(Iterable(Long.box(17L)).asJava))
    )

  val result404NotFoundBuilder: SiteOfferStatResponse.Builder =
    SiteOfferStatResponse
      .newBuilder()
      .setError(Error.newBuilder().setCode(ErrorCode.NOT_FOUND))

  val result401Unauthorized: SiteOfferStatResponse.Builder =
    SiteOfferStatResponse
      .newBuilder()
      .setError(Error.newBuilder().setCode(ErrorCode.AUTH_ERROR))

  "GET /primarySale/roomsStat" should {
    "success for siteId" in {
      (manager
        .siteOfferStat(_: SiteOfferStatQuery, _: SiteOfferStatFetchType, _: Option[String], _: Option[AdSource])(
          _: Request
        ))
        .expects(psQueryTo200Ok, offerStatFetch, loginOpt, *, *)
        .returning(Future.successful(result200OkBuilder.build()))

      request200Ok
        .withUser(passportUser) ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          entityAs[SiteOfferStatResponse] should be(result200OkBuilder.build())
        }
    }

    "produce 404 Not Found for non-existent siteId" in {
      (manager
        .siteOfferStat(_: SiteOfferStatQuery, _: SiteOfferStatFetchType, _: Option[String], _: Option[AdSource])(
          _: Request
        ))
        .expects(psQueryTo404NotFound, offerStatFetch, loginOpt, *, *)
        .returning(Future.failed[SiteOfferStatResponse] {
          val result = result404NotFoundBuilder.build()
          CommonError("Not found", result.getError.getMessage, StatusCodes.NotFound)
        })

      request404NotFound
        .withUser(passportUser) ~>
        route ~>
        check {
          status should be(StatusCodes.NotFound)
        }
    }

    "produce 401 Unauthorized for unknown users" in {
      (manager
        .siteOfferStat(_: SiteOfferStatQuery, _: SiteOfferStatFetchType, _: Option[String], _: Option[AdSource])(
          _: Request
        ))
        .expects(*, *, *, *, *)
        .returning(Future.failed[SiteOfferStatResponse] {
          val result = result401Unauthorized.build()
          CommonError("Unauthorized", result.getError.getMessage, StatusCodes.Unauthorized)
        })

      request401Unauthorized
        .withUser(noUser) ~>
        route ~>
        check {
          status should be(StatusCodes.Unauthorized)
        }
    }
  }
}
