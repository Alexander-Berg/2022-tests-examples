package ru.auto.tests.publicapi.dealer

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
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
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor
import ru.auto.tests.publicapi.consts.Owners.TIMONDL
import ru.auto.tests.publicapi.model.AutoApiErrorResponse
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.{BAD_REQUEST, NO_AUTH}
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.publicapi.testdata.DealerAccounts.getDemoAccount

import scala.annotation.meta.getter

@DisplayName("GET /dealer/campaign/product/{product}")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetExpensesReportTest {

  private val DATE_FROM = "2017-01-01"
  private val DATE_TO = "2018-01-01"

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
    api.dealer.offersExpenses.execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee401WithoutSessionId(): Unit = {
    val response = api.dealer.offersExpenses.reqSpec(defaultSpec)
      .fromDateQuery(DATE_FROM)
      .toDateQuery(DATE_TO)
      .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasStatus(ERROR).hasError(NO_AUTH)
      .hasDetailedError("Expected dealer user. Provide valid session_id")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithoutFromDate(): Unit = {
    val response = api.dealer.offersExpenses.reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasStatus(ERROR).hasError(BAD_REQUEST)
    Assertions.assertThat(response.getDetailedError).contains("Request is missing required query parameter 'from_date'")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithoutToDate(): Unit = {
    val response = api.dealer.offersExpenses.reqSpec(defaultSpec)
      .fromDateQuery(DATE_FROM)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasStatus(ERROR).hasError(BAD_REQUEST)
    Assertions.assertThat(response.getDetailedError).contains("Request is missing required query parameter 'to_date'")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithInvalidFromDate(): Unit = {
    val response = api.dealer.offersExpenses.reqSpec(defaultSpec)
      .fromDateQuery(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasStatus(ERROR).hasError(BAD_REQUEST)
    Assertions.assertThat(response.getDetailedError).contains("The query parameter 'from_date' was malformed")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithInvalidToDate(): Unit = {
    val response = api.dealer.offersExpenses.reqSpec(defaultSpec)
      .fromDateQuery(DATE_FROM)
      .toDateQuery(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasStatus(ERROR).hasError(BAD_REQUEST)
    Assertions.assertThat(response.getDetailedError).contains("The query parameter 'to_date' was malformed")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSeeReadyToDownload(): Unit = {
    val sessionId = adaptor.login(getDemoAccount).getSession.getId

    val response = api.dealer.offersExpenses.reqSpec(defaultSpec)
      .collectorTimeoutSecondsQuery(5)
      .fromDateQuery(DATE_FROM)
      .toDateQuery(DATE_TO)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    Assertions.assertThat(response.get("process_status").getAsString).isEqualTo("READY_TO_BE_DOWNLOADED")
    Assertions.assertThat(response.get("report_download_link").getAsString).isNotBlank
  }

  @Test
  @Owner(TIMONDL)
  def shouldSeeSentToEmail(): Unit = {
    val sessionId = adaptor.login(getDemoAccount).getSession.getId

    val response = api.dealer.offersExpenses.reqSpec(defaultSpec)
      .collectorTimeoutSecondsQuery(0)
      .fromDateQuery(DATE_FROM)
      .toDateQuery(DATE_TO)
      .xSessionIdHeader(sessionId)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    Assertions.assertThat(response.get("process_status").getAsString).isEqualTo("WILL_BE_SENT_TO_EMAIL")
  }
}
