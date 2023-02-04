package ru.yandex.vertis.passport.api.v2.service.users

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.Uri
import org.mockito.Mockito.verify
import org.mockito.Mockito.times
import org.scalatest.WordSpec
import ru.yandex.passport.model.api.ApiModel
import ru.yandex.passport.model.api.ApiModel.{LoadUserHint, PasswordValidationError}
import ru.yandex.vertis.passport.api.v2.V2Spec
import ru.yandex.vertis.passport.api.{MockedBackend, RootedSpecBase}
import ru.yandex.vertis.passport.model.UserResult
import ru.yandex.vertis.passport.model.api.{ChangePasswordParameters, NotifyUserParameters}
import ru.yandex.vertis.passport.proto.NoContextApiProtoFormats
import ru.yandex.vertis.passport.test.ModelGenerators
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.util.PasswordIsTooWeakException

import scala.collection.JavaConverters._
import scala.concurrent.Future
import ru.yandex.vertis.passport.api.NoTvmAuthorization

/**
  *
  * @author zvez
  */
class SingleUserHandlerSpec
  extends WordSpec
  with RootedSpecBase
  with MockedBackend
  with V2Spec
  with NoTvmAuthorization {

  import NoContextApiProtoFormats.ChangePasswordParametersFormat

  val base = "/api/2.x/auto/users"

  "get user" should {
    "return user" in {
      val user = ModelGenerators.legacyUser.next
      val result = UserResult(user)

      when(userService.get(eq(user.id), ?)(?)).thenReturn(Future.successful(result))

      Get(s"$base/${user.id}") ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType
          val response = responseAs[ApiModel.UserResult]
          val expected = apiCtx.UserResultWriter.write(result)
          response shouldBe expected
        }
    }

    "return 404 when user not found" in {
      val userId = ModelGenerators.userId.next

      when(userService.get(eq(userId), ?)(?)).thenReturn(Future.failed(new NoSuchElementException))

      Get(s"$base/$userId") ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe NotFound
          contentType shouldBe expectedContentType
        }
    }

    "support hints" in {
      val user = ModelGenerators.legacyUser.next
      val result = UserResult(user)

      when(userService.get(?, ?)(?)).thenReturn(Future.successful(result))

      Get(s"$base/${user.id}?loadHints=SOMETHING,NOT_CONFIRMED_EMAILS,WRONG") ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType
          val response = responseAs[ApiModel.UserResult]
          val expected = apiCtx.UserResultWriter.write(result)
          response shouldBe expected

          verify(userService).get(eq(user.id), eq(Set(LoadUserHint.NOT_CONFIRMED_EMAILS)))(?)
        }
    }
  }

  "get user essentials" should {
    "return them" in {
      val user = ModelGenerators.userEssentials.next

      when(userProvider.get(eq(user.id), ?, eq(false))(?)).thenReturn(Future.successful(user))

      Get(s"$base/${user.id}/essentials") ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType
          val response = responseAs[ApiModel.UserEssentials]
          val expected = apiCtx.UserEssentialsWriter.write(user)
          response shouldBe expected
        }
    }
    "apply loadLastSeen flag" in {
      val user = ModelGenerators.userEssentials.next

      when(userProvider.get(eq(user.id), ?, eq(true))(?)).thenReturn(Future.successful(user))

      Get(Uri(s"$base/${user.id}/essentials").withQuery(Uri.Query("loadLastSeen" -> "true"))) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType
          val response = responseAs[ApiModel.UserEssentials]
          val expected = apiCtx.UserEssentialsWriter.write(user)
          response shouldBe expected
        }
    }
  }

  "invalidate cached" should {
    "work" in {
      val userId = ModelGenerators.userId.next
      when(sessionFacade.invalidateCachedUser(eq(userId))(?)).thenReturn(Future.unit)

      Post(s"$base/$userId/invalidate-cached") ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
        }
    }
  }

  "change password" should {
    "work" in {
      val userId = ModelGenerators.userId.next
      val params = ChangePasswordParameters(
        ModelGenerators.readableString.next,
        ModelGenerators.readableString.next
      )
      val result = ModelGenerators.loginResult.next

      when(userService.changePassword(eq(userId), eq(params))(?))
        .thenReturn(Future.successful(result))

      Post(s"$base/$userId/password", ChangePasswordParametersFormat.write(params)) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType
          responseAs[ApiModel.LoginResult]
        }
    }

    "fail with PasswordErrorResult" in {
      val userId = ModelGenerators.userId.next
      val params = ChangePasswordParameters(
        ModelGenerators.readableString.next,
        ModelGenerators.readableString.next
      )
      val passwordProblems = Seq(PasswordValidationError.TOO_SHORT)
      when(userService.changePassword(eq(userId), eq(params))(?))
        .thenReturn(Future.failed(new PasswordIsTooWeakException(passwordProblems)))

      Post(s"$base/$userId/password", ChangePasswordParametersFormat.write(params)) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe BadRequest
          contentType shouldBe expectedContentType

          val response = responseAs[ApiModel.PasswordValidationErrorResult]
          response.getPasswordErrorsCount shouldBe 1
          response.getPasswordErrors(0) shouldBe PasswordValidationError.TOO_SHORT
        }
    }
  }

  "notify user" should {
    "work" in {
      val user = ModelGenerators.fullUser.next
      val userId = user.id
      val phone = ModelGenerators.phoneNumber.next
      val text = ModelGenerators.readableString.next
      val params = NotifyUserParameters.PlainSms(Some(phone), text)

      when(communicationService.notifyUserByUserId(eq(userId), eq(params))(?))
        .thenReturn(Future.unit)

      Post(s"$base/$userId/notify", apiCtx.NotifyUserParametersFormat.write(params)) ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
        }
    }
  }

  "get user sessions" should {
    "work" in {
      val user = ModelGenerators.fullUser.next
      val userId = user.id
      val sessions = ModelGenerators.session.next(10)

      when(sessionFacade.getUserSessions(eq(userId))(?))
        .thenReturn(Future.successful(sessions))

      Get(s"$base/$userId/sessions") ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK

          val response = responseAs[ApiModel.GetUserSessionsResult]
          (response.getSessionList.asScala.map(_.getId) should contain
            theSameElementsAs sessions.map(_.id.asString))
        }
    }
  }

  "forget user" should {
    "work" in {
      val user = ModelGenerators.fullUser.next
      val userId = user.id

      when(userService.forgetUser(?)(?))
        .thenReturn(Future.successful(()))

      Post(s"$base/$userId/forget") ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          verify(userService).forgetUser(eq(userId))(?)
        }
    }
  }
}
