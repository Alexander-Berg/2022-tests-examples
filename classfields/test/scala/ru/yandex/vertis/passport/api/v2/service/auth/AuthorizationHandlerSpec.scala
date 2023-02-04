package ru.yandex.vertis.passport.api.v2.service.auth

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.Uri
import org.scalacheck.Gen
import org.scalatest.WordSpec
import org.scalatest.prop.PropertyChecks
import ru.yandex.passport.model.api.ApiModel
import ru.yandex.passport.model.api.ApiModel.SimpleErrorResponse
import ru.yandex.vertis.passport.api.v2.V2Spec
import ru.yandex.vertis.passport.api.{DomainDirectives, MockedBackend, RootedSpecBase}
import ru.yandex.vertis.passport.model.SocializedUser._
import ru.yandex.vertis.passport.model.{Identity, IdentityOrToken, LoginOrRegisterParameters, LoginOrRegisterResult, SocialLoginParameters, SocialProviders, SocialUserPhone}
import ru.yandex.vertis.passport.proto.NoContextApiProtoFormats
import ru.yandex.vertis.passport.service.AccessDeniedException
import ru.yandex.vertis.passport.test.ModelGenerators
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.util.{AuthenticationException, PasswordExpiredException, PhoneIsBannedException, YandexSessionMissingException}

import scala.concurrent.Future
import ru.yandex.vertis.passport.api.NoTvmAuthorization

/**
  *
  * @author zvez
  */
class AuthorizationHandlerSpec
  extends WordSpec
  with RootedSpecBase
  with MockedBackend
  with V2Spec
  with PropertyChecks
  with NoTvmAuthorization {

  val base = "/api/2.x/auto/auth"

  import NoContextApiProtoFormats._

  "login" should {
    "response with LoginResult" in {
      forAll(ModelGenerators.loginParameters, ModelGenerators.loginResult) { (params, result) =>
        when(authService.login(eq(params))(?)).thenReturn(Future.successful(result))

        Post(s"$base/login", LoginParametersFormat.write(params)) ~>
          commonHeaders ~>
          route ~>
          check {
            status shouldBe OK
            contentType shouldBe expectedContentType
            val response = responseAs[ApiModel.LoginResult]
            response shouldBe apiCtx.LoginResultWriter.write(result)
            response.hasUser shouldBe true
          }
      }
    }

    "response with 401 Unauthorized on authentication error" in {
      forAll(ModelGenerators.loginParameters) { params =>
        when(authService.login(eq(params))(?))
          .thenReturn(Future.failed(new AuthenticationException("forbidden")))

        Post(s"$base/login", LoginParametersFormat.write(params)) ~>
          commonHeaders ~>
          route ~>
          check {
            status shouldBe Unauthorized
            contentType shouldBe expectedContentType
          }
      }
    }

    "response with 401 Unauthorized on completely wrong login" in {
      val params = ApiModel.LoginParameters.newBuilder().setLogin("somethingwrong").build()

      Post(s"$base/login", params) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe Unauthorized
          contentType shouldBe expectedContentType
        }
    }

    "response with LoginForbiddenErrorResponse" in {
      forAll(ModelGenerators.loginParameters, ModelGenerators.readableString) { (params, code) =>
        when(authService.login(eq(params))(?))
          .thenReturn(Future.failed(new PasswordExpiredException("123", Some(code))))

        Post(s"$base/login", LoginParametersFormat.write(params)) ~>
          commonHeaders ~>
          route ~>
          check {
            status shouldBe Forbidden
            contentType shouldBe expectedContentType

            val response = responseAs[ApiModel.LoginForbiddenErrorResult]
            response.getChangePasswordCode shouldBe code
          }
      }
    }
  }

  "login-social" should {
    "response with SocialLoginResult" in {
      val loginParams = ModelGenerators.socialLoginParameters.next
      val expectedResult = ModelGenerators.socialLoginResult.next

      when(authService.loginSocial(eq(loginParams))(?))
        .thenReturn(Future.successful(expectedResult))

      Post(s"$base/login-social", SocialLoginParametersFormat.write(loginParams)) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType
          val response = responseAs[ApiModel.SocialLoginResult]
          response shouldBe apiCtx.SocialLoginResultWriter.write(expectedResult)
          response.hasUser shouldBe true
          response.getCreated shouldBe expectedResult.socializedUser.isInstanceOf[Created]
          response.getLinked shouldBe expectedResult.socializedUser.isInstanceOf[Linked]
        }
    }

    "response 401-Unauthorized on authentication error" in {
      val loginParams = ModelGenerators.socialLoginParameters.next

      when(authService.loginSocial(eq(loginParams))(?))
        .thenReturn(Future.failed(new AuthenticationException("something went wrong")))

      Post(s"$base/login-social", SocialLoginParametersFormat.write(loginParams)) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe Unauthorized
          contentType shouldBe expectedContentType
          responseAs[SimpleErrorResponse].getError.getMessage should not be empty
        }
    }

    "should skip non-valid phones" in {
      val validPhone = ModelGenerators.phoneNumber.next
      val invalidPhone = "2051723"
      val socialUser = ModelGenerators.socialUserSource.next.copy(
        phones = Seq(SocialUserPhone(invalidPhone), SocialUserPhone(validPhone))
      )
      val loginParams = SocialLoginParameters(SocialProviders.Hsd, Right(socialUser))
      val result = ModelGenerators.socialLoginResult.next

      val expectedUser = socialUser.copy(phones = socialUser.phones.drop(1))
      val expectedParams = loginParams.copy(authOrUser = Right(expectedUser))

      when(authService.loginSocial(eq(expectedParams))(?)).thenReturn(Future.successful(result))

      Post(s"$base/login-social", SocialLoginParametersFormat.write(loginParams)) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType
          val response = responseAs[ApiModel.SocialLoginResult]
          response shouldBe apiCtx.SocialLoginResultWriter.write(result)
        }
    }
  }

  "login-or-register-yandex" should {

    val yandexSessionId = Gen.numStr.next
    val yandexSslSessionId = Gen.numStr.next

    "response with YandexAuthResult" in {
      val expectedResult = ModelGenerators.yandexAuthResultLinked.next

      when(authService.loginOrRegisterYandex(?, ?)(?))
        .thenReturn(Future.successful(expectedResult))

      Post(s"$base/login-or-register-yandex") ~>
        commonHeaders ~>
        yandexHeaders(yandexSessionId, yandexSslSessionId) ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType
          val response = responseAs[ApiModel.YandexAuthResult]
          response shouldBe apiCtx.YandexAuthResultWriter.write(expectedResult)
        }
    }

    "response 400-BadRequest on yandex header missing" in {

      when(authService.loginOrRegisterYandex(?, ?)(?))
        .thenReturn(Future.failed(new YandexSessionMissingException("X-Yandex-Session-ID")))

      Post(s"$base/login-or-register-yandex") ~>
        commonHeaders ~>
        yandexHeaders("", "") ~>
        route ~>
        check {
          status shouldBe BadRequest
          contentType shouldBe expectedContentType
          responseAs[SimpleErrorResponse].getError.getMessage should not be empty
        }
    }

    "response 401-Unauthorized on yandex session mismatch autoru session" in {

      when(authService.loginOrRegisterYandex(?, ?)(?))
        .thenReturn(
          Future.failed(
            new AuthenticationException(
              s" session for user differs from auto.ru session user "
            )
          )
        )

      Post(s"$base/login-or-register-yandex") ~>
        commonHeaders ~>
        yandexHeaders("", "") ~>
        route ~>
        check {
          status shouldBe Unauthorized
          contentType shouldBe expectedContentType
          responseAs[SimpleErrorResponse].getError.getMessage should not be empty
        }
    }

  }

  "impersonate" should {
    "response with forbidden when impersonation is not allowed" in {
      val sid = ModelGenerators.fakeSessionId.next
      val targetUserId = ModelGenerators.userId.next

      when(authService.impersonate(eq(sid), eq(targetUserId), ?)(?))
        .thenReturn(Future.failed(new AccessDeniedException("reasons")))

      Post(s"$base/impersonate?targetUserId=$targetUserId") ~>
        commonHeaders ~>
        addHeader(DomainDirectives.SessionIdHeader, sid.asString) ~>
        route ~>
        check {
          status shouldBe Forbidden
        }
    }

    "return new session result" in {
      val sid = ModelGenerators.fakeSessionId.next
      val targetUserId = ModelGenerators.userId.next
      val loginResult = ModelGenerators.loginResult.next
      val returnPath = loginResult.session.returnPath

      when(authService.impersonate(eq(sid), eq(targetUserId), eq(returnPath))(?))
        .thenReturn(Future.successful(loginResult))

      val query = {
        val builder = Uri.Query.newBuilder
        builder += "targetUserId" -> targetUserId
        returnPath.foreach { value =>
          builder += "returnPath" -> value
        }
        builder.result()
      }
      Post(Uri(s"$base/impersonate").withQuery(query)) ~>
        commonHeaders ~>
        addHeader(DomainDirectives.SessionIdHeader, sid.asString) ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType
          val response = responseAs[ApiModel.LoginResult]
          response shouldBe apiCtx.LoginResultWriter.write(loginResult)
        }
    }
  }

  "login-or-register" should {
    "return ok" in {
      val phone = ModelGenerators.phoneNumber.next
      val params = LoginOrRegisterParameters(IdentityOrToken.RealIdentity(Identity.Phone(phone)))

      when(authService.loginOrRegister(eq(params))(?))
        .thenReturn(Future.successful(LoginOrRegisterResult(None, "code")))

      Post(s"$base/login-or-register?phone=$phone") ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
        }
    }

    "return ok (params in body)" in {
      val params = ModelGenerators.loginOrRegisterParams.next

      when(authService.loginOrRegister(eq(params))(?))
        .thenReturn(Future.successful(LoginOrRegisterResult(None, "123456")))

      Post(s"$base/login-or-register", LoginOrRegisterParametersProtoFormat.write(params)) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          val response = responseAs[ApiModel.LoginOrRegisterResult]
          response.getCodeLength shouldBe 6
        }
    }

    "return 403 if phone is banned" in {
      val phone = ModelGenerators.phoneNumber.next
      val params = LoginOrRegisterParameters(IdentityOrToken.RealIdentity(Identity.Phone(phone)))

      when(authService.loginOrRegister(eq(params))(?))
        .thenReturn(Future.failed(new PhoneIsBannedException("+7123123123")))

      Post(s"$base/login-or-register", LoginOrRegisterParametersProtoFormat.write(params)) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe Forbidden
        }
    }
  }

}
