package ru.auto.tests.publicapi.session

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_FORBIDDEN
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import org.assertj.core.api.Assertions
import org.hamcrest.MatcherAssert
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200OkJSON, shouldBeCode, validatedWith}
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.utils.UtilsPublicApi.getRandomDeviceId

import scala.annotation.meta.getter


@DisplayName("GET /session")
@RunWith(classOf[GuiceTestRunner])
@GuiceModules(Array(classOf[PublicApiModule]))
class SessionTest {

  private val IGNORE_PATHS = Array("session.expire_timestamp", "session.creation_timestamp", "session.id", "userTicket")

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Inject
  private val am: AccountManager = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Test
  def shouldSee403WhenNoAuth(): Unit = {
    api.session.getSession.execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def shouldSeeAnonymousSessionWhenInvalidSessionId(): Unit = {
    val response = api.session.getSession.reqSpec(defaultSpec)
      .xSessionIdHeader(getRandomString)
      .executeAs(validatedWith(shouldBe200OkJSON))

    Assertions.assertThat(response.getSession.getId).startsWith("a:")
    Assertions.assertThat(response.getSession.getUserId).isNull()
  }

  @Test
  def shouldSeeAuthSessionWithUserTicketRequest(): Unit = {
    val account = am.create()
    val sessionId = adaptor.login(account).getSession.getId
    val userTicket = adaptor.session(sessionId).getUserTicket

    val response = api.session.getSession.reqSpec(defaultSpec)
      .xSessionIdHeader(sessionId)
      .xYaUserTicketVertisHeader(userTicket)
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(response.getSession).hasId(sessionId)
    assertThat(response.getUser).hasId(account.getId)
    assertThat(response).hasTrusted(true)
  }

  @Test
  @DisplayName("Получение сессии для анонимного пользователя")
  def shouldSeeNotAuthSession(): Unit = {
    val deviceId = getRandomDeviceId

    val request = (apiClient: ApiClient) => apiClient.session.getSession
      .reqSpec(defaultSpec)
      .xDeviceUidHeader(deviceId)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    MatcherAssert.assertThat(request(api), jsonEquals[JsonObject](request(prodApi)).whenIgnoringPaths(IGNORE_PATHS: _*))
  }

  @Test
  @DisplayName("Получение сессии для существующего пользователя")
  def shouldSeeAuthSession(): Unit = {
    val account = am.create()
    val loginResult = adaptor.login(account)

    val response = api.session.getSession.reqSpec(defaultSpec)
      .xSessionIdHeader(loginResult.getSession.getId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    assertThat(response.getUser).hasId(account.getId)
    assertThat(response).hasTrusted(true)
  }

  @Test
  def shouldAuthHasNoDiffWithProduction(): Unit = {
    val account = am.create
    val loginResult = adaptor.login(account)
    val deviceId = getRandomDeviceId

    val request = (apiClient: ApiClient) => apiClient.session.getSession
      .reqSpec(defaultSpec)
      .xSessionIdHeader(loginResult.getSession.getId)
      .xDeviceUidHeader(deviceId)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    MatcherAssert.assertThat(request(api), jsonEquals[JsonObject](request(prodApi)).whenIgnoringPaths(IGNORE_PATHS: _*))
  }
}
