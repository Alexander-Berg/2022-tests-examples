package ru.auto.tests.publicapi.calltracking

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_NOT_FOUND, SC_FORBIDDEN, SC_UNAUTHORIZED}
import org.assertj.core.api.Assertions
import io.qameta.allure.junit4.DisplayName
import org.junit.runner.RunWith
import org.junit.rules.RuleChain
import org.junit.{Test, Rule}
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.{BAD_REQUEST, NO_AUTH, NOT_FOUND}
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.{ERROR, SUCCESS}
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import ru.auto.tests.commons.restassured.ResponseSpecBuilders._
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;
import ru.auto.tests.publicapi.testdata.DealerAccounts.getDemoAccount
import ru.auto.tests.publicapi.model._

import scala.annotation.meta.getter

@DisplayName("PUT /calltracking/redirect/confirm")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class CalltrackingRedirectConfirmTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Test
  def shouldSee403WhenNoAuth(): Unit = {
    api.calltracking.confirmRedirect().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def shouldSee401WithoutSessionId(): Unit = {
    val response = api.calltracking.confirmRedirect()
      .reqSpec(defaultSpec)
      .body(new AutoApiCalltrackingConfirmRedirectRequest())
      .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasStatus(ERROR).hasError(NO_AUTH)
      .hasDetailedError("Expected dealer user. Provide valid session_id")
  }

  @Test
  def shouldSee400WithoutBody(): Unit = {
    val response = api.calltracking.confirmRedirect()
      .reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasStatus(ERROR).hasError(BAD_REQUEST)
    Assertions.assertThat(response.getDetailedError)
      .contains("The request content was malformed:\nExpect message object but got: null")
  }

  @Test
  def shouldReturnSuccessOnExistingRedirect(): Unit = {
    val sessionId = adaptor.login(getDemoAccount).getSession().getId
    
    val requestBody =
      new AutoApiCalltrackingConfirmRedirectRequest()
        .platform("autoru")
        .redirectPhone("+71112223344")

    api.calltracking.confirmRedirect()
      .reqSpec(defaultSpec)
      .body(requestBody)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBeSuccess()))
  }

  @Test
  def shouldReturnErrorOnUnknownRedirect(): Unit = {
    val sessionId = adaptor.login(getDemoAccount).getSession().getId
    val clientId = adaptor.login(getDemoAccount)
      .getUser()
      .getProfile()
      .getAutoru()
      .getClientId()

    val requestBody =
      new AutoApiCalltrackingConfirmRedirectRequest()
        .platform("unknown")
        .redirectPhone("unknown")

    val response = api.calltracking.confirmRedirect()
      .reqSpec(defaultSpec)
      .body(requestBody)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasStatus(ERROR).hasError(NOT_FOUND)
    Assertions.assertThat(response.getDetailedError)
      .contains(s"Redirect unknown:unknown not found for client $clientId")
  }

}
