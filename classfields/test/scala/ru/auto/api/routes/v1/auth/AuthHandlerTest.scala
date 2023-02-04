package ru.auto.api.routes.v1.auth

import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{ContentTypes, MediaTypes, StatusCodes}
import org.mockito.Mockito.verify
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.ApiSpec
import ru.auto.api.ResponseModel._
import ru.auto.api.exceptions.{AuthenticationException, PasswordExpiredException, PhoneIsBannedException}
import ru.auto.api.managers.favorite.FavoritesHelper
import ru.auto.api.managers.passport.PassportModelConverters.AutoruConvertable
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.history.HistoryEntity
import ru.auto.api.model.{ModelGenerators, SessionID}
import ru.auto.api.services.MockedClients
import ru.auto.api.util.Protobuf
import ru.yandex.passport.model.api.ApiModel._
import ru.yandex.vertis.SocialProvider
import ru.yandex.vertis.subscriptions.api.ApiModel.Watch

import scala.concurrent.Future

/**
  *
  * @author zvez
  */
class AuthHandlerTest extends ApiSpec with MockedClients with ScalaCheckPropertyChecks {

  override lazy val favoritesHelper = mock[FavoritesHelper]

  private val commonHeaders =
    xAuthorizationHeader ~> addHeader(Accept(MediaTypes.`application/json`))

  "login" should {
    "respond with LoginResponse" in {
      forAll(PassportLoginParametersGen, PassportLoginResultGen) { (params, result) =>
        when(passportClient.login(?)(?)).thenReturnF(result)
        when(favoritesHelper.getNotesAndFavorites(?, ?)(?)).thenReturnF(Seq.empty[Offer])
        when(historyClient.getHistory(?)(?)).thenReturnF(Seq.empty[HistoryEntity])
        when(watchClient.getWatch(?)(?)).thenReturnF(Watch.getDefaultInstance)
        when(favoriteClient.moveSavedSearches(?, ?, ?)(?)).thenReturnF(())
        when(subscriptionClient.moveSubscriptions(?, ?, ?)(?)).thenReturnF(())

        Post(s"/1.0/auth/login", params) ~>
          commonHeaders ~>
          route ~>
          check {
            withClue(responseAs[String]) {
              status shouldBe StatusCodes.OK
              contentType shouldBe ContentTypes.`application/json`
            }
            entityAs[LoginResponse] shouldBe result.asAutoru

            verify(passportClient).login(eq(params))(?)
          }
      }
    }

    "fail with Unauthorized" in {
      forAll(PassportLoginParametersGen) { params =>
        when(passportClient.login(?)(?)).thenReturn(Future.failed(new AuthenticationException))

        Post(s"/1.0/auth/login", params) ~>
          commonHeaders ~>
          route ~>
          check {
            withClue(responseAs[String]) {
              status shouldBe StatusCodes.Unauthorized
              contentType shouldBe ContentTypes.`application/json`
            }
            verify(passportClient).login(eq(params))(?)
          }
      }
    }

    "fail when password is expired" in {
      forAll(PassportLoginParametersGen, ReadableStringGen) { (params, code) =>
        when(passportClient.login(?)(?)).thenReturn(Future.failed(new PasswordExpiredException(Some(code))))

        Post(s"/1.0/auth/login", params) ~>
          commonHeaders ~>
          route ~>
          check {
            withClue(responseAs[String]) {
              status shouldBe StatusCodes.Forbidden
              contentType shouldBe ContentTypes.`application/json`

              val response = entityAs[LoginForbiddenErrorResponse]
              response.getChangePasswordCode shouldBe code
            }

            verify(passportClient).login(eq(params))(?)
          }
      }
    }
  }

  "login by token" should {
    "respond with LoginByTokenResponse" in {
      forAll(LoginByTokenParamsGen, LoginByTokenResultGen) { (params, result) =>
        when(passportClient.loginByToken(?)(?)).thenReturnF(result)
        when(favoritesHelper.getNotesAndFavorites(?, ?)(?)).thenReturnF(Seq.empty[Offer])
        when(historyClient.getHistory(?)(?)).thenReturnF(Seq.empty[HistoryEntity])
        when(watchClient.getWatch(?)(?)).thenReturnF(Watch.getDefaultInstance)
        when(favoriteClient.moveSavedSearches(?, ?, ?)(?)).thenReturnF(())
        when(subscriptionClient.moveSubscriptions(?, ?, ?)(?)).thenReturnF(())

        Post(s"/1.0/auth/login-by-token", params) ~>
          commonHeaders ~>
          route ~>
          check {
            withClue(responseAs[String]) {
              status shouldBe StatusCodes.OK
              contentType shouldBe ContentTypes.`application/json`
            }
            entityAs[LoginByTokenResponse] shouldBe result.asAutoru

            verify(passportClient).loginByToken(eq(params))(?)
          }
      }
    }
  }

  "login-social" should {
    "response with SocialLoginResult" in {
      val params =
        SocialLoginParameters
          .newBuilder()
          .setProvider(SocialProvider.HSD)
          .setUser(ModelGenerators.SocialUserSourceGen.next)
          .build()

      Post(s"/1.0/auth/login-social", params) ~>
        commonHeaders ~>
        route ~>
        check {
          withClue(responseAs[String]) {
            status shouldBe StatusCodes.BadRequest
            contentType shouldBe ContentTypes.`application/json`
          }
        }
    }
  }

  "login-social/auth-uri" should {
    "return uri" in {
      forAll(SocialProviderGen, PassportPlatformGen, SocialProviderAuthUriResultGen) { (provider, platform, result) =>
        when(passportClient.getSocialProviderAuthUri(eq(provider), eq(platform), eq(None), eq(None))(?))
          .thenReturnF(result)

        Get(s"/1.0/auth/login-social/auth-uri/$provider?platform=$platform") ~>
          commonHeaders ~>
          route ~>
          check {
            withClue(responseAs[String]) {
              status shouldBe StatusCodes.OK
              contentType shouldBe ContentTypes.`application/json`
            }
            entityAs[SocialProviderAuthUriResult] shouldBe result
          }
      }
    }
  }

  "login-or-register" should {
    "respond with LoginOrRegisterResponse" in {
      forAll(PassportLoginOrRegisterParamsGen, PassportLoginOrRegisterResultGen) { (params, result) =>
        when(passportClient.loginOrRegister(?)(?)).thenReturnF(result)

        Post(s"/1.0/auth/login-or-register", params) ~>
          commonHeaders ~>
          route ~>
          check {
            val response = responseAs[String]
            withClue(response) {
              status shouldBe StatusCodes.OK
              contentType shouldBe ContentTypes.`application/json`

              val responseParsed = Protobuf.fromJson[LoginOrRegisterResponse](response)
              responseParsed.getCodeLength shouldBe result.getCodeLength
              responseParsed.hasStatus shouldBe true
              responseParsed.getStatus shouldBe ResponseStatus.SUCCESS
            }
            verify(passportClient).loginOrRegister(eq(params))(?)
          }
      }
    }

    "fail if phone is banned" in {
      forAll(PassportLoginOrRegisterParamsGen) { params =>
        when(passportClient.loginOrRegister(?)(?)).thenReturn(Future.failed(new PhoneIsBannedException))

        Post(s"/1.0/auth/login-or-register", params) ~>
          commonHeaders ~>
          route ~>
          check {
            val response = responseAs[String]
            withClue(response) {
              status shouldBe StatusCodes.Forbidden
            }
          }
      }
    }
  }

  "logout" should {
    "work" in {
      val currentSession = SessionResultGen.filter(v => !v.hasNewSession).next
      val result = AnonSessionResultGen.filter(v => !v.hasNewSession).next
      val sid = SessionID(currentSession.getSession.getId)
      when(passportClient.getSession(eq(sid))(?)).thenReturnF(currentSession)
      when(passportClient.deleteSession(?)(?)).thenReturn(Future.unit)
      when(passportClient.createAnonymousSession()(?)).thenReturnF(result)
      when(pushnoyClient.detachUsersFromDevice(?)(?)).thenReturn(Future.unit)
      Post(s"/1.0/auth/logout") ~>
        commonHeaders ~>
        addHeader("X-Session-Id", sid.value) ~>
        route ~>
        check {
          val response = responseAs[String]
          withClue(response) {
            status shouldBe StatusCodes.OK
            contentType shouldBe ContentTypes.`application/json`

            val responseParsed = Protobuf.fromJson[SessionResult](response)
            responseParsed shouldBe result
          }
          verify(passportClient).deleteSession(eq(sid))(?)
          verify(passportClient).createAnonymousSession()(?)
        }
    }
  }

}
