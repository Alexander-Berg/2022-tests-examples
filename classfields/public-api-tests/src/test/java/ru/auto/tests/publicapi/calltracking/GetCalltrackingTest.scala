package ru.auto.tests.publicapi.calltracking

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN, SC_UNAUTHORIZED}
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import org.assertj.core.api.Assertions
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.consts.Owners.TIMONDL
import ru.auto.tests.publicapi.model.{AutoApiCalltrackingCallListingRequest, AutoApiErrorResponse}
import ru.auto.tests.publicapi.model.AutoCommonRequestPagination
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.{BAD_REQUEST, NO_AUTH}
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.testdata.DealerAccounts.getDemoAccount

import scala.annotation.meta.getter

@DisplayName("POST /calltracking")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetCalltrackingTest {

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Test
  @Owner(TIMONDL)
  def shouldSee403WhenNoAuth(): Unit = {
    api.calltracking.getCallsCalltracking.execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee401WithoutSessionId(): Unit = {
    val response = api.calltracking.getCallsCalltracking.reqSpec(defaultSpec)
      .body(new AutoApiCalltrackingCallListingRequest)
      .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasStatus(ERROR).hasError(NO_AUTH)
      .hasDetailedError("Expected dealer user. Provide valid session_id")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithoutBody(): Unit = {
    val response = api.calltracking.getCallsCalltracking.reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasStatus(ERROR).hasError(BAD_REQUEST)
    Assertions.assertThat(response.getDetailedError)
      .contains("The request content was malformed:\nExpect message object but got: null")
  }

  @Test
  @Owner(TIMONDL)
  def shouldHasNoDiffWithProduction(): Unit = {
    val sessionId = adaptor.login(getDemoAccount).getSession.getId
    val requestBody = new AutoApiCalltrackingCallListingRequest()
      .pagination(new AutoCommonRequestPagination().page(1).pageSize(20))

    val request = (apiClient: ApiClient) => apiClient.calltracking.getCallsCalltracking
      .reqSpec(defaultSpec)
      .body(requestBody)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    MatcherAssert.assertThat(request(api), jsonEquals[JsonObject](request(prodApi)))
  }
}
