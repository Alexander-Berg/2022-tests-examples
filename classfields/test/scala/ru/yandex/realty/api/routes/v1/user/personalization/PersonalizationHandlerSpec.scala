package ru.yandex.realty.api.routes.v1.user.personalization

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.JsNull
import ru.yandex.realty.adsource.{AdSource, AdSourceService}
import ru.yandex.realty.akka.http.PlayJsonSupport._
import ru.yandex.realty.api.model.response.Response
import ru.yandex.realty.api.routes._
import ru.yandex.realty.clients.recommendations.model.RecommendationParams
import ru.yandex.realty.clients.searcher.SearcherResponseModel.CountResponse
import ru.yandex.realty.clients.searcher.SearcherResponseModelFormatters._
import ru.yandex.realty.experiments.AllSupportedExperiments
import ru.yandex.realty.features.SimpleFeatures
import ru.yandex.realty.http.HandlerSpecBase
import ru.yandex.realty.managers.personalization.PersonalizationManager
import ru.yandex.realty.managers.personalization.model.RecommendedOffersResponse
import ru.yandex.realty.model.offer.{CategoryType, OfferType}
import ru.yandex.realty.model.region.NodeRgid
import ru.yandex.realty.model.user.{UserRef, UserRefGenerators}
import ru.yandex.realty.persistence.OfferId
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

/**
  * @author Vsevolod Levin
  */
@RunWith(classOf[JUnitRunner])
class PersonalizationHandlerSpec extends HandlerSpecBase {

  private val personalizationManager: PersonalizationManager = mock[PersonalizationManager]
  private val passportUser = UserRefGenerators.passportUserGen.next
  private val webUser = UserRefGenerators.webUserGen.next
  private val noUser = UserRefGenerators.noUserGen.next
  private val appUser = UserRefGenerators.appUserGen.next

  override def routeUnderTest: Route = new PersonalizationHandler(personalizationManager).route

  "GET /user/me/personalization/hiddenCount" should {
    val request = Get("/user/me/personalization/hiddenCount")

    "return count for authorized user" in {

      val resp = Response(CountResponse(15))

      (personalizationManager
        .getHiddenCount(_: String)(_: Traced))
        .expects(passportUser.uid.toString, *)
        .returning(Future.successful(resp))

      request
        .withUser(passportUser) ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          entityAs[Response[CountResponse]] should be(resp)
        }
    }

    "return count for unauthorized web user" in {

      val resp = Response(CountResponse(20))

      (personalizationManager
        .getHiddenCount(_: String)(_: Traced))
        .expects(s"yandexuid:${webUser.yandexUid}", *)
        .returning(Future.successful(resp))

      request
        .withUser(webUser) ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          entityAs[Response[CountResponse]] should be(resp)
        }
    }

    "return unauthorized for unknown users" in {
      request
        .withUser(noUser) ~>
        route ~>
        check {
          status should be(StatusCodes.Unauthorized)
        }
    }

    "return count for app user" in {
      val resp = Response(CountResponse(22))

      (personalizationManager
        .getHiddenCount(_: String)(_: Traced))
        .expects(s"device:${appUser.uuid}", *)
        .returning(Future.successful(resp))

      request
        .withUser(appUser) ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          entityAs[Response[CountResponse]] should be(resp)
        }
    }

  }

  "DELETE /user/me/personalization/hideOffers" should {
    val request = Delete("/user/me/personalization/hideOffers?offerId=987654321")
    val malformedRequest = Delete("/user/me/personalization/hideOffers")

    "return OK status for authorized user" in {

      (personalizationManager
        .hideOffers(_: String, _: Seq[OfferId])(_: Traced))
        .expects(passportUser.uid.toString, *, *)
        .returning(Future.successful(()))

      request
        .withUser(passportUser) ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
        }
    }

    "return OK status for unauthorized web user" in {

      (personalizationManager
        .hideOffers(_: String, _: Seq[OfferId])(_: Traced))
        .expects(s"yandexuid:${webUser.yandexUid}", *, *)
        .returning(Future.successful(()))

      request
        .withUser(webUser) ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
        }
    }

    "return unauthorized for unknown users" in {
      request
        .withUser(noUser) ~>
        route ~>
        check {
          status should be(StatusCodes.Unauthorized)
        }
    }

    "return OK status for app user" in {
      (personalizationManager
        .hideOffers(_: String, _: Seq[OfferId])(_: Traced))
        .expects(s"device:${appUser.uuid}", *, *)
        .returning(Future.successful(()))

      request
        .withUser(appUser) ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
        }
    }

    "return unauthorized for request without offerId" in {
      malformedRequest
        .withUser(webUser) ~>
        route ~>
        check {
          status should be(StatusCodes.BadRequest)
        }
    }

  }

  "PUT /user/me/personalization/showOffers" should {
    val request = Put("/user/me/personalization/showOffers?offerId=987654321")
    val malformedRequest = Put("/user/me/personalization/showOffers")

    "return OK status for authorized user" in {

      (personalizationManager
        .showOffers(_: String, _: Seq[OfferId])(_: Traced))
        .expects(passportUser.uid.toString, *, *)
        .returning(Future.successful(()))

      request
        .withUser(passportUser) ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
        }
    }

    "return OK status for unauthorized web user" in {

      (personalizationManager
        .showOffers(_: String, _: Seq[OfferId])(_: Traced))
        .expects(s"yandexuid:${webUser.yandexUid}", *, *)
        .returning(Future.successful(()))

      request
        .withUser(webUser) ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
        }
    }

    "return unauthorized for unknown users" in {
      request
        .withUser(noUser) ~>
        route ~>
        check {
          status should be(StatusCodes.Unauthorized)
        }
    }

    "return OK status for app user" in {
      (personalizationManager
        .showOffers(_: String, _: Seq[OfferId])(_: Traced))
        .expects(s"device:${appUser.uuid}", *, *)
        .returning(Future.successful(()))

      request
        .withUser(appUser) ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
        }
    }

    "return unauthorized for request without offerId" in {
      malformedRequest
        .withUser(webUser) ~>
        route ~>
        check {
          status should be(StatusCodes.BadRequest)
        }
    }

  }

  "PUT /user/me/personalization/moveHiddenOffers" should {
    val request = Put("/user/me/personalization/moveHiddenOffers?to=1234")
    val malformedRequest = Put("/user/me/personalization/moveHiddenOffers")

    "return OK status for authorized user" in {

      (personalizationManager
        .moveHiddenOffers(_: String, _: String)(_: Traced))
        .expects(passportUser.uid.toString, *, *)
        .returning(Future.successful(()))

      request
        .withUser(passportUser) ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
        }
    }

    "return OK status for unauthorized web user" in {

      (personalizationManager
        .moveHiddenOffers(_: String, _: String)(_: Traced))
        .expects(s"yandexuid:${webUser.yandexUid}", *, *)
        .returning(Future.successful(()))

      request
        .withUser(webUser) ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
        }
    }

    "return unauthorized for unknown users" in {
      request
        .withUser(noUser) ~>
        route ~>
        check {
          status should be(StatusCodes.Unauthorized)
        }
    }

    "return OK status for app user" in {
      (personalizationManager
        .moveHiddenOffers(_: String, _: String)(_: Traced))
        .expects(s"device:${appUser.uuid}", *, *)
        .returning(Future.successful(()))

      request
        .withUser(appUser) ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
        }
    }

    "return unauthorized for request without offerId" in {
      malformedRequest
        .withUser(webUser) ~>
        route ~>
        check {
          status should be(StatusCodes.BadRequest)
        }
    }

  }

  "GET /user/me/personalization/recommend" should {

    val rgid = NodeRgid.MOSCOW_AND_MOS_OBLAST
    val offerType = OfferType.SELL
    val category = CategoryType.APARTMENT
    val email = "devnull@yandex.ru"
    val expFlags = Set(AllSupportedExperiments.RELEVANCE_2.getExperimentName)
    val expFlagsQuery = "&expFlags"
    val request = Get(
      "/user/me/personalization/recommend?" +
        s"rgid=$rgid&offer_type=${offerType.name()}&category_type=${category.name()}&email=$email" +
        s"$expFlagsQuery=${expFlags.mkString(expFlagsQuery)}"
    )

    "return recommendations for authorized user" in {

      val resp = RecommendedOffersResponse("baseline_popular", "0_items_from_user_history_recommendations", JsNull)
      val params = RecommendationParams(Some(rgid.toString), Some(offerType), Some(category), Some(email), expFlags)

      val userRef: UserRef = passportUser
      (personalizationManager
        .getRecommendedOffers(_: UserRef, _: RecommendationParams, _: Option[AdSource])(_: Traced))
        .expects(userRef, params, *, *)
        .returning(Future.successful(resp))

      request
        .withUser(passportUser) ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          entityAs[RecommendedOffersResponse] should be(resp)
        }
    }
  }

  override protected val exceptionHandler: ExceptionHandler = defaultExceptionHandler

  override protected val rejectionHandler: RejectionHandler = defaultRejectionHandler
}
