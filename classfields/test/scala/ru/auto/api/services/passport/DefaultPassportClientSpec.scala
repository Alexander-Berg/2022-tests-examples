package ru.auto.api.services.passport

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.exceptions._
import ru.auto.api.managers.TestRequest
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model._
import ru.auto.api.model.gen.BasicGenerators
import ru.auto.api.model.gen.NetGenerators
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.api.util.StringUtils._
import ru.yandex.passport.model.api.ApiModel
import ru.yandex.passport.model.api.ApiModel._

import scala.jdk.CollectionConverters.ListHasAsScala

/**
  *
  * @author zvez
  */
class DefaultPassportClientSpec
  extends HttpClientSpec
  with MockedHttpClient
  with ScalaCheckPropertyChecks
  with TestRequest {

  val passportClient = new DefaultPassportClient(http)

  "PassportClient.loginSocial" should {
    "do social login" in {
      forAll(SocialLoginParametersGen, SocialLoginResultGen) { (params, result) =>
        http.expectUrl(POST, "/api/2.x/auto/auth/login-social")
        http.expectProto(params)

        http.respondWith(StatusCodes.OK, result)

        val callResult = passportClient.loginSocial(params).futureValue
        callResult shouldBe result
      }
    }

    "fail with AuthenticationException on login error" in {
      forAll(SocialLoginParametersGen) { params =>
        http.expectUrl(POST, "/api/2.x/auto/auth/login-social")
        http.expectProto(params)

        http.respondWithStatus(StatusCodes.Unauthorized)

        intercept[AuthenticationException] {
          passportClient.loginSocial(params).await
        }
      }
    }
  }

  "PassportClient.getSocialProviderAuthUri" should {
    "return uri" in {
      forAll(SocialProviderGen, PassportPlatformGen, SocialProviderAuthUriResultGen) { (provider, platform, result) =>
        http.expectUrl(GET, url"/api/2.x/auto/auth/login-social/auth-uri/$provider?platform=$platform")

        http.respondWith(StatusCodes.OK, result)

        val callResult = passportClient.getSocialProviderAuthUri(provider, platform, None, None).futureValue
        callResult shouldBe result
      }
    }
  }

  "PassportClient.loginByToken" should {
    "login" in {
      forAll(LoginByTokenParamsGen, LoginByTokenResultGen) { (params, result) =>
        http.expectUrl(POST, url"/api/2.x/auto/auth/login-by-token")
        http.expectProto(params)

        http.respondWith(StatusCodes.OK, result)

        val callResult = passportClient.loginByToken(params).futureValue
        callResult shouldBe result
      }
    }
  }

  "PassportClient.getSession" should {
    "return session" in {
      forAll(SessionResultGen) { result =>
        val sid = result.getSession.getId
        http.reset()
        http.expectUrl(GET, url"/api/2.x/auto/sessions")
        http.expectHeader("X-Session-ID", sid)

        http.respondWith(StatusCodes.OK, result)

        val callResult = passportClient.getSession(SessionID(sid)).futureValue
        callResult shouldBe result
      }
    }

    "fail with SessionNotFoundException if it wasn't found" in {
      forAll(SessionIdGen) { sid =>
        http.reset()
        http.expectUrl(GET, url"/api/2.x/auto/sessions")
        http.expectHeader("X-Session-ID", sid.value)

        http.respondWithStatus(StatusCodes.NotFound)

        intercept[SessionNotFoundException] {
          passportClient.getSession(sid).await
        }
      }
    }

    "fail with SessionNotFoundException when session is invalid" in {
      forAll(SessionIdGen) { sid =>
        http.reset()
        http.expectUrl(GET, url"/api/2.x/auto/sessions")
        http.expectHeader("X-Session-ID", sid.value)

        responseWithPasswordError(StatusCodes.BadRequest, ErrorCode.INVALID_SESSION_ID)
        intercept[SessionNotFoundException] {
          passportClient.getSession(sid).await
        }
      }
    }
  }

  "PassportClient.getUser" should {
    "return user" in {
      forAll(PassportUserGen) { user =>
        http.expectUrl(GET, url"/api/2.x/auto/users/${user.getId}")

        val response = UserResult.newBuilder().setUser(user).build()
        http.respondWith(StatusCodes.OK, response)

        val callResult = passportClient.getUser(AutoruUser(user.getId.toLong)).futureValue
        callResult shouldBe response
      }
    }

    "return user with hinturl" in {
      forAll(PassportUserGen, BasicGenerators.set(1, 3, BasicGenerators.protoEnum(LoadUserHint.values()))) {
        (user, hints) =>
          http.expectUrl(GET, url"/api/2.x/auto/users/${user.getId}?loadHints=${hints.toSeq.mkString(",")}")

          val response = UserResult.newBuilder().setUser(user).build()
          http.respondWith(StatusCodes.OK, response)

          val callResult = passportClient.getUserWithHints(AutoruUser(user.getId.toLong), hints.toSeq).futureValue
          callResult shouldBe response
      }
    }

    "fail with UserNotFoundException if user was not found" in {
      forAll(PrivateUserRefGen) { userId =>
        http.expectUrl(GET, url"/api/2.x/auto/users/${userId.uid}")

        http.respondWithStatus(StatusCodes.NotFound)

        intercept[UserNotFoundException] {
          passportClient.getUser(userId).await
        }
      }
    }
  }

  "PassportClient.getUserEssentials" should {
    "work" in {
      forAll(UserEssentialsGen) { user =>
        http.expectUrl(GET, url"/api/2.x/auto/users/${user.getId}/essentials")

        http.respondWith(StatusCodes.OK, user)

        val callResult =
          passportClient.getUserEssentials(AutoruUser(user.getId.toLong), withLastSeen = false).futureValue
        callResult shouldBe user
      }
    }
  }

  "PassportClient.getUserByEmail" should {
    "work" in {
      forAll(NetGenerators.emailGen, PassportUserGen) { (email, user) =>
        http.expectUrl(GET, url"/api/2.x/auto/users/search?email=$email")
        http.respondWithMany(StatusCodes.OK, List(user))

        val result = passportClient.getUserByEmail(email).futureValue
        result shouldBe user
      }
    }

    "fail on not found" in {
      forAll(NetGenerators.emailGen) { email =>
        http.expectUrl(GET, url"/api/2.x/auto/users/search?email=$email")
        http.respondWithStatus(StatusCodes.NotFound)

        val result = passportClient.getUserByEmail(email).failed.futureValue
        result shouldBe a[UserNotFoundException]
      }
    }

    "fail on not found in results" in {
      forAll(NetGenerators.emailGen) { email =>
        http.expectUrl(GET, url"/api/2.x/auto/users/search?email=$email")
        http.respondWithMany(StatusCodes.OK, List())

        val result = passportClient.getUserByEmail(email).failed.futureValue
        result shouldBe a[UserNotFoundException]
      }
    }

    "fail on invalid email" in {
      val invalidEmail = "invalid-email"

      http.expectUrl(GET, url"/api/2.x/auto/users/search?email=$invalidEmail")
      http.respondWithStatus(StatusCodes.BadRequest)

      val result = passportClient.getUserByEmail(invalidEmail).failed.futureValue
      result shouldBe an[InvalidEmailException]
    }
  }

  "PassportClient.getUserByPhone" should {
    "work" in {
      forAll(NetGenerators.phoneGen, PassportUserGen) { (phone, user) =>
        http.expectUrl(GET, url"/api/2.x/auto/users/search?phone=$phone")
        http.respondWithMany(StatusCodes.OK, List(user))

        val result = passportClient.getUserByPhone(phone).futureValue
        result shouldBe user
      }
    }

    "fail on not found" in {
      forAll(NetGenerators.phoneGen) { phone =>
        http.expectUrl(GET, url"/api/2.x/auto/users/search?phone=$phone")
        http.respondWithStatus(StatusCodes.NotFound)

        val result = passportClient.getUserByPhone(phone).failed.futureValue
        result shouldBe a[UserNotFoundException]
      }
    }

    "fail on not found in results" in {
      forAll(NetGenerators.phoneGen) { phone =>
        http.expectUrl(GET, url"/api/2.x/auto/users/search?phone=$phone")
        http.respondWithMany(StatusCodes.OK, List())

        val result = passportClient.getUserByPhone(phone).failed.futureValue
        result shouldBe a[UserNotFoundException]
      }
    }

    "fail on invalid phone" in {
      val invalidPhone = "invalid-phone"

      http.expectUrl(GET, url"/api/2.x/auto/users/search?phone=$invalidPhone")
      http.respondWithStatus(StatusCodes.BadRequest)

      val result = passportClient.getUserByPhone(invalidPhone).failed.futureValue
      result shouldBe an[InvalidPhoneNumberException]
    }
  }

  "PassportClient.login" should {
    "work" in {
      forAll(PassportLoginParametersGen, PassportLoginResultGen) { (params, result) =>
        http.expectUrl(POST, url"/api/2.x/auto/auth/login")
        http.expectProto(params)

        http.respondWith(StatusCodes.OK, result)
        val res = passportClient.login(params).futureValue
        res shouldBe result
      }
    }

    "fail with PasswordExpiredException" in {
      forAll(PassportLoginParametersGen, ModelGenerators.ReadableStringGen) { (params, code) =>
        http.expectUrl(POST, url"/api/2.x/auto/auth/login")
        http.expectProto(params)

        val response = LoginForbiddenErrorResult
          .newBuilder()
          .setError(ErrorData.newBuilder().setCode(ErrorCode.PASSWORD_EXPIRED))
          .setChangePasswordCode(code)
          .build()
        http.respondWith(StatusCodes.Forbidden, response)

        val ex = intercept[PasswordExpiredException] {
          throw passportClient.login(params).failed.futureValue
        }
        ex.changePasswordCode shouldBe Some(code)
      }
    }
  }

  "Passportclient.loginOrRegisterInt" should {
    "work" in {
      forAll(PassportInternalLoginOrRegisterParametersGen, PassportLoginResultGen) { (params, result) =>
        http.expectUrl(POST, s"/api/2.x/auto/auth/login-or-register-int")
        http.expectProto(params)

        http.respondWith(StatusCodes.OK, result)
        val res = passportClient.loginOrRegisterInt(params).futureValue
        res shouldBe result
      }
    }
  }

  "PassportClient.loginOrRegister" should {
    "work" in {
      forAll(PassportLoginOrRegisterParamsGen, PassportLoginOrRegisterResultGen) { (params, loginResult) =>
        http.expectUrl(POST, url"/api/2.x/auto/auth/login-or-register")
        http.expectProto(params)

        http.respondWith(StatusCodes.OK, loginResult)

        val res = passportClient.loginOrRegister(params).futureValue
        res shouldBe loginResult
      }
    }

    "fail with PhoneIsBannedException if phone is banned" in {
      forAll(PassportLoginOrRegisterParamsGen) { params =>
        http.expectUrl(POST, url"/api/2.x/auto/auth/login-or-register")
        http.expectProto(params)

        responseWithPasswordError(StatusCodes.Forbidden, ErrorCode.PHONE_IS_BANNED)

        intercept[PhoneIsBannedException] {
          passportClient.loginOrRegister(params).await
        }
      }
    }

    "fail with PasswordAuthRequiredException" in {
      forAll(PassportLoginOrRegisterParamsGen) { params =>
        http.expectUrl(POST, url"/api/2.x/auto/auth/login-or-register")
        http.expectProto(params)

        responseWithPasswordError(StatusCodes.Forbidden, ErrorCode.PASSWORD_AUTH_REQUIRED)

        intercept[PasswordAuthRequiredException] {
          passportClient.loginOrRegister(params).await
        }
      }
    }

    "fail with InvalidEmailException" in {
      forAll(PassportLoginOrRegisterParamsGen) { params =>
        http.expectUrl(POST, url"/api/2.x/auto/auth/login-or-register")
        http.expectProto(params)

        responseWithPasswordError(StatusCodes.BadRequest, ErrorCode.INVALID_EMAIL)

        intercept[InvalidEmailException] {
          passportClient.loginOrRegister(params).await
        }
      }
    }
  }

  "PassportClient.deleteSession" should {
    "work" in {
      forAll(SessionIdGen) { sid =>
        http.reset()
        http.expectUrl(DELETE, url"/api/2.x/auto/sessions")
        http.expectHeader("X-Session-ID", sid.value)

        http.respondWithStatus(StatusCodes.OK)
        passportClient.deleteSession(sid).futureValue
      }
    }
  }

  "PassportClient.addPhone" should {
    "work" in {
      forAll(PrivateUserRefGen, PassportAddPhoneParamsGen) { (user, params) =>
        http.expectUrl(POST, url"/api/2.x/auto/users/${user.uid}/phones")
        http.expectProto(params)

        http.respondWith(AddIdentityResult.newBuilder().build())

        passportClient.addPhone(user, params).futureValue
      }
    }
  }

  "PassportClient.confirmIdentity" should {
    "work" in {
      forAll(ConfirmIdentityParamsGen, ConfirmIdentityResultGen) { (params, result) =>
        http.expectUrl(POST, url"/api/2.x/auto/users/confirm")
        http.expectProto(params)
        http.respondWith(result)

        val callResult = passportClient.confirmIdentity(params).futureValue
        callResult shouldBe result
      }
    }

    "fail on invalid phone" in {
      forAll(ConfirmIdentityParamsGen) { (params) =>
        http.expectUrl(POST, url"/api/2.x/auto/users/confirm")
        responseWithPasswordError(StatusCodes.BadRequest, ErrorCode.INVALID_PHONE)

        intercept[InvalidPhoneNumberException] {
          passportClient.confirmIdentity(params).await
        }
      }
    }
  }

  "PassportClient.getDeviceUid" should {
    "return it" in {
      forAll(DeviceUidGen) { deviceUid =>
        val result = ApiModel.DeviceUidResult.newBuilder().setDeviceUid(deviceUid).build
        http.expectUrl(GET, url"/api/2.x/auto/device/uid")
        http.respondWith(result)

        val callResult = passportClient.getDeviceUid().futureValue
        callResult shouldBe result
      }
    }
  }

  "PassportClient.addSocialProfile" should {
    "work with empty response" in {
      forAll(PrivateUserRefGen, PassportAddSocialProfileParamsGen) { (user, params) =>
        http.expectUrl(POST, url"/api/2.x/auto/users/${user.uid}/social-profiles")
        http.respondWithStatus(StatusCodes.OK)

        val expectedResult = AddSocialProfileResult.newBuilder().build()

        passportClient.addSocialProfile(user, params).futureValue shouldBe expectedResult
      }
    }

    "work" in {
      forAll(PrivateUserRefGen, PassportAddSocialProfileParamsGen, PassportAddSocialProfileResultGen) {
        (user, params, response) =>
          http.expectUrl(POST, url"/api/2.x/auto/users/${user.uid}/social-profiles")
          http.respondWith(response)

          passportClient.addSocialProfile(user, params).futureValue shouldBe response
      }
    }
  }

  "PassportClient.removeSocialProfile" should {
    "work" in {
      val removeLast = BasicGenerators.bool.next
      forAll(PrivateUserRefGen, PassportRemoveSocialProfileParamsGen) { (user, params) =>
        http.expectUrl(POST, url"/api/2.x/auto/users/${user.uid}/social-profiles/remove?removeLast=$removeLast")
        http.respondWith(params)

        passportClient.removeSocialProfile(user, params, removeLastIdentity = removeLast).futureValue
      }
    }
  }

  "PassportClient.updateUserProfile" should {
    "work" in {
      forAll(PrivateUserRefGen, PassportAutoruProfileGen, PassportProfilePatchGen) { (userId, profile, patch) =>
        val expectedRequest = UserProfilePatch.newBuilder().setAutoru(patch).build()
        http.expectUrl(POST, url"/api/2.x/auto/users/${userId.uid}/profile")
        http.expectProto(expectedRequest)
        http.respondWith(UserProfile.newBuilder().setAutoru(profile).build())

        val res = passportClient.updateUserProfile(userId, patch).futureValue
        res shouldBe profile
      }
    }
    "translate error" in {
      forAll(PrivateUserRefGen, PassportAutoruProfileGen, PassportProfilePatchGen) { (userId, profile, patch) =>
        val expectedRequest = UserProfilePatch.newBuilder().setAutoru(patch).build()
        http.expectUrl(POST, url"/api/2.x/auto/users/${userId.uid}/profile")
        http.expectProto(expectedRequest)
        responseWithPasswordError(StatusCodes.BadRequest, ErrorCode.ALIAS_IS_NOT_UNIQUE)

        val res = passportClient.updateUserProfile(userId, patch).failed.futureValue
        res shouldBe an[AliasIsNotUniqueException]
      }
    }
  }

  "PassportClient.getUserProfile" should {
    "return it" in {
      forAll(PrivateUserRefGen, PassportAutoruProfileGen) { (userId, profile) =>
        http.expectUrl(GET, url"/api/2.x/auto/users/${userId.uid}/profile")
        http.respondWith(UserProfile.newBuilder().setAutoru(profile).build())

        val res = passportClient.getUserProfile(userId).futureValue
        res shouldBe profile
      }
    }
  }

  "PassportClient.changePassword" should {
    "work" in {
      forAll(PrivateUserRefGen, PassportChangePasswordParamsGen, PassportLoginResultGen) { (userId, params, result) =>
        http.expectUrl(POST, url"/api/2.x/auto/users/${userId.uid}/password")
        http.expectProto(params)
        http.respondWith(result)

        val res = passportClient.changePassword(userId, params).futureValue
        res shouldBe result
      }
    }

    "translate password validation error" in {
      forAll(PrivateUserRefGen, PassportChangePasswordParamsGen, PasswordValidationErrorResultGen) {
        (userId, params, response) =>
          http.expectUrl(POST, url"/api/2.x/auto/users/${userId.uid}/password")
          http.expectProto(params)

          http.respondWith(StatusCodes.BadRequest, response)

          val ex = intercept[PasswordValidationException] {
            throw passportClient.changePassword(userId, params).failed.futureValue
          }
          ex.problems should contain theSameElementsAs response.getPasswordErrorsList.asScala
      }
    }
  }

  "PassportClient.requestPasswordReset" should {
    "work" in {
      forAll(RequestPasswordResetParamsGen, RequestPasswordResetResultGen) { (params, result) =>
        http.expectUrl(POST, url"/api/2.x/auto/users/password/request-reset")
        http.expectProto(params)
        http.respondWith(result)

        passportClient.requestPasswordReset(params).futureValue shouldBe result
      }
    }
  }

  "PassportClient.resetPassword" should {
    "work" in {
      forAll(ResetPasswordParametersGen, PassportLoginResultGen) { (params, result) =>
        http.expectUrl(POST, url"/api/2.x/auto/users/password/reset")
        http.expectProto(params)
        http.respondWith(result)

        passportClient.resetPassword(params).futureValue shouldBe result
      }
    }

    "translate password validation error" in {
      forAll(ResetPasswordParametersGen, PasswordValidationErrorResultGen) { (params, response) =>
        http.expectUrl(POST, url"/api/2.x/auto/users/password/reset")
        http.expectProto(params)

        http.respondWith(StatusCodes.BadRequest, response)

        val ex = intercept[PasswordValidationException] {
          throw passportClient.resetPassword(params).failed.futureValue
        }
        ex.problems should contain theSameElementsAs response.getPasswordErrorsList.asScala
      }
    }
  }

  "PassportClient.removePhone" should {
    "work" in {
      forAll(PrivateUserRefGen, PhoneGen) { (userId, phone) =>
        http.expectUrl(DELETE, url"/api/2.x/auto/users/${userId.uid}/phones/$phone")
        http.respondWithStatus(StatusCodes.OK)

        passportClient.removePhone(userId, phone)
      }
    }
  }

  "PassportClient.requestEmailChange" should {
    "work" in {
      forAll(PrivateUserRefGen, PassportRequestEmailChangeParamsGen, PassportRequestEmailChangeResultGen) {
        (userId, params, result) =>
          http.expectUrl(POST, url"/api/2.x/auto/users/${userId.uid}/email/request-change")
          http.expectProto(params)

          http.respondWith(StatusCodes.OK, result)

          passportClient.requestEmailChangeCode(userId, params).futureValue shouldBe result
      }
    }
  }

  "PassportClient.changeEmail" should {
    "work" in {
      forAll(PrivateUserRefGen, PassportChangeEmailParamsGen, PassportAddIdentityResultGen) {
        (userId, params, result) =>
          http.expectUrl(POST, url"/api/2.x/auto/users/${userId.uid}/email/change")
          http.expectProto(params)

          http.respondWith(StatusCodes.OK, result)

          passportClient.changeEmail(userId, params).futureValue shouldBe result
      }
    }
  }

  "PassportClient.grants" should {
    "work" in {
      forAll(PrivateUserRefGen, PassportUserGrantsGen) { (userId, result) =>
        http.expectUrl(GET, url"/api/2.x/auto/users/${userId.uid}/grants")

        http.respondWith(result)

        passportClient.getGrants(userId).futureValue shouldBe result
      }
    }
  }

  "PassportClient.grants" should {
    "add appHash param" in {
      forAll(PrivateUserRefGen, PassportUserGrantsGen) { (userId, result) =>
        http.expectUrl(GET, url"/api/2.x/auto/users/${userId.uid}/grants")
        http.expectHeader("X-AppHash", "test-hash")
        http.respondWith(result)
        val passportRequestInfo = PassportRequestInfo.fromRequest(request).copy(appHash = Some("test-hash"))
        passportClient.getGrants(userId)(passportRequestInfo).futureValue shouldBe result
      }
    }
  }

  "PassportClient.getApiToken" should {
    "work" in {
      forAll(ApiTokenResultGen()) { (result) =>
        http.expectUrl(GET, url"/api/2.x/auto/api-tokens/${result.getToken.getId}")

        http.respondWith(result)

        passportClient.getApiToken(result.getToken.getId).futureValue shouldBe result
      }
    }
  }

  "PassportClient.getDealerUsers" should {
    "work" in {
      forAll(DealerUserRefGen) { dealer =>
        val result =
          UserIdsResult
            .newBuilder()
            .addUserIds("1234567")
            .addUsers {
              User
                .newBuilder()
                .setId("1234567")
                .setActive(true)
            }
            .build()

        http.expectUrl(GET, url"/api/2.x/auto/clients/id/${dealer.clientId}/users?withProfile=true")
        http.respondWith(result)

        passportClient.getDealerUsers(dealer).futureValue shouldBe result
      }
    }
  }

  "PassportClient.linkDealerUser" should {
    "work" in {
      forAll(DealerUserRefGen, PrivateUserRefGen) { (dealer, user) =>
        val userId = user.uid.toString

        http.expectUrl(
          POST,
          url"/api/2.x/auto/clients/id/${dealer.clientId}/users?userId=$userId&clientGroup=test-group"
        )
        http.respondWithStatus(StatusCodes.OK)

        passportClient.linkDealerUser(dealer, user, "test-group")
      }
    }
  }

  "PassportClient.voxNameAndPassword" should {
    "work" in {
      forAll(PrivateUserRefGen) { user =>
        val userId = user.uid

        http.expectUrl(
          GET,
          url"/api/2.x/auto/vox/by-user-id?user-id=$userId"
        )
        http.respondWithStatus(StatusCodes.OK)

        passportClient.voxNameAndPassword(userId)
      }
    }
  }

  "PassportClient.unlinkDealerUser" should {
    "work" in {
      forAll(DealerUserRefGen, PrivateUserRefGen) { (dealer, user) =>
        val userId = user.uid.toString

        http.expectUrl(DELETE, url"/api/2.x/auto/clients/user/$userId")
        http.respondWithStatus(StatusCodes.OK)

        passportClient.unlinkDealerUser(dealer, user)
      }
    }
  }

  private def responseWithPasswordError(code: StatusCode, errorCode: ApiModel.ErrorCode): Unit = {
    val response = SimpleErrorResponse
      .newBuilder()
      .setError(ErrorData.newBuilder().setCode(errorCode))
      .build()
    http.respondWith(code, response)
  }
}
