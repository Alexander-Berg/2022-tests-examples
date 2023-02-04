package ru.yandex.realty.api.routes.v1.user.draft

import akka.http.scaladsl.model.{ContentType, HttpCharsets, MediaType, StatusCodes}
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, RequestContext, Route}
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import play.api.libs.json.{JsObject, JsValue, Json}
import ru.yandex.realty.api.routes._
import ru.yandex.realty.auth.AuthInfo
import ru.yandex.realty.features.SimpleFeatures
import ru.yandex.realty.http.HandlerSpecBase
import ru.yandex.realty.managers.drafts.DraftManager
import ru.yandex.realty.model.user.{UserRef, UserRefGenerators}
import ru.yandex.realty.platform.PlatformInfo
import ru.yandex.realty.request.Request
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future
import scala.language.implicitConversions

/**
  * @author Vsevolod Levin
  */
@RunWith(classOf[JUnitRunner])
class DraftHandlerSpec extends HandlerSpecBase with PropertyChecks with UserRefGenerators {

  private val manager: DraftManager = mock[DraftManager]

  override def routeUnderTest: Route = new DraftHandler(manager)(new SimpleFeatures()).route

  private val jsonType = ContentType(MediaType.applicationWithFixedCharset("json", HttpCharsets.`UTF-8`, "json"))

  private val usersWithIdsGen: Gen[UserRef] = Gen.oneOf(webUserGen, passportUserGen)

  "POST /user/offers/draft" should {
    val request = Post("/user/offers/draft")

    "fail on anon users" in {
      forAll(anonymousUserRefGen) { user =>
        request
          .withPlatform(Some(PlatformInfo("android", "xxxdpi")))
          .withUser(user) ~>
          route ~>
          check {
            status should be(StatusCodes.Unauthorized)
          }
      }
    }

    "fail on passport users without platform" in {
      forAll(passportUserGen) { user =>
        request
          .withUser(user) ~>
          route ~>
          check {
            status should be(StatusCodes.BadRequest)
          }
      }
    }

    "succeed on passport users" in {
      forAll(passportUserGen) { user =>
        (manager
          .createDraft(_: RequestContext, _: AuthInfo, _: PlatformInfo, _: Option[JsValue], _: UserRef)(_: Traced))
          .expects(*, *, *, *, user, *)
          .returning(Future.successful(Json.obj()))

        request
          .withPlatform(Some(PlatformInfo("android", "xxxdpi")))
          .withUser(user) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
          }
      }
    }
  }

  "POST /user/me/offers/draft" should {
    val request = Post("/user/me/offers/draft")

    "succeed on defined users" in {
      forAll(usersWithIdsGen) { user =>
        (manager
          .createDraft(_: RequestContext, _: AuthInfo, _: PlatformInfo, _: Option[JsValue], _: UserRef)(_: Traced))
          .expects(*, *, *, *, user, *)
          .returning(Future.successful(Json.obj()))

        request
          .withEntity(jsonType, "{}".getBytes)
          .withPlatform(Some(PlatformInfo("android", "xxxdpi")))
          .withUser(user) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
          }
      }
    }

    "fail on users without platform" in {
      forAll(usersWithIdsGen) { user =>
        request
          .withEntity(jsonType, "{}".getBytes)
          .withUser(user) ~>
          route ~>
          check {
            status should be(StatusCodes.BadRequest)
          }
      }
    }
  }

  "PUT /user/offers/draft" should {
    val request = Put("/user/offers/draft/123456?publish=true")

    "fail on anon users" in {
      forAll(anonymousUserRefGen) { user =>
        request
          .withEntity(jsonType, "{}".getBytes)
          .withPlatform(Some(PlatformInfo("android", "xxxdpi")))
          .withUser(user) ~>
          route ~>
          check {
            status should be(StatusCodes.Unauthorized)
          }
      }
    }

    "fail on users without platform" in {
      forAll(passportUserGen) { user =>
        request
          .withEntity(jsonType, "{}".getBytes)
          .withUser(user) ~>
          route ~>
          check {
            status should be(StatusCodes.BadRequest)
          }
      }
    }

    "succeed on passport users" in {
      forAll(passportUserGen) { user =>
        (manager
          .updateDraft(_: RequestContext, _: AuthInfo, _: PlatformInfo, _: JsObject, _: String, _: Boolean, _: UserRef)(
            _: Traced,
            _: Request
          ))
          .expects(*, *, *, *, "123456", true, user, *, *)
          .returning(Future.successful(Json.obj()))

        request
          .withEntity(jsonType, "{}".getBytes)
          .withPlatform(Some(PlatformInfo("android", "xxxdpi")))
          .withUser(user) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
          }
      }
    }
  }

  "PUT /user/me/offers/draft" should {
    val request = Put("/user/me/offers/draft/123456?publish=true")

    "succeed on defined users" in {
      forAll(usersWithIdsGen) { user =>
        (manager
          .updateDraft(_: RequestContext, _: AuthInfo, _: PlatformInfo, _: JsObject, _: String, _: Boolean, _: UserRef)(
            _: Traced,
            _: Request
          ))
          .expects(*, *, *, *, "123456", true, user, *, *)
          .returning(Future.successful(Json.obj()))

        request
          .withEntity(jsonType, "{}".getBytes)
          .withPlatform(Some(PlatformInfo("android", "xxxdpi")))
          .withUser(user) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
          }
      }
    }

    "fail on passport users without platform" in {
      forAll(usersWithIdsGen) { user =>
        request
          .withEntity(jsonType, "{}".getBytes)
          .withUser(user) ~>
          route ~>
          check {
            status should be(StatusCodes.BadRequest)
          }
      }
    }
  }

  "GET /user/me/offers/draft" should {
    val request = Get("/user/me/offers/draft")

    "succeed on defined users" in {
      forAll(usersWithIdsGen) { user =>
        (manager
          .getDrafts(_: RequestContext, _: UserRef)(_: Traced))
          .expects(*, user, *)
          .returning(Future.successful(Json.obj()))

        request
          .withEntity(jsonType, "{}".getBytes)
          .withPlatform(Some(PlatformInfo("android", "xxxdpi")))
          .withUser(user) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
          }
      }
    }

    "fail on no user" in {
      forAll(noUserGen) { user =>
        request
          .withEntity(jsonType, "{}".getBytes)
          .withPlatform(Some(PlatformInfo("android", "xxxdpi")))
          .withUser(user) ~>
          route ~>
          check {
            status should be(StatusCodes.Unauthorized)
          }
      }
    }
  }

  "GET /user/me/offers/draft/123456" should {
    val request = Get("/user/me/offers/draft/123456")

    "succeed on defined users" in {
      forAll(usersWithIdsGen) { user =>
        (manager
          .getDraft(_: RequestContext, _: UserRef, _: String)(_: Traced))
          .expects(*, user, "123456", *)
          .returning(Future.successful(Json.obj()))

        request
          .withEntity(jsonType, "{}".getBytes)
          .withPlatform(Some(PlatformInfo("android", "xxxdpi")))
          .withUser(user) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
          }
      }
    }

    "fail on no user" in {
      forAll(noUserGen) { user =>
        request
          .withEntity(jsonType, "{}".getBytes)
          .withPlatform(Some(PlatformInfo("android", "xxxdpi")))
          .withUser(user) ~>
          route ~>
          check {
            status should be(StatusCodes.Unauthorized)
          }
      }
    }
  }

  override protected val exceptionHandler: ExceptionHandler = defaultExceptionHandler

  override protected val rejectionHandler: RejectionHandler = defaultRejectionHandler
}
