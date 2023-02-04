package ru.yandex.vertis.passport.api.v1.service

import akka.http.scaladsl.model.FormData
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.headers.Authorization
import org.scalatest.WordSpec
import ru.yandex.vertis.passport.api.v1.SessionTestUtils
import ru.yandex.vertis.passport.api.{DomainDirectives, MockedBackend, RootedSpecBase}
import ru.yandex.vertis.passport.service.AccessDeniedException
import ru.yandex.vertis.passport.test.ModelGenerators
import ru.yandex.vertis.passport.model.IdentityOrToken
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.view.SessionResultView

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
  with SessionTestUtils
  with NoTvmAuthorization {

  val commonHeaders = addHeader(Accept(MediaTypes.`application/json`))
  val base = "/api/1.x/service/auto/auth"

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

      when(authService.impersonate(eq(sid), eq(targetUserId), ?)(?))
        .thenReturn(Future.successful(loginResult))

      Post(s"$base/impersonate?targetUserId=$targetUserId") ~>
        commonHeaders ~>
        addHeader(DomainDirectives.SessionIdHeader, sid.asString) ~>
        route ~>
        check {
          status shouldBe OK
          val response = responseAs[SessionResultView]
          checkSessionResult(loginResult.asSessionResult, response)
        }
    }
  }

  "login" should {
    "work with login header" in {
      val loginParameters = ModelGenerators.loginParametersF(ModelGenerators.realIdentity).next
      val loginResult = ModelGenerators.loginResult.next

      val creds = loginParameters.credentials
      val header = Authorization(BasicHttpCredentials(creds.identity.identity.login, creds.password))

      when(authService.login(eq(loginParameters))(?))
        .thenReturn(Future.successful(loginResult))

      Post(s"$base/login") ~>
        addHeader(header) ~>
        route ~>
        check {
          status shouldBe OK
          val response = responseAs[SessionResultView]
          checkSessionResult(loginResult.asSessionResult, response)
        }
    }

    "work with login form field" in {
      val loginParameters = ModelGenerators.loginParametersF(ModelGenerators.realIdentity).next
      val loginResult = ModelGenerators.loginResult.next

      val creds = loginParameters.credentials
      val formData = FormData("login" -> creds.identity.identity.login, "password" -> creds.password)

      when(authService.login(eq(loginParameters))(?))
        .thenReturn(Future.successful(loginResult))

      Post(s"$base/login", formData) ~>
        route ~>
        check {
          status shouldBe OK
          val response = responseAs[SessionResultView]
          checkSessionResult(loginResult.asSessionResult, response)
        }
    }

    "work with identity token form field" in {
      val loginParameters = ModelGenerators.loginParametersF(ModelGenerators.tokenIdentity).next
      val loginResult = ModelGenerators.loginResult.next

      val creds = loginParameters.credentials
      val formData = FormData("identity_token" -> creds.identity.token, "password" -> creds.password)

      when(authService.login(eq(loginParameters))(?))
        .thenReturn(Future.successful(loginResult))

      Post(s"$base/login", formData) ~>
        route ~>
        check {
          status shouldBe OK
          val response = responseAs[SessionResultView]
          checkSessionResult(loginResult.asSessionResult, response)
        }
    }
  }
}
