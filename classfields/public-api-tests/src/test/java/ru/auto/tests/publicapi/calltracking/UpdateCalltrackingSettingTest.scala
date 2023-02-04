package ru.auto.tests.publicapi.calltracking

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN, SC_UNAUTHORIZED}
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import org.assertj.core.api.Assertions
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.consts.Owners.TIMONDL
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.{BAD_REQUEST, NO_AUTH}
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR
import ru.auto.tests.publicapi.model.{AutoApiCalltrackingSaveSettingsRequest, AutoApiErrorResponse, AutoCalltrackingSeconds, AutoCalltrackingSettings}
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.testdata.DealerAccounts.getDemoAccount

import scala.annotation.meta.getter
import scala.util.Random

@DisplayName("PUT /calltracking/settings")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class UpdateCalltrackingSettingTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Test
  @Owner(TIMONDL)
  def shouldSee403WhenNoAuth(): Unit = {
    api.calltracking.saveSettings().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee401WithoutSessionId(): Unit = {
    val response = api.calltracking.saveSettings().reqSpec(defaultSpec)
      .body(new AutoApiCalltrackingSaveSettingsRequest)
      .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasStatus(ERROR).hasError(NO_AUTH)
      .hasDetailedError("Expected dealer user. Provide valid session_id")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithoutBody(): Unit = {
    val response = api.calltracking.saveSettings().reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasStatus(ERROR).hasError(BAD_REQUEST)
    Assertions.assertThat(response.getDetailedError)
      .contains("The request content was malformed:\nExpect message object but got: null")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSuccessUpdateSettings(): Unit = {
    val sessionId = adaptor.login(getDemoAccount).getSession.getId
    val seconds = Random.between(30, 60)
    val settings = new AutoCalltrackingSettings().calltrackingEnabled(true).calltrackingClassifiedsEnabled(true)
      .offersStatEnabled(true).targetCallDuration(new AutoCalltrackingSeconds().seconds(seconds))

    api.calltracking.saveSettings().reqSpec(defaultSpec)
      .body(new AutoApiCalltrackingSaveSettingsRequest().settings(settings))
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))

    val calltrackingSettings = adaptor.getCalltrackingSettings(sessionId)
    assertThat(calltrackingSettings.getSettings)
      .hasCalltrackingEnabled(true)
      .hasCalltrackingClassifiedsEnabled(true)
      .hasOffersStatEnabled(true)
    assertThat(calltrackingSettings.getSettings.getTargetCallDuration).hasSeconds(seconds)
  }
}
