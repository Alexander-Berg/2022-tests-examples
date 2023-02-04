package ru.yandex.realty.api.routes.v2.handlers.userfeeds

import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.directives.BasicDirectives
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.api.ProtoResponse._
import ru.yandex.realty.api.routes._
import ru.yandex.realty.api.routes.v2.userfeeds.UserFeedHandlerImpl
import ru.yandex.realty.api.{ApiExceptionHandler, ApiRejectionHandler}
import ru.yandex.realty.clients.capa.Partner
import ru.yandex.realty.clients.capa.gen.PartnerGen
import ru.yandex.realty.http.HandlerSpecBase
import ru.yandex.realty.managers.feed.PartnersProtoFormats
import ru.yandex.realty.managers.userfeeds.UserFeedManager
import ru.yandex.realty.model.exception.ForbiddenException
import ru.yandex.realty.model.user.{PassportUser, UserRefGenerators}
import ru.yandex.realty.request.Request
import ru.yandex.vertis.generators.BasicGenerators
import ru.yandex.vertis.scalamock.util.RichFutureCallHandler

import scala.languageFeature.postfixOps

@RunWith(classOf[JUnitRunner])
class UserFeedHandlerSpec
  extends HandlerSpecBase
  with PropertyChecks
  with PartnersProtoFormats
  with PartnerGen
  with BasicGenerators
  with UserRefGenerators
  with BasicDirectives
  with ScalatestRouteTest {

  override protected val exceptionHandler: ExceptionHandler = ApiExceptionHandler.handler
  override protected val rejectionHandler: RejectionHandler = ApiRejectionHandler.handler

  implicit val excHandler: ExceptionHandler = exceptionHandler
  implicit val rejHandler: RejectionHandler = rejectionHandler

  override def routeUnderTest: Route = Route.seal(new UserFeedHandlerImpl(manager).route)

  private val manager: UserFeedManager = mock[UserFeedManager]
  private val forbidden = new ForbiddenException(None.orNull)
  private val notFound = new NoSuchElementException()

  private val mockRecheckFeed =
    toMockFunction3(manager.recheckFeed(_: Long, _: Long)(_: Request))

  "UserFeedHandlerSpec" when {
    "recheckFeed" should {
      "propagate missing/inapt authorization as AUTH_ERROR" in inSequence {
        forAll(passportUserGen, posNum[Long]) { (userRef, partnerId) =>
          mockRecheckFeed
            .expects(userRef.uid, partnerId, *)
            .anyNumberOfTimes()
            .throwingF(forbidden)

          Put(
            Uri(s"/user/${userRef.toPlain}/feed/$partnerId/recheck")
          ).acceptingProto
            .withUser(userRef) ~>
            route ~>
            check {
              status should be(StatusCodes.Forbidden)
            }
        }
      }

      "propagate missing partnerId/userId mapping as 404 Not Found" in inSequence {
        forAll(passportUserGen, partnerGen) { (userRef, partner) =>
          mockRecheckFeed
            .expects(userRef.uid, partner.partnerId, *)
            .anyNumberOfTimes()
            .throwingF(notFound)

          Put(
            Uri(s"/user/${userRef.toPlain}/feed/${partner.partnerId}/recheck")
          ).acceptingProto
            .withUser(userRef) ~>
            route ~>
            check {
              status should be(StatusCodes.NotFound)
            }
        }
      }

      "conclude with a valid response HTTP 200 OK for a valid partner" in inSequence {
        forAll(passportUserGen, posNum[Long]) { (userRef, partnerId) =>
          mockRecheckFeed
            .expects(userRef.uid, partnerId, *)
            .anyNumberOfTimes()
            .returningF(PartnerFeedsResponse.getDefaultInstance)

          Put(
            Uri(s"/user/${userRef.toPlain}/feed/$partnerId/recheck")
          ).acceptingProto
            .withUser(userRef) ~>
            route ~>
            check {
              status should be(StatusCodes.OK)
            }
        }
      }
    }

    "conclude with BadRequest for a valid partner in inoperable state" in inSequence {
      forAll(passportUserGen, partnerGen) { (userRef: PassportUser, partner: Partner) =>
        mockRecheckFeed
          .expects(userRef.uid, partner.partnerId, *)
          .anyNumberOfTimes()
          .throwingF(new IllegalArgumentException())

        Put(
          Uri(s"/user/${userRef.toPlain}/feed/${partner.partnerId}/recheck")
        ).acceptingProto
          .withUser(userRef) ~>
          route ~>
          check {
            status should be(StatusCodes.BadRequest)
          }
      }
    }
  }
}
