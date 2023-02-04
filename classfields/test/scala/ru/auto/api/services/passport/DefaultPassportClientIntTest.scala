package ru.auto.api.services.passport

import org.scalatest.Inspectors
import ru.auto.api.auth.Application
import ru.auto.api.exceptions._
import ru.auto.api.http.HttpClientConfig
import ru.auto.api.managers.TestRequest
import ru.auto.api.model.{AutoruUser, ModelGenerators, SessionID}
import ru.auto.api.services.HttpClientSuite
import ru.yandex.passport.model.api.ApiModel._
import ru.yandex.vertis.SocialProvider

import scala.jdk.CollectionConverters.ListHasAsScala
import scala.util.{Failure, Success}

/**
  *
  * @author zvez
  */
class DefaultPassportClientIntTest extends HttpClientSuite with TestRequest with Inspectors {

  override protected def config: HttpClientConfig =
    HttpClientConfig("http", "passport-api-server.vrts-slb.test.vertis.yandex.net", 80)

  private val passportClient = new DefaultPassportClient(http)

  val testUserEmail = "test@auto.ru"
  val testUserPassword = "autoru"

  val loginParams = LoginParameters
    .newBuilder()
    .setLogin(testUserEmail)
    .setPassword(testUserPassword)
    .build()

  private val testUser = AutoruUser(3050391L) //test@auto.ru

  test("login") {
    val res = passportClient.login(loginParams).futureValue
    res.hasUser shouldBe true
    res.getUser.getEmailsList.asScala.exists(_.getEmail == testUserEmail) shouldBe true
  }

  test("login (failed)") {
    val loginParams = LoginParameters
      .newBuilder()
      .setLogin("something@gmail.com")
      .setPassword("wrong")
      .build()

    passportClient.login(loginParams).failed.futureValue shouldBe an[AuthenticationException]
  }

  test("login (not allowed for client from mobile apps)") {
    val email = "ak@ubps.ru" // User 4813
    val clientId = "27648" // for User 4813
    val loginParams = LoginParameters
      .newBuilder()
      .setLogin(email)
      .setPassword("autoru")
      .build()

    def originalRequest(app: Application) =
      PassportRequestInfo
        .fromRequest(request)
        .copy(application = app)

    forAll(Seq(Application.iosApp, Application.androidApp)) { app =>
      passportClient
        .login(loginParams)(originalRequest(app))
        .failed
        .futureValue shouldBe an[ClientLoginNotAllowedException]
    }

    forAll(Seq(Application.web, Application.mobile, Application.desktop)) { app =>
      val loginResult = passportClient.login(loginParams)(originalRequest(app)).futureValue
      loginResult.getUserEssentials.getClientId shouldBe clientId
    }
  }

  test("get social provider's auth uri") {
    pending
    val res = passportClient
      .getSocialProviderAuthUri(SocialProvider.FACEBOOK, Platform.DESKTOP, None, None)
      .futureValue
    res.getUri should not be empty
  }

  test("get non-existent session") {
    val call = passportClient.getSession(SessionID("1234.obviously-fake-sid"))
    call.failed.futureValue shouldBe a[SessionNotFoundException]
  }

  test("get session by malformed sid") {
    val call = passportClient.getSession(
      SessionID("26301062|1527703024360.604800.rFRGtbpyjC88re7iA5xtJw.y0eQVbjueHGfHYGtaNOeO8it1u6VpZh02TW6C7Ot9_U")
    )
    call.failed.futureValue shouldBe a[SessionNotFoundException]
  }

  test("get session") {
    val loginResult = passportClient.login(loginParams).futureValue
    val sessionId = loginResult.getSession.getId
    val passportUserId = loginResult.getUser.getId

    val res = passportClient.getSession(SessionID(sessionId)).futureValue
    res.getSession.getId shouldBe sessionId
    res.getSession.getUserId shouldBe passportUserId
  }

  test("get non-existent user") {
    val call = passportClient.getUser(AutoruUser(666424242L))
    call.failed.futureValue shouldBe an[UserNotFoundException]
  }

  test("get user") {
    val res = passportClient.getUser(AutoruUser(2L)).futureValue
    res.hasUser shouldBe true
    res.getUser.getId should not be empty
    res.getUser.getProfile.getProfileCase shouldBe UserProfile.ProfileCase.AUTORU
  }

  test("get user essentials") {
    val userId = 2L
    val res = passportClient.getUserEssentials(AutoruUser(userId), withLastSeen = false).futureValue
    res.getId shouldBe userId.toString
    res.hasProfile shouldBe true
  }

  test("add phone") {
    val userId = testUser
    val params =
      AddPhoneParameters
        .newBuilder()
        .setPhone("79052572910")
        .setSteal(true)
        .setSuppressNotifications(true)
        .build()
    val f = passportClient.addPhone(userId, params).transform {
      case Success(_) => Success(())
      case Failure(_: TooManyConfirmationRequestsException) => Success(())
      case Failure(other) => fail(s"Unexpected exception from client $other")
    }
    f.futureValue
  }

  test("confirm phone") {
    val params = ConfirmIdentityParameters
      .newBuilder()
      .setPhone("+79052572910")
      .setCode("something wrong")
      .setCreateSession(true)
      .build()
    passportClient.confirmIdentity(params).failed.futureValue should matchPattern {
      case _: ConfirmationCodeNotFoundException =>
      case _: TooManyFailedConfirmationAttemptsException =>
    }
  }

  test("get device uid") {
    val res = passportClient.getDeviceUid().futureValue
    res.getDeviceUid.isEmpty shouldBe false
  }

  test("delete session") {
    val email = "test@auto.ru"

    val loginParams = LoginParameters
      .newBuilder()
      .setLogin(email)
      .setPassword("autoru")
      .build()

    val res = passportClient.login(loginParams).futureValue
    val sid = SessionID(res.getSession.getId)
    passportClient.getSession(sid).futureValue

    passportClient.deleteSession(sid).futureValue

    passportClient.getSession(sid).failed.futureValue shouldBe a[SessionNotFoundException]
  }

  test("remove social profile") {
    val params = ModelGenerators.PassportRemoveSocialProfileParamsGen.next
    passportClient.removeSocialProfile(testUser, params, removeLastIdentity = false).futureValue
  }

  test("get userpic upload uri") {
    passportClient.getUserpicUploadUri(testUser).futureValue should not be empty
  }

  test("get user's profile") {
    val res = passportClient.getUserProfile(testUser).futureValue
    res.getAlias should not be empty
  }

  test("forget user") {
    // This is not idempotent - forgetting the user a second time produces an error. As it stands, we can't run or
    // capture this test without manual preparation.
    pending
    val call = passportClient.forgetUser(AutoruUser(71864908L))
    call.futureValue
  }

  test("forget non-existent user") {
    val call = passportClient.forgetUser(AutoruUser(666424242L))
    call.failed.futureValue shouldBe an[UserNotFoundException]
  }
}
