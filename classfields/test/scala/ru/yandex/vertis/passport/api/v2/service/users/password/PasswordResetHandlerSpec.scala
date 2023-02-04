package ru.yandex.vertis.passport.api.v2.service.users.password

import akka.http.scaladsl.model.StatusCodes._
import org.scalatest.WordSpec
import ru.yandex.passport.model.api.ApiModel
import ru.yandex.passport.model.api.ApiModel.PasswordValidationError
import ru.yandex.vertis.passport.api.v2.V2Spec
import ru.yandex.vertis.passport.api.{MockedBackend, RootedSpecBase}
import ru.yandex.vertis.passport.model.api.RequestPasswordResetParameters
import ru.yandex.vertis.passport.proto.ProtoUtils._
import ru.yandex.vertis.passport.proto.NoContextApiProtoFormats
import ru.yandex.vertis.passport.test.ModelGenerators
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.util.PasswordIsTooWeakException

import scala.concurrent.Future
import ru.yandex.vertis.passport.api.NoTvmAuthorization

/**
  *
  * @author zvez
  */
class PasswordResetHandlerSpec
  extends WordSpec
  with RootedSpecBase
  with MockedBackend
  with V2Spec
  with NoTvmAuthorization {
  import NoContextApiProtoFormats._

  val base = "/api/2.x/auto/users/password"

  "request password reset" should {
    "work with Identity (deprecated)" in {
      val identity = ModelGenerators.identity.next
      val expectedParams = RequestPasswordResetParameters(identity)

      when(userService.askForPasswordReset(eq(expectedParams))(?))
        .thenReturn(Future.successful("1234"))

      Post(s"$base/request-reset", UserIdentityFormat.write(identity)) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType
          val response = responseAs[ApiModel.RequestPasswordResetResult]
          response.getCodeLength shouldBe 4
        }
    }

    "work" in {
      val params = ModelGenerators.requestPasswordResetParams.next

      when(userService.askForPasswordReset(eq(params))(?))
        .thenReturn(Future.successful("1234"))

      Post(s"$base/request-reset", params.toProto) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType
          val response = responseAs[ApiModel.RequestPasswordResetResult]
          response.getCodeLength shouldBe 4
        }
    }
  }

  "reset password" should {
    "work" in {
      val params = ModelGenerators.resetPasswordParameters.next
      val result = ModelGenerators.loginResult.next

      when(userService.resetPassword(eq(params.confirmationCode), eq(params.newPassword))(?))
        .thenReturn(Future.successful(result))

      Post(s"$base/reset", ResetPasswordParametersFormat.write(params)) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType

          responseAs[ApiModel.LoginResult]
        }
    }

    "fail with PasswordErrorResult" in {
      val params = ModelGenerators.resetPasswordParameters.next
      val passwordProblems = Seq(PasswordValidationError.NOT_UNIQUE)

      when(userService.resetPassword(eq(params.confirmationCode), eq(params.newPassword))(?))
        .thenReturn(Future.failed(new PasswordIsTooWeakException(passwordProblems)))

      Post(s"$base/reset", ResetPasswordParametersFormat.write(params)) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe BadRequest
          contentType shouldBe expectedContentType

          val response = responseAs[ApiModel.PasswordValidationErrorResult]
          response.getPasswordErrorsCount shouldBe 1
          response.getPasswordErrors(0) shouldBe PasswordValidationError.NOT_UNIQUE
        }
    }
  }

}
