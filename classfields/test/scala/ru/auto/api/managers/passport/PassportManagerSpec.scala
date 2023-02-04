package ru.auto.api.managers.passport

import akka.http.scaladsl.model.StatusCodes
import org.mockito.Mockito
import org.mockito.Mockito.verifyNoMoreInteractions
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks._
import ru.auto.api.ResponseModel.LoginOrRegisterResponse
import ru.auto.api.auth.Application
import ru.auto.api.managers.TestRequest
import ru.auto.api.managers.fake.FakeManager
import ru.auto.api.managers.features.AppsFeaturesManager
import ru.auto.api.managers.passport.PassportModelConverters.AutoruConvertable
import ru.auto.api.managers.sync.SyncManager
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.gen.BasicGenerators
import ru.auto.api.model.{RequestParams, UserRef}
import ru.auto.api.services.passport.{PassportClient, PassportRequestInfo}
import ru.auto.api.services.pushnoy.PushnoyClient
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.api.{AsyncTasksSupport, BaseSpec}
import ru.yandex.passport.model.api.ApiModel.{LoginOrRegisterParameters, LoginOrRegisterResult, SocialLoginParameters, UserResult}
import ru.yandex.vertis.SocialProvider
import ru.yandex.vertis.SocialProvider._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import scala.jdk.CollectionConverters._
import scala.concurrent.Future

/**
  *
  * @author zvez
  */
class PassportManagerSpec extends BaseSpec with MockitoSupport with AsyncTasksSupport {

  class Context extends TestRequest {
    val passportClient: PassportClient = mock[PassportClient]
    val pushnoyClient: PushnoyClient = mock[PushnoyClient]
    val syncManager: SyncManager = mock[SyncManager]
    val appsFeaturesManager: AppsFeaturesManager = mock[AppsFeaturesManager]
    val fakeManager: FakeManager = mock[FakeManager]

    val passportManager =
      new PassportManager(passportClient, pushnoyClient, syncManager, appsFeaturesManager, fakeManager)
  }

  private def withContext(f: Context => Any): Unit = {
    val ctx = new Context
    f(ctx)
    Mockito.verifyNoMoreInteractions(ctx.passportClient, ctx.pushnoyClient, ctx.syncManager)
  }

  "PassportManger.addPhone" should {
    val paramsGen =
      PassportAddPhoneParamsGen.map(_.toBuilder.setConfirmed(false).setSteal(false).build())
    val confirmedParamsGen =
      PassportAddPhoneParamsGen.map(_.toBuilder.setConfirmed(true).setSteal(false).build())

    "call loginOrRegister for anon user" in withContext { ctx =>
      implicit val request: Request = ctx.request
      forAll(AnonymousUserRefGen, paramsGen) { (user, params) =>
        val expectedLoginParams = LoginOrRegisterParameters
          .newBuilder()
          .setPhone(params.getPhone)
          .build()
        val passportRequestInfo = PassportRequestInfo.fromRequest(request).copy(appHash = Some("Od0YYJizRLJ"))
        when(ctx.appsFeaturesManager.isSmsRetrieverSupported(?)).thenReturnF(true)
        when(ctx.passportClient.loginOrRegister(eq(expectedLoginParams))(eq(passportRequestInfo)))
          .thenReturnF(LoginOrRegisterResult.newBuilder().setCodeLength(6).build())

        val res = ctx.passportManager.addPhone(user, params).futureValue
        res.getNeedConfirm shouldBe true
        Mockito.verify(ctx.passportClient).loginOrRegister(eq(expectedLoginParams))(eq(passportRequestInfo))
      }
    }

    "call addPhone for logged-in user" in withContext { ctx =>
      implicit val request: Request = ctx.request
      forAll(PrivateUserRefGen, paramsGen, PassportAddIdentityResultGen) { (user, params, result) =>
        val expectedParams = params.toBuilder.setSteal(true).build()
        when(ctx.passportClient.addPhone(eq(user), eq(expectedParams))(?))
          .thenReturnF(result)
        when(ctx.appsFeaturesManager.isSmsRetrieverSupported(?)).thenReturnF(false)

        val res = ctx.passportManager.addPhone(user, params).futureValue
        res shouldBe result.asAutoru

        Mockito.verify(ctx.passportClient).addPhone(eq(user), eq(expectedParams))(?)
      }
    }

    "call addPhone (confirmed case)" in withContext { ctx =>
      implicit val request: Request = ctx.request
      forAll(PrivateUserRefGen, confirmedParamsGen, PassportAddIdentityResultGen) { (user, params, result) =>
        val expectedParams = params.toBuilder.setSteal(false).build()
        when(ctx.appsFeaturesManager.isSmsRetrieverSupported(?)).thenReturnF(false)
        when(ctx.passportClient.addPhone(eq(user), eq(expectedParams))(?))
          .thenReturnF(result)

        val res = ctx.passportManager.addPhone(user, params).futureValue
        res shouldBe result.asAutoru

        Mockito.verify(ctx.passportClient).addPhone(eq(user), eq(expectedParams))(?)
      }
    }
  }

  "PassportManger.confirmIdentity" should {

    "ask for new session for anon user" in {
      forAll(AnonymousUserRefGen, ConfirmIdentityParamsGen, ConfirmIdentityResultGen) { (user, params, result) =>
        withContext { ctx =>
          implicit val request: Request = ctx.request
          val expectedParams = params.toBuilder.setCreateSession(true).build()

          when(ctx.passportClient.confirmIdentity(eq(expectedParams))(?)).thenReturnF(result)
          when(ctx.syncManager.syncUserData(?, ?)(?)).thenReturn(Future.unit)
          when(ctx.appsFeaturesManager.isSmsRetrieverSupported(?)).thenReturnF(false)

          val res = ctx.passportManager.confirmIdentity(user, params).futureValue
          request.tasks.start(StatusCodes.OK).foreach(_.await)
          res shouldBe result

          val newUserId = result.getUser.getId
          newUserId should not be empty
          val newUser = UserRef.user(newUserId.toLong)

          Mockito.verify(ctx.syncManager).syncUserData(eq(newUser), ?)(?)
          Mockito.verify(ctx.passportClient).confirmIdentity(eq(expectedParams))(?)
        }
      }
    }

    "don't ask for new session for logged-in user" in withContext { ctx =>
      implicit val request: Request = ctx.request
      forAll(PrivateUserRefGen, ConfirmIdentityParamsGen, ConfirmIdentityResultGen) { (user, params, result) =>
        val expectedParams = params.toBuilder.setCreateSession(false).build()

        when(ctx.appsFeaturesManager.isSmsRetrieverSupported(?)).thenReturnF(false)
        when(ctx.passportClient.confirmIdentity(eq(expectedParams))(?))
          .thenReturnF(result)

        val res = ctx.passportManager.confirmIdentity(user, params).futureValue
        res shouldBe result

        Mockito.verify(ctx.passportClient).confirmIdentity(eq(expectedParams))(?)
      }
    }
  }

  "PassportManger.logout" should {
    "return parent session if any" in {
      forAll(PrivateUserRefGen, SessionResultGen, SessionResultGen) { (user, sr, parentSr) =>
        withContext { ctx =>
          val sid = sr.getSession.getId

          val preparedSessionResult = {
            val session = sr.getSession.toBuilder.setParentSession(parentSr.getSession)
            sr.toBuilder.setSession(session).build
          }

          implicit val request = {
            val r = new RequestImpl
            r.setTrace(Traced.empty)
            r.setRequestParams(RequestParams.construct("1.1.1.1", sessionId = Some(sid)))
            r.setApplication(Application.swagger)
            r.setUser(user)
            r.setSession(preparedSessionResult)
            r
          }

          when(ctx.passportClient.deleteSession(?)(?)).thenReturn(Future.unit)
          when(ctx.passportClient.getSession(?)(?)).thenReturnF(parentSr)
          when(ctx.pushnoyClient.detachUsersFromDevice(?)(?)).thenReturn(Future.unit)

          val result = ctx.passportManager.logout()(request).futureValue
          request.tasks.start(StatusCodes.OK).foreach(_.await)

          result.getSession shouldBe parentSr.getSession
          result.getUser shouldBe parentSr.getUser
          result.hasReturnPath shouldBe sr.getSession.getReturnPath.nonEmpty
          result.getReturnPath shouldBe sr.getSession.getReturnPath
          Mockito.verify(ctx.passportClient).deleteSession(eq(sid))(?)
          Mockito.verify(ctx.passportClient).getSession(eq(parentSr.getSession.getId))(?)
          Mockito.verify(ctx.pushnoyClient).detachUsersFromDevice(eq(sr.getSession.getDeviceUid))(?)
        }
      }
    }
  }

  "PassportManager.loginOrRegisterInt" should {
    "work" in withContext { ctx =>
      implicit val request: Request = ctx.request
      forAll(PassportInternalLoginOrRegisterParametersGen, PassportLoginResultGen) { (params, loginResult) =>
        when(ctx.appsFeaturesManager.isSmsRetrieverSupported(?)).thenReturnF(false)
        when(ctx.passportClient.loginOrRegisterInt(?)(?)).thenReturnF(loginResult)
        val res = ctx.passportManager.loginOrRegisterInt(params).futureValue
        res shouldBe loginResult.asAutoru
        Mockito.verify(ctx.passportClient, Mockito.atLeast(1)).loginOrRegisterInt(eq(params))(?)
      }
    }
  }

  "PassportManager.loginOrRegister" should {
    "check redirect_path domain" in withContext { ctx =>
      when(ctx.fakeManager.shouldSkipRegistration(?)).thenReturn(false)

      implicit val request: Request = ctx.request
      val result = PassportLoginOrRegisterResultGen.next
      when(ctx.passportClient.loginOrRegister(?)(?)).thenReturnF(result)
      when(ctx.appsFeaturesManager.isSmsRetrieverSupported(?)).thenReturnF(false)
      val builder = PassportLoginOrRegisterParametersGen.next.toBuilder
      builder.getEmailSettingsBuilder.setRedirectPath("https://auto.ru/some/path/")
      val params = builder.build

      ctx.passportManager.loginOrRegister(params, RegisterContext.Default).futureValue

      Mockito.verify(ctx.passportClient).loginOrRegister(?)(?)
    }

    "forbid bad redirect_path domain" in withContext { ctx =>
      when(ctx.fakeManager.shouldSkipRegistration(?)).thenReturn(false)

      implicit val request: Request = ctx.request
      when(ctx.appsFeaturesManager.isSmsRetrieverSupported(?)).thenReturnF(false)
      val builder = PassportLoginOrRegisterParametersGen.next.toBuilder
      builder.getEmailSettingsBuilder.setRedirectPath("https://trashauto.ru/some/path/")
      val params = builder.build
      an[IllegalArgumentException] shouldBe thrownBy {
        ctx.passportManager.loginOrRegister(params, RegisterContext.Default).await
      }
    }

    "call loginOrRegister for anon robot user " in withContext { ctx =>
      when(ctx.fakeManager.shouldSkipRegistration(?)).thenReturn(true)
      val resp = LoginOrRegisterResponse.newBuilder().build()
      when(ctx.fakeManager.getFakeLoginOrRegisterResponse(?)).thenReturn(resp)

      implicit val request: Request = ctx.request
      val result = PassportLoginOrRegisterResultGen.next
      when(ctx.passportClient.loginOrRegister(?)(?)).thenReturnF(result)
      when(ctx.appsFeaturesManager.isSmsRetrieverSupported(?)).thenReturnF(false)
      val builder = PassportLoginOrRegisterParametersGen.next.toBuilder
      builder.getEmailSettingsBuilder.setRedirectPath("https://auto.ru/some/path/")
      val params = builder.build

      val res = ctx.passportManager.loginOrRegister(params, RegisterContext.Default).futureValue
      assert(res == resp)
      verifyNoMoreInteractions(ctx.passportClient)
    }
  }

  "PassportManager.enrichLoginOrRegisterParams" should {
    "fallback bad redirect_path URI" in withContext { ctx =>
      implicit val request: Request = ctx.request
      when(ctx.appsFeaturesManager.isSmsRetrieverSupported(?)).thenReturnF(false)
      val paramsBuilder = PassportLoginOrRegisterParametersGen.next.toBuilder
      paramsBuilder.getEmailSettingsBuilder.setRedirectPath("https://trashauto.ru/some/path/?a= b")
      val params = paramsBuilder.build

      val builder = PassportManager.enrichLoginOrRegisterParams(params, RegisterContext.Default)
      builder.getEmailSettingsOrBuilder.getRedirectPath shouldBe PassportManager.FallbackRedirectPath
    }
  }

  "PassportManager.login" should {
    "attach dealer device to dealer id" in withContext { ctx =>
      val loginResult = PassportLoginResultGen.next.toBuilder
      val clientId = DealerUserRefGen.next.clientId.toString
      loginResult.getUserBuilder.getProfileBuilder.getAutoruBuilder.setClientId(clientId)
      val dealerRef = UserRef.dealer(loginResult.getUser.getProfile.getAutoru.getClientId.toLong)
      implicit val request: Request = iosRequestGen.next
      val loginParams = PassportLoginParametersGen.next

      when(ctx.appsFeaturesManager.isSmsRetrieverSupported(?)).thenReturnF(false)
      when(ctx.passportClient.login(?)(?)).thenReturnF(loginResult.build())
      when(ctx.pushnoyClient.attachDeviceToUser(?, ?)(?)).thenReturn(Future.unit)
      when(ctx.syncManager.syncUserData(?, ?)(?)).thenReturn(Future.unit)

      ctx.passportManager.login(loginParams)(request).futureValue
      request.tasks.start(StatusCodes.OK).foreach(_.await)

      Mockito.verify(ctx.pushnoyClient).attachDeviceToUser(eq(dealerRef.toPlain), ?)(?)
      Mockito.verify(ctx.passportClient).login(?)(?)
      Mockito.verify(ctx.syncManager).syncUserData(?, ?)(?)
    }
  }

  "PassportManager.removeSocialProfile" should {
    "not set removeLast for all apps except auto24" in withContext { ctx =>
      implicit val request: Request = ctx.request
      val user = PrivateUserRefGen.next
      val socialProvider = SocialProviderGen.filter(_ != SocialProvider.S_24_AUTO).next
      val socialProfileId = BasicGenerators.readableString.next
      when(ctx.passportClient.removeSocialProfile(?, ?, eq(false))(?)).thenReturn(Future.unit)
      ctx.passportManager.removeSocialProfile(user, socialProvider, socialProfileId).futureValue
      Mockito.verify(ctx.passportClient).removeSocialProfile(?, ?, eq(false))(?)
    }
  }

  "PassportManager.getModerationStatus" should {
    "work" in withContext { ctx =>
      implicit val request: Request = ctx.request
      forAll(userModerationStatusResponseGen) { response =>
        val user = PrivateUserRefGen.next
        when(ctx.passportClient.getUserModeration(?)(?)).thenReturnF(response.getModerationStatus)
        val res = ctx.passportManager.getModerationStatus(user).futureValue
        res shouldBe response
        Mockito.verify(ctx.passportClient).getUserModeration(eq(user))(?)
      }
    }
  }

  "PassportManager.loginByToken" should {
    "work" in forAll(LoginByTokenParamsGen, LoginByTokenResultGen) { (params, result) =>
      withContext { ctx =>
        implicit val request: Request = ctx.request
        when(ctx.appsFeaturesManager.isSmsRetrieverSupported(?)).thenReturnF(false)
        when(ctx.passportClient.loginByToken(?)(?)).thenReturnF(result)
        when(ctx.syncManager.syncUserData(?, ?)(?)).thenReturn(Future.unit)
        val res = ctx.passportManager.loginByToken(params).futureValue
        request.tasks.start(StatusCodes.OK).foreach(_.await)

        res.getSession shouldBe result.getSession
        Mockito.verify(ctx.passportClient).loginByToken(?)(?)
        Mockito.verify(ctx.syncManager).syncUserData(?, ?)(?)
      }
    }
  }

  "PassportManager.getUser" should {
    "return social profiles other than fb, hsd, apple, gosuslugi for android app with version<10.12.0" in {
      withContext { ctx =>
        val userRef = PrivateUserRefGen.next
        implicit val request: Request = {
          val r = new RequestImpl
          r.setTrace(Traced.empty)
          r.setRequestParams(RequestParams.construct(ip = "1.1.1.1", androidAppVersion = Some("10.0.0")))
          r.setApplication(Application.androidApp)
          r.setUser(userRef)
          r
        }
        val userResult = UserResult.newBuilder()
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(YANDEX)
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(FACEBOOK)
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(HSD)
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(APPLE)
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(GOSUSLUGI)
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(VK)
        when(ctx.passportClient.getUser(?)(?)).thenReturnF(userResult.build())
        val user = ctx.passportManager.getUser(userRef).futureValue
        user.getUser.getSocialProfilesList.asScala
          .map(_.getProvider) should contain theSameElementsAs Seq(YANDEX, VK)
        Mockito.verify(ctx.passportClient).getUser(?)(?)
      }
    }

    "return social profiles other than fb, hsd, apple, gosuslugi for android app with version=10.12.0" in {
      withContext { ctx =>
        val userRef = PrivateUserRefGen.next
        implicit val request: Request = {
          val r = new RequestImpl
          r.setTrace(Traced.empty)
          r.setRequestParams(RequestParams.construct(ip = "1.1.1.1", androidAppVersion = Some("10.12.0")))
          r.setApplication(Application.androidApp)
          r.setUser(userRef)
          r
        }
        val userResult = UserResult.newBuilder()
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(YANDEX)
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(FACEBOOK)
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(HSD)
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(APPLE)
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(GOSUSLUGI)
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(VK)
        when(ctx.passportClient.getUser(?)(?)).thenReturnF(userResult.build())
        val user = ctx.passportManager.getUser(userRef).futureValue
        user.getUser.getSocialProfilesList.asScala
          .map(_.getProvider) should contain theSameElementsAs Seq(YANDEX, VK)
        Mockito.verify(ctx.passportClient).getUser(?)(?)
      }
    }

    "return social profiles other than fb, hsd, apple, gosuslugi for android app without version" in {
      withContext { ctx =>
        val userRef = PrivateUserRefGen.next
        implicit val request: Request = {
          val r = new RequestImpl
          r.setTrace(Traced.empty)
          r.setRequestParams(RequestParams.construct(ip = "1.1.1.1"))
          r.setApplication(Application.androidApp)
          r.setUser(userRef)
          r
        }
        val userResult = UserResult.newBuilder()
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(YANDEX)
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(FACEBOOK)
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(HSD)
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(APPLE)
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(GOSUSLUGI)
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(VK)
        when(ctx.passportClient.getUser(?)(?)).thenReturnF(userResult.build())
        val user = ctx.passportManager.getUser(userRef).futureValue
        user.getUser.getSocialProfilesList.asScala
          .map(_.getProvider) should contain theSameElementsAs Seq(YANDEX, VK)
        Mockito.verify(ctx.passportClient).getUser(?)(?)
      }
    }

    "return all social profiles for android app with version higher 10.12.0" in {
      withContext { ctx =>
        val userRef = PrivateUserRefGen.next
        implicit val request: Request = {
          val r = new RequestImpl
          r.setTrace(Traced.empty)
          r.setRequestParams(RequestParams.construct(ip = "1.1.1.1", androidAppVersion = Some("10.12.1")))
          r.setApplication(Application.androidApp)
          r.setUser(userRef)
          r
        }
        val userResult = UserResult.newBuilder()
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(YANDEX)
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(FACEBOOK)
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(HSD)
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(APPLE)
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(GOSUSLUGI)
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(VK)
        when(ctx.passportClient.getUser(?)(?)).thenReturnF(userResult.build())
        val user = ctx.passportManager.getUser(userRef).futureValue
        user.getUser.getSocialProfilesList.asScala
          .map(_.getProvider) should contain theSameElementsAs Seq(YANDEX, FACEBOOK, HSD, VK, APPLE, GOSUSLUGI)
        Mockito.verify(ctx.passportClient).getUser(?)(?)
      }
    }

    "return all social profiles for non-android app" in {
      withContext { ctx =>
        val userRef = PrivateUserRefGen.next
        implicit val request: Request = {
          val r = new RequestImpl
          r.setTrace(Traced.empty)
          r.setRequestParams(RequestParams.construct("1.1.1.1"))
          r.setApplication(Application.swagger)
          r.setUser(userRef)
          r
        }
        val userResult = UserResult.newBuilder()
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(YANDEX)
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(FACEBOOK)
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(HSD)
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(APPLE)
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(GOSUSLUGI)
        userResult.getUserBuilder.addSocialProfilesBuilder().setProvider(VK)
        when(ctx.passportClient.getUser(?)(?)).thenReturnF(userResult.build())
        val user = ctx.passportManager.getUser(userRef).futureValue
        user.getUser.getSocialProfilesList.asScala
          .map(_.getProvider) should contain theSameElementsAs Seq(YANDEX, FACEBOOK, HSD, VK, APPLE, GOSUSLUGI)
        Mockito.verify(ctx.passportClient).getUser(?)(?)
      }
    }
  }

  "PassportManager.loginOrAddSocial" should {
    "fail on disabled social networks" in {
      withContext { ctx =>
        implicit val request: Request = ctx.request
        val params = SocialLoginParameters
          .newBuilder()
          .setProvider(SocialProvider.RUGION)
          .build()
        an[IllegalArgumentException] shouldBe thrownBy {
          ctx.passportManager.loginSocial(params).await
        }
      }
    }

    "PasportManager.getClientGroup" should {
      "work" in forAll(PrivateUserRefGen, UserEssentialsGen) { (user, userEssentials) =>
        withContext { ctx =>
          implicit val request: Request = ctx.request
          val essentialsBuilder = userEssentials.toBuilder

          essentialsBuilder.getProfileBuilder
            .setClientGroup("test_group")

          when(ctx.passportClient.getUserEssentials(?, ?)(?)).thenReturnF {
            essentialsBuilder.build()
          }

          ctx.passportManager.getClientGroup(user).futureValue shouldBe Some("test_group")

          Mockito.verify(ctx.passportClient).getUserEssentials(eq(user), eq(false))(?)
        }
      }

      "return empty group" in forAll(PrivateUserRefGen, UserEssentialsGen) { (user, userEssentials) =>
        withContext { ctx =>
          implicit val request: Request = ctx.request
          val essentialsBuilder = userEssentials.toBuilder

          essentialsBuilder.getProfileBuilder
            .clearClientGroup()

          when(ctx.passportClient.getUserEssentials(?, ?)(?)).thenReturnF {
            essentialsBuilder.build()
          }

          ctx.passportManager.getClientGroup(user).futureValue shouldBe None

          Mockito.verify(ctx.passportClient).getUserEssentials(eq(user), eq(false))(?)
        }
      }
    }
  }

}
