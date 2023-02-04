package ru.yandex.vertis.passport.api.v2.service.users

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Query
import org.scalatest.WordSpec
import ru.yandex.passport.model.api.ApiModel
import ru.yandex.vertis.passport.api.v2.V2Spec
import ru.yandex.vertis.passport.api.{MockedBackend, RootedSpecBase}
import ru.yandex.vertis.passport.model.{AddIdentityResult, AddPhoneParameters, Identity, UserId}
import ru.yandex.vertis.passport.proto.NoContextApiProtoFormats
import ru.yandex.vertis.passport.service.user.social.clients.SimpleOAuth2Client.OAuthAuthorizationException
import ru.yandex.vertis.passport.test.ModelGenerators
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.util.IdentityAlreadyTakenException
import ru.yandex.vertis.passport.util.lang.BooleanOption

import scala.concurrent.Future
import ru.yandex.vertis.passport.api.NoTvmAuthorization

/**
  *
  * @author zvez
  */
class UserIdentitiesHandlerSpec
  extends WordSpec
  with RootedSpecBase
  with MockedBackend
  with V2Spec
  with NoTvmAuthorization {

  import NoContextApiProtoFormats._

  //scalastyle:off multiple.string.literals

  val base = "/api/2.x/auto/users"

  "add phone" should {
    "return ok" in {
      val userId = ModelGenerators.userId.next
      val params = ModelGenerators.addPhoneParams.next
      when(userService.addPhone(eq(userId), eq(params))(?))
        .thenReturn(Future.successful(AddIdentityResult(needConfirm = true, Some("code"))))

      Post(addPhoneUri(userId, params)) ~>
        commonHeaders ~>
        route ~>
        check {
          withClue(responseAs[String]) {
            status shouldBe OK
          }
        }
    }

    "return 409 Conflict if phone is already taken" in {
      val userId = ModelGenerators.userId.next
      val params = ModelGenerators.addPhoneParams.next
      when(userService.addPhone(eq(userId), eq(params))(?))
        .thenReturn(Future.failed(new IdentityAlreadyTakenException("taken")))

      Post(addPhoneUri(userId, params)) ~>
        commonHeaders ~>
        route ~>
        check {
          withClue(responseAs[String]) {
            status shouldBe Conflict
            contentType shouldBe expectedContentType
          }
        }
    }

    "return ok (when params in body)" in {
      val userId = ModelGenerators.userId.next
      val params = ModelGenerators.addPhoneParams.next
      when(userService.addPhone(eq(userId), eq(params))(?))
        .thenReturn(Future.successful(AddIdentityResult(needConfirm = true, Some("code"))))

      Post(s"$base/$userId/phones", AddPhoneParametersProtoFormat.write(params)) ~>
        commonHeaders ~>
        route ~>
        check {
          withClue(responseAs[String]) {
            status shouldBe OK
          }
        }
    }

    "return 409 Conflict if phone is already taken (when params in body)" in {
      val userId = ModelGenerators.userId.next
      val params = ModelGenerators.addPhoneParams.next
      when(userService.addPhone(eq(userId), eq(params))(?))
        .thenReturn(Future.failed(new IdentityAlreadyTakenException("taken")))

      Post(s"$base/$userId/phones", AddPhoneParametersProtoFormat.write(params)) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe Conflict
          contentType shouldBe expectedContentType
        }
    }
  }

  "add social profile" should {
    "return ok" in {
      val userId = ModelGenerators.userId.next
      val params = ModelGenerators.addSocialProfileParams.next
      val result = ModelGenerators.addSocialProfileResult.next
      when(socialUserService.addSocialProfile(eq(userId), eq(params))(?))
        .thenReturn(Future.successful(result))

      Post(s"$base/$userId/social-profiles", AddSocialProfileParametersFormat.write(params)) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK

          val parsedResponse = responseAs[ApiModel.AddSocialProfileResult]
          parsedResponse.getRedirectPath shouldBe result.redirectPath.getOrElse("")
        }
    }

    "return conflict" in {
      val userId = ModelGenerators.userId.next
      val params = ModelGenerators.addSocialProfileParams.next
      when(socialUserService.addSocialProfile(eq(userId), eq(params))(?))
        .thenReturn(Future.failed(new IdentityAlreadyTakenException("something")))

      Post(s"$base/$userId/social-profiles", AddSocialProfileParametersFormat.write(params)) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe Conflict
          contentType shouldBe expectedContentType
        }
    }

    "return Unauthorized" in {
      val userId = ModelGenerators.userId.next
      val params = ModelGenerators.addSocialProfileParams.next
      when(socialUserService.addSocialProfile(eq(userId), eq(params))(?))
        .thenReturn(Future.failed(new OAuthAuthorizationException("something")))

      Post(s"$base/$userId/social-profiles", AddSocialProfileParametersFormat.write(params)) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe Unauthorized
          contentType shouldBe expectedContentType
        }
    }
  }

  "remove social profile" should {
    val userId = ModelGenerators.userId.next
    val params = ModelGenerators.removeSocialProfileParams.next
    "work" when {
      "removeLast is not set" in {
        when(socialUserService.removeSocialProfile(eq(userId), eq(params), eq(false))(?))
          .thenReturn(Future.unit)

        Post(s"$base/$userId/social-profiles/remove", RemoveSocialProfileParametersFormat.write(params)) ~>
          commonHeaders ~>
          route ~>
          check {
            status shouldBe OK
          }
      }
      "removeLast is set" in {
        when(socialUserService.removeSocialProfile(eq(userId), eq(params), eq(true))(?))
          .thenReturn(Future.unit)

        Post(
          Uri(s"$base/$userId/social-profiles/remove").withQuery(Query("removeLast" -> "true")),
          RemoveSocialProfileParametersFormat.write(params)
        ) ~>
          commonHeaders ~>
          route ~>
          check {
            status shouldBe OK
          }
      }
    }
  }

  "change email" should {
    val userId = ModelGenerators.userId.next
    val request = ModelGenerators.requestEmailChangeParams.next
    val code = ModelGenerators.readableString.next
    val params = ModelGenerators.changeEmailParams.next
    val result = ModelGenerators.addIdentityResult.next

    "request change" in {
      when(
        userService.askForEmailChange(eq(userId), eq(request))(?)
      ).thenReturn(Future.successful(code))

      Post(s"$base/$userId/email/request-change", RequestEmailChangeParametersProtoFormat.write(request)) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType
          val result = responseAs[ApiModel.RequestEmailChangeResult]
          result.getCodeLength shouldBe code.length
        }
    }

    "change" in {

      when(
        userService.changeEmail(eq(userId), eq(params))(?)
      ).thenReturn(Future.successful(result))

      Post(s"$base/$userId/email/change", ChangeEmailParametersProtoFormat.write(params)) ~>
        commonHeaders ~>
        route ~>
        check {
          withClue(responseAs[String]) {
            status shouldBe OK
            contentType shouldBe expectedContentType

            val response = responseAs[ApiModel.AddIdentityResult]
            response.getNeedConfirm shouldBe result.needConfirm
          }
        }
    }
  }

  "remove email" should {
    "work" in {
      val userId = ModelGenerators.userId.next
      val email = ModelGenerators.emailAddress.next

      when(userService.removeIdentity(eq(userId), eq(Identity.Email(email)), eq(false))(?))
        .thenReturn(Future.successful(true))

      Delete(Uri(s"$base/$userId/email/").withQuery(Query("email" -> email))) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
        }
    }
  }

  private def addPhoneUri(userId: UserId, params: AddPhoneParameters) = {
    import params._
    val queryParams = Map(
      "phone" -> phone,
      "steal" -> steal.toString
    ) ++
      confirmed.option("confirmed" -> "true") ++
      suppressNotifications.option("suppressNotifications" -> "true")
    Uri(s"$base/$userId/phones").withQuery(Query(queryParams))
  }

}
