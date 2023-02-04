package ru.yandex.vertis.passport.api.v2.service.users.profile

import akka.http.scaladsl.model.StatusCodes._
import org.scalatest.WordSpec
import ru.yandex.passport.model.api.ApiModel
import ru.yandex.vertis.RawMdsIdentity
import ru.yandex.vertis.passport.Domains
import ru.yandex.vertis.passport.api.v2.V2Spec
import ru.yandex.vertis.passport.api.{MockedBackend, RootedSpecBase}
import ru.yandex.vertis.passport.proto.NoContextApiProtoFormats
import ru.yandex.vertis.passport.service.user.userpic.UserpicService
import ru.yandex.vertis.passport.test.ModelGenerators
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer

import scala.concurrent.Future
import ru.yandex.vertis.passport.api.NoTvmAuthorization

/**
  *
  * @author zvez
  */
class UserProfileHandlerSpec
  extends WordSpec
  with RootedSpecBase
  with MockedBackend
  with V2Spec
  with NoTvmAuthorization {
  import NoContextApiProtoFormats._

  val base = "/api/2.x/auto/users"

  "update user profile" should {
    "return updated profile" in {
      val patch = ModelGenerators.autoruUserProfilePatch.next
      val user = ModelGenerators.legacyUser.next

      when(userService.updateProfile(eq(user.id), eq(patch))(?)).thenReturn(Future.successful(user))

      Post(s"$base/${user.id}/profile", UserProfilePatchFormat.write(patch)) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType
          val response = responseAs[ApiModel.UserProfile]
          val expected = apiCtx.UserProfileFormat.write(user.profile)
          response shouldBe expected
        }
    }
  }

  "get user profile" should {
    "return it" in {
      val userId = ModelGenerators.userId.next
      val profile = ModelGenerators.userProfile.next
      when(userService.getProfile(eq(userId))(?)).thenReturn(Future.successful(profile))

      Get(s"$base/$userId/profile") ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType
          val response = responseAs[ApiModel.UserProfile]
          apiCtx.UserProfileFormat.read(response) shouldBe profile
        }
    }
  }

  "get userpic upload uri" should {
    "return it" in {
      val userId = ModelGenerators.userId.next
      val result = ModelGenerators.readableString.next
      when(userpicService.makeUserpicUploadUri(eq(userId))(?)).thenReturn(Future.successful(result))

      Get(s"$base/$userId/profile/userpic-upload-uri") ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType
          val response = responseAs[ApiModel.StringResult]
          response.getValue shouldBe result
        }
    }
  }

  "uploader callback" should {
    "return OK" in {
      val userId = ModelGenerators.userId.next
      val request = RawMdsIdentity
        .newBuilder()
        .setGroupId(42)
        .setName("test")
        .setNamespace("ns")
        .build()
      val expected = MdsImageInfoProtoReader.read(request)
      val imageId = "123-123"

      when(userpicService.setUserpic(eq(userId), eq(expected))(?))
        .thenReturn(Future.successful(imageId))

      val callbackUri = UserpicService.buildCallbackUri("", Domains.Auto, userId)

      Post(callbackUri, request) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType

          val response = responseAs[ApiModel.ImageUrl]
          response.getName shouldBe imageId
        }
    }
  }

}
