package ru.auto.tests.publicapi.parsing

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_FORBIDDEN, SC_NOT_FOUND, SC_UNAUTHORIZED}
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.passport.manager.AccountManager
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.consts.Owners.TIMONDL
import ru.auto.tests.publicapi.model.AutoApiErrorResponse
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.AUTH_ERROR
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("POST /parsing/{hash}/login-or-register-owner")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class LoginOrRegisterOwnerTest {

  private val HASH = "ab1d3b8dfdc5f04a3c09a5b78effc5fb"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val accountManager: AccountManager = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Test
  @Owner(TIMONDL)
  def shouldSee403WhenNoAuth(): Unit = {
    api.parsing.loginOrRegister()
      .hashPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidParsingHash(): Unit = {
    api.parsing.loginOrRegister().reqSpec(defaultSpec)
      .hashPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee401WhenCanPublishIsFalse(): Unit = {
    val account = accountManager.create()
    val sessionId = adaptor.login(account).getSession.getId

    val response = api.parsing.loginOrRegister().reqSpec(defaultSpec)
      .hashPath(HASH)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasStatus(ERROR).hasError(AUTH_ERROR).hasDetailedError("canPublish = false")
  }
}
