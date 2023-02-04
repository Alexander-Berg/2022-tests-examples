package ru.auto.api.routes.v1.user.password

import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{ContentTypes, MediaTypes, StatusCodes}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiSpec
import ru.auto.api.ResponseModel.{LoginResponse, PasswordValidationErrorResponse, RequestPasswordResetResponse, ResponseStatus}
import ru.auto.api.exceptions.{AuthenticationException, PasswordValidationException}
import ru.auto.api.managers.passport.PassportModelConverters._
import ru.auto.api.model.ModelGenerators
import ru.auto.api.model.ModelGenerators.{PassportChangePasswordParamsGen, PassportLoginResultGen, PassportUserIdentityGen, PrivateUserRefGen, RequestPasswordResetParamsGen, RequestPasswordResetResultGen, ResetPasswordParametersGen}
import ru.auto.api.services.MockedClients
import ru.auto.api.util.Protobuf
import ru.yandex.passport.model.api.ApiModel.{PasswordValidationError, RequestPasswordResetParameters, UserIdentity}

import scala.jdk.CollectionConverters.ListHasAsScala
import scala.concurrent.Future

/**
  *
  * @author zvez
  */
class PasswordHandlerSpec extends ApiSpec with MockedClients with ScalaCheckPropertyChecks {

  private val commonHeaders =
    xAuthorizationHeader ~> addHeader(Accept(MediaTypes.`application/json`))

  "change password" should {
    "return LoginResponse and set new session" in {
      forAll(PrivateUserRefGen, PassportChangePasswordParamsGen, PassportLoginResultGen) { (userId, params, result) =>
        when(passportClient.getUserEssentials(eq(userId), ?)(?))
          .thenReturnF(ModelGenerators.UserEssentialsGen.next)
        when(passportClient.changePassword(eq(userId), eq(params))(?))
          .thenReturnF(result)

        Post(s"/1.0/user/password", params) ~>
          addHeader("x-uid", userId.uid.toString) ~>
          commonHeaders ~>
          route ~>
          check {
            val response = responseAs[String]
            withClue(response) {
              status shouldBe StatusCodes.OK
              contentType shouldBe ContentTypes.`application/json`

              val responseParsed = Protobuf.fromJson[LoginResponse](response)
              responseParsed shouldBe result.asAutoru
              responseParsed.getStatus shouldBe ResponseStatus.SUCCESS
            }
            val sessionHeader = header("X-Session-Id")
            sessionHeader shouldBe defined
            sessionHeader.get.value() shouldBe result.getSession.getId
          }
      }
    }

    "fail with PasswordValidationErrorResponse" in {
      forAll(PrivateUserRefGen, PassportChangePasswordParamsGen) { (userId, params) =>
        val errors = Seq(PasswordValidationError.NOT_UNIQUE, PasswordValidationError.TOO_SHORT)

        when(passportClient.getUserEssentials(eq(userId), ?)(?))
          .thenReturnF(ModelGenerators.UserEssentialsGen.next)
        when(passportClient.changePassword(eq(userId), eq(params))(?))
          .thenReturn(Future.failed(new PasswordValidationException(errors)))

        Post(s"/1.0/user/password", params) ~>
          addHeader("x-uid", userId.uid.toString) ~>
          commonHeaders ~>
          route ~>
          check {
            val response = responseAs[String]
            withClue(response) {
              status shouldBe StatusCodes.BadRequest
              contentType shouldBe ContentTypes.`application/json`

              val responseParsed = Protobuf.fromJson[PasswordValidationErrorResponse](response)
              responseParsed.getPasswordErrorsList.asScala.toSeq shouldBe errors
            }
          }
      }
    }

    "fail with AuthenticationException with x-is-login" in {
      forAll(PrivateUserRefGen, PassportChangePasswordParamsGen) { (userId, params) =>
        when(passportClient.getUserEssentials(eq(userId), ?)(?))
          .thenReturnF(ModelGenerators.UserEssentialsGen.next)
        when(passportClient.changePassword(eq(userId), eq(params))(?))
          .thenReturn(Future.failed(new AuthenticationException(isLogin = true)))

        Post(s"/1.0/user/password", params) ~>
          addHeader("x-uid", userId.uid.toString) ~>
          commonHeaders ~>
          route ~>
          check {
            val response = responseAs[String]
            withClue(response) {
              status shouldBe StatusCodes.Unauthorized
              contentType shouldBe ContentTypes.`application/json`
              headers.find(_.is("x-is-login")).fold("")(_.value()) shouldBe "true"
            }
          }
      }
    }
  }

  "request-reset" should {
    "work" in {
      forAll(RequestPasswordResetParamsGen, RequestPasswordResetResultGen) { (params, result) =>
        when(passportClient.createAnonymousSession()(?))
          .thenReturnF(ModelGenerators.AnonSessionResultGen.next)
        when(passportClient.requestPasswordReset(eq(params))(?)).thenReturnF(result)

        Post(s"/1.0/user/password/request-reset", params) ~>
          commonHeaders ~>
          route ~>
          check {
            val response = responseAs[String]
            withClue(response) {
              status shouldBe StatusCodes.OK
              contentType shouldBe ContentTypes.`application/json`

              val responseParsed = Protobuf.fromJson[RequestPasswordResetResponse](response)
              responseParsed shouldBe result.asAutoru
              responseParsed.getStatus shouldBe ResponseStatus.SUCCESS
            }
          }
      }
    }

    "work (deprecated - pass identity)" in {
      forAll(PassportUserIdentityGen, RequestPasswordResetResultGen) { (userIdentity, result) =>
        val expectedParams = userIdentity.getIdentityCase match {
          case UserIdentity.IdentityCase.EMAIL =>
            RequestPasswordResetParameters
              .newBuilder()
              .setEmail(userIdentity.getEmail)
              .build()
          case UserIdentity.IdentityCase.PHONE =>
            RequestPasswordResetParameters
              .newBuilder()
              .setPhone(userIdentity.getPhone)
              .build()
          case _ => ???
        }

        when(passportClient.createAnonymousSession()(?))
          .thenReturnF(ModelGenerators.AnonSessionResultGen.next)
        when(passportClient.requestPasswordReset(eq(expectedParams))(?)).thenReturnF(result)

        Post(s"/1.0/user/password/request-reset", userIdentity) ~>
          commonHeaders ~>
          route ~>
          check {
            val response = responseAs[String]
            withClue(response) {
              status shouldBe StatusCodes.OK
              contentType shouldBe ContentTypes.`application/json`

              val responseParsed = Protobuf.fromJson[RequestPasswordResetResponse](response)
              responseParsed shouldBe result.asAutoru
              responseParsed.getStatus shouldBe ResponseStatus.SUCCESS
            }
          }
      }
    }
  }

  "reset" should {
    "return LoginResponse and set new session" in {
      forAll(ResetPasswordParametersGen, PassportLoginResultGen) { (params, result) =>
        when(passportClient.createAnonymousSession()(?))
          .thenReturnF(ModelGenerators.AnonSessionResultGen.next)
        when(passportClient.resetPassword(eq(params))(?)).thenReturnF(result)

        Post(s"/1.0/user/password/reset", params) ~>
          commonHeaders ~>
          route ~>
          check {
            val response = responseAs[String]
            withClue(response) {
              status shouldBe StatusCodes.OK
              contentType shouldBe ContentTypes.`application/json`

              val responseParsed = Protobuf.fromJson[LoginResponse](response)
              responseParsed shouldBe result.asAutoru
              responseParsed.getStatus shouldBe ResponseStatus.SUCCESS
            }
            val sessionHeader = header("X-Session-Id")
            sessionHeader shouldBe defined
            sessionHeader.get.value() shouldBe result.getSession.getId
          }
      }
    }
  }

}
