package ru.yandex.vertis.passport.api.v2.service.users.confirm

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Query
import org.scalatest.FreeSpec
import ru.yandex.passport.model.api.ApiModel
import ru.yandex.vertis.passport.api.v2.V2Spec
import ru.yandex.vertis.passport.api.{MockedBackend, RootedSpecBase}
import ru.yandex.vertis.passport.model.ConfirmationCode
import ru.yandex.vertis.passport.proto.NoContextApiProtoFormats
import ru.yandex.vertis.passport.test.ModelGenerators
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer

import scala.concurrent.Future
import ru.yandex.vertis.passport.api.NoTvmAuthorization

/**
  *
  * @author zvez
  */
class ConfirmationHandlerSpec
  extends FreeSpec
  with RootedSpecBase
  with MockedBackend
  with V2Spec
  with NoTvmAuthorization {

  import NoContextApiProtoFormats.ConfirmIdentityParametersProtoFormat

  val base = "/api/2.x/auto/users/confirm"

  //scalastyle:off multiple.string.literals
  "confirm" - {
    "email" - {
      "should return valid result" in {
        val identity = ModelGenerators.emailIdentity.next
        val code = ConfirmationCode(identity, ModelGenerators.readableString.next)
        val result = ModelGenerators.confirmIdentityResult.next

        when(userService.confirmIdentity(eq(code), eq(false))(?))
          .thenReturn(Future.successful(result))

        Post(
          Uri(s"$base/email")
            .withQuery(Query("email" -> identity.login, "code" -> code.code))
        ) ~>
          commonHeaders ~>
          route ~>
          check {
            status shouldBe OK
            contentType shouldBe expectedContentType
            val response = responseAs[ApiModel.ConfirmIdentityResult]
            val expected = apiCtx.ConfirmIdentityResultWriter.write(result)
            response shouldBe expected
          }
      }

      "should return 404 if code was not found" in {
        val identity = ModelGenerators.emailIdentity.next
        val code = ConfirmationCode(identity, ModelGenerators.readableString.next)
        when(userService.confirmIdentity(eq(code), eq(false))(?))
          .thenReturn(Future.failed(new NoSuchElementException))

        Post(
          Uri(s"$base/email")
            .withQuery(Query("email" -> identity.login, "code" -> code.code))
        ) ~>
          commonHeaders ~>
          route ~>
          check {
            status shouldBe NotFound
            contentType shouldBe expectedContentType
          }
      }
    }

    "phone" - {
      "should return valid result" in {
        val identity = ModelGenerators.phoneIdentity.next
        val code = ConfirmationCode(identity, ModelGenerators.readableString.next)
        val result = ModelGenerators.confirmIdentityResult.next

        when(userService.confirmIdentity(eq(code), eq(true))(?))
          .thenReturn(Future.successful(result))

        Post(s"$base/phone?code=${code.code}&phone=$identity&createSession=true") ~>
          commonHeaders ~>
          route ~>
          check {
            status shouldBe OK
            contentType shouldBe expectedContentType
            val response = responseAs[ApiModel.ConfirmIdentityResult]
            val expected = apiCtx.ConfirmIdentityResultWriter.write(result)
            response shouldBe expected
          }
      }

      "should return 404 if code was not found" in {
        val identity = ModelGenerators.phoneIdentity.next
        val code = ConfirmationCode(identity, ModelGenerators.readableString.next)
        when(userService.confirmIdentity(eq(code), eq(true))(?))
          .thenReturn(Future.failed(new NoSuchElementException))

        Post(s"$base/phone?code=${code.code}&phone=$identity&createSession=true") ~>
          commonHeaders ~>
          route ~>
          check {
            status shouldBe NotFound
            contentType shouldBe expectedContentType
          }
      }
    }

    "any identity" - {
      "should return valid result" in {
        val params = ModelGenerators.confirmIdentityParams.next
        val result = ModelGenerators.confirmIdentityResult.next

        when(userService.confirmIdentity(eq(params.code), eq(params.createSession))(?))
          .thenReturn(Future.successful(result))

        Post(s"$base", ConfirmIdentityParametersProtoFormat.write(params)) ~>
          commonHeaders ~>
          route ~>
          check {
            status shouldBe OK
            contentType shouldBe expectedContentType
            val response = responseAs[ApiModel.ConfirmIdentityResult]
            val expected = apiCtx.ConfirmIdentityResultWriter.write(result)
            response shouldBe expected
          }
      }
    }
  }
}
