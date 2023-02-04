package ru.yandex.vertis.passport.api.v2.service.users

import org.scalatest.WordSpec
import ru.yandex.passport.model.api.ApiModel
import ru.yandex.vertis.passport.api.{MockedBackend, RootedSpecBase}
import ru.yandex.vertis.passport.api.v2.V2Spec
import ru.yandex.vertis.passport.proto.ApiProtoFormats
import ru.yandex.vertis.passport.test.ModelGenerators
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.util.InvalidEmailException
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.Uri
import org.scalacheck.Gen
import ru.yandex.vertis.passport.service.user.UserService.SearchFilter

import scala.concurrent.Future
import ru.yandex.vertis.passport.api.NoTvmAuthorization

/**
  *
  * @author zvez
  */
class UsersHandlerSpec extends WordSpec with RootedSpecBase with MockedBackend with V2Spec with NoTvmAuthorization {

  val base = "/api/2.x/auto/users"

  "create user" should {
    "return created user" in {
      val source = ModelGenerators.userSource.next
      val result = ModelGenerators.createUserResult.next

      when(userService.create(eq(source))(?))
        .thenReturn(Future.successful(result))

      Post(base, apiCtx.UserSourceFormat.write(source)) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType

          val response = responseAs[ApiModel.CreateUserResult]
          val expectedResponse = apiCtx.CreateUserResultWriter.write(result)
          response shouldBe expectedResponse
        }
    }

    "return 400 and error code on wrong email" in {
      val source = ModelGenerators.userSource.next

      when(userService.create(eq(source))(?))
        .thenReturn(Future.failed(new InvalidEmailException("sss")))

      Post(base, apiCtx.UserSourceFormat.write(source)) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe BadRequest
          contentType shouldBe expectedContentType

          val response = responseAs[ApiModel.SimpleErrorResponse]
          response.getError.getCode shouldBe ApiModel.ErrorCode.INVALID_EMAIL
        }
    }
  }

  "find user" should {
    val users = Gen.listOf(ModelGenerators.fullUser).next
    "find by phone" in {
      val phone = ModelGenerators.phoneNumber.next
      when(userService.search(eq(SearchFilter.ByPhone(phone)))(?))
        .thenReturn(Future.successful(users))
      Get(Uri(s"$base/search").withQuery(Uri.Query("phone" -> phone))) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType

          val response = responseAs[Seq[ApiModel.User]]
          response.map(_.getId) should contain theSameElementsAs users.map(_.id)
        }
    }
    "find by email" in {
      val email = ModelGenerators.emailAddress.next
      when(userService.search(eq(SearchFilter.ByEmail(email)))(?))
        .thenReturn(Future.successful(users))
      Get(Uri(s"$base/search").withQuery(Uri.Query("email" -> email))) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType

          val response = responseAs[Seq[ApiModel.User]]
          response.map(_.getId) should contain theSameElementsAs users.map(_.id)
        }
    }
    "find by social user id" in {
      val socialUser = ModelGenerators.socialProviderUser.next
      val socialProvider = ModelGenerators.socialProvider.next
      when(userService.search(eq(SearchFilter.BySocialUserId(socialProvider, socialUser.id)))(?))
        .thenReturn(Future.successful(users))
      Get(
        Uri(s"$base/search")
          .withQuery(Uri.Query("social-provider" -> socialProvider.toString, "social-user-id" -> socialUser.id))
      ) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType

          val response = responseAs[Seq[ApiModel.User]]
          response.map(_.getId) should contain theSameElementsAs users.map(_.id)
        }
    }
    "fail to use phone and email simultaneously" in {
      val phone = ModelGenerators.phoneNumber.next
      val email = ModelGenerators.emailAddress.next
      Get(Uri(s"$base/search").withQuery(Uri.Query("email" -> email, "phone" -> phone))) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe BadRequest
        }
    }
    "fail to use socialProvider without socialUserId" in {
      val socialProvider = ModelGenerators.socialProvider.next
      Get(Uri(s"$base/search").withQuery(Uri.Query("social-provider" -> socialProvider.toString))) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe BadRequest
        }
    }
    "fail to use socialUserId without socialProvider" in {
      val socialUser = ModelGenerators.socialProviderUser.next
      Get(Uri(s"$base/search").withQuery(Uri.Query("social-user-id" -> socialUser.id))) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe BadRequest
        }
    }
    "fail to use unexpected socialProvider" in {
      val socialUser = ModelGenerators.socialProviderUser.next
      val socialProvider = "unexpected_social_provider"
      Get(
        Uri(s"$base/search")
          .withQuery(Uri.Query("social-provider" -> socialProvider, "social-user-id" -> socialUser.id))
      ) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe BadRequest
        }
    }
    "fail to use empty socialUserId" in {
      val socialUserId = ""
      val socialProvider = ModelGenerators.socialProvider.next
      Get(
        Uri(s"$base/search")
          .withQuery(Uri.Query("social-provider" -> socialProvider.toString, "social-user-id" -> socialUserId))
      ) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe BadRequest
        }
    }
    "fail to use phone and socialProvider/socialUserId pair simultaneously" in {
      val phone = ModelGenerators.phoneNumber.next
      val socialUser = ModelGenerators.socialProviderUser.next
      val socialProvider = ModelGenerators.socialProvider.next
      Get(
        Uri(s"$base/search")
          .withQuery(
            Uri.Query("phone" -> phone, "social-provider" -> socialProvider.toString, "social-user-id" -> socialUser.id)
          )
      ) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe BadRequest
        }
    }
    "fail to use email and socialProvider/socialUserId pair simultaneously" in {
      val email = ModelGenerators.emailAddress.next
      val socialUser = ModelGenerators.socialProviderUser.next
      val socialProvider = ModelGenerators.socialProvider.next
      Get(
        Uri(s"$base/search")
          .withQuery(
            Uri.Query("email" -> email, "social-provider" -> socialProvider.toString, "social-user-id" -> socialUser.id)
          )
      ) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe BadRequest
        }
    }
    "fail if neither is provided" in {
      Get(Uri(s"$base/search")) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe BadRequest
        }
    }
  }
}
