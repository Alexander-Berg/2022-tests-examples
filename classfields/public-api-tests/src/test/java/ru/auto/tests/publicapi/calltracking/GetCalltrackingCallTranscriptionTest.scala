package ru.auto.tests.publicapi.calltracking

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN, SC_UNAUTHORIZED}
import org.assertj.core.api.Assertions
import io.qameta.allure.junit4.DisplayName
import org.junit.runner.RunWith
import org.junit.rules.RuleChain
import org.junit.{Test, Rule}
import org.hamcrest.MatcherAssert
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.{BAD_REQUEST, NO_AUTH}
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import ru.auto.tests.commons.restassured.ResponseSpecBuilders._
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.testdata.DealerAccounts.getDemoAccount
import ru.auto.tests.publicapi.model._

import scala.annotation.meta.getter

@DisplayName("POST /calltracking/call/transcription")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetCalltrackingCallTranscriptionTest {

  private val callId: Long = 1197183

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val adaptor: PublicApiAdaptor = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Test
  def shouldSee403WhenNoAuth(): Unit = {
    api.calltracking.getTranscription.execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  def shouldSee401WithoutSessionId(): Unit = {
    val response = api.calltracking.getTranscription
      .reqSpec(defaultSpec)
      .body(new AutoApiCalltrackingCallTranscriptionRequest())
      .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasStatus(ERROR).hasError(NO_AUTH)
      .hasDetailedError("Expected dealer user. Provide valid session_id")
  }

  @Test
  def shouldSee400WithoutBody(): Unit = {
    val response = api.calltracking.getTranscription()
      .reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasStatus(ERROR).hasError(BAD_REQUEST)
    Assertions.assertThat(response.getDetailedError)
      .contains("The request content was malformed:\nExpect message object but got: null")
  }

  @Test
  def shouldReturnDialogOf35Phrases(): Unit = {
    val sessionId = adaptor.login(getDemoAccount).getSession().getId

    val requestBody =
      new AutoApiCalltrackingCallTranscriptionRequest()
        .callId(callId)

    val response = api.calltracking.getTranscription()
      .reqSpec(defaultSpec)
      .body(requestBody)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    Assertions.assertThat(response.getTranscription().getPhrases().size()).isEqualTo(35)
  }

  @Test
  def shouldReturnHighlightedPhrases(): Unit = {
    val sessionId = adaptor.login(getDemoAccount).getSession().getId

    val requestBody =
      new AutoApiCalltrackingCallTranscriptionRequest()
        .callId(callId)
        .textFilter {
          new AutoCalltrackingFullTextFilter()
            .websearchQuery("информационный")
        }

    val response = api.calltracking.getTranscription()
      .reqSpec(defaultSpec)
      .body(requestBody)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    val highlightedPhrases = response.getTranscription().getPhrases()
      .stream()
      .filter(_.getText().contains("<b>информационный</b>"))
      .toArray()

    Assertions.assertThat(highlightedPhrases.length).isEqualTo(1)
  }

  @Test
  def shouldReturnNoHighlightedPhrasesForWrongSpeaker(): Unit = {
    val sessionId = adaptor.login(getDemoAccount).getSession().getId

    val requestBody =
      new AutoApiCalltrackingCallTranscriptionRequest()
        .callId(callId)
        .textFilter {
          new AutoCalltrackingFullTextFilter()
            .domain(AutoCalltrackingFullTextFilter.DomainEnum.TARGET)
            .websearchQuery("информационный")
        }

    val response = api.calltracking.getTranscription()
      .reqSpec(defaultSpec)
      .body(requestBody)
      .xSessionIdHeader(sessionId)
      .executeAs(validatedWith(shouldBe200OkJSON))

    val highlightedPhrases = response.getTranscription().getPhrases()
      .stream()
      .filter(_.getText().contains("<b>информационный</b>"))
      .toArray()

    Assertions.assertThat(highlightedPhrases.length).isZero()
  }

  @Test
  def shouldHasNoDiffWithProduction(): Unit = {
    val sessionId = adaptor.login(getDemoAccount).getSession().getId

    def request(apiClient: ApiClient) =
      apiClient.calltracking.getTranscription()
        .reqSpec(defaultSpec)
        .body(new AutoApiCalltrackingCallTranscriptionRequest().callId(callId))
        .xSessionIdHeader(sessionId)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(request(api), jsonEquals[JsonObject](request(prodApi)))
  }

}
