package ru.auto.tests.billing_campaign

import java.lang.String.format
import java.util.Random

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.{Issue, Owner}
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_NOT_FOUND}
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.MatcherAssert
import org.junit.{Ignore, Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.constants.Constants.{SALESMAN_USER, SERVICE}
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.module.SalesmanApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.ra.ResponseSpecBuilders.shouldBe400WithMissedSalesmanUser

import scala.annotation.meta.getter

@DisplayName("PUT /service/{service}/billing/campaign/call/client/{clientId}")
@GuiceModules(Array(classOf[SalesmanApiModule]))
@RunWith(classOf[GuiceTestRunner])
class CreateCampaignTest {

  private val CLIENT_ID = 20101
  private val INVALID_CLIENT_ID = 0
  private val INVALID_DAY_LIMIT = -1

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSee400WhenMissedSalesmanUserHeader(): Unit =
    api.billingCampaign.upsertCampaignRoute
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(CLIENT_ID)
      .execute(validatedWith(shouldBe400WithMissedSalesmanUser))

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidService(): Unit =
    api.billingCampaign.upsertCampaignRoute
      .reqSpec(defaultSpec)
      .servicePath(getRandomString)
      .clientIdPath(CLIENT_ID)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))

  @Test
  @Owner(TIMONDL)
  @Ignore
  @Issue("VSMONEY-1171")
  def shouldSee400WithInvalidDailyLimit(): Unit =
    api.billingCampaign.upsertCampaignRoute
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(CLIENT_ID)
      .dayLimitQuery(INVALID_DAY_LIMIT)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))

  @Test
  @Owner(TIMONDL)
  def shouldSeeNewDailyLimit(): Unit = {
    val dayLimit = new Random().nextInt(5000)

    val createResponse = api.billingCampaign.upsertCampaignRoute
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(CLIENT_ID)
      .dayLimitQuery(dayLimit)
      .enabledQuery(true)
      .recalculateCostPerCallQuery(true)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    val fundsInCreateResponse = createResponse
      .get("limits")
      .getAsJsonObject
      .get("comingDaily")
      .getAsJsonObject
      .get("funds")
      .getAsInt
    assertThat(fundsInCreateResponse).isEqualTo(dayLimit)

    val getResponse = api.billingCampaign.getCampaignRoute
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(CLIENT_ID)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    val fundsInGetResponse = getResponse
      .get("limits")
      .getAsJsonObject
      .get("comingDaily")
      .getAsJsonObject
      .get("funds")
      .getAsInt
    assertThat(fundsInGetResponse).isEqualTo(dayLimit)
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidClientId(): Unit = {
    val response = api.billingCampaign.upsertCampaignRoute
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(INVALID_CLIENT_ID)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
      .as(classOf[JsonObject])

    val error = response.get("error").getAsString
    assertThat(error).isEqualTo(
      format("Unable to resolve client %s", INVALID_CLIENT_ID)
    )
  }

  @Test
  @Owner(TIMONDL)
  def shouldCreateCampaignRouteHasNoDiffWithProduction(): Unit = {
    val dayLimit = new Random().nextInt(5000)

    val req = (apiClient: ApiClient) =>
      apiClient.billingCampaign.upsertCampaignRoute
        .reqSpec(defaultSpec)
        .servicePath(SERVICE)
        .clientIdPath(CLIENT_ID)
        .dayLimitQuery(dayLimit)
        .enabledQuery(true)
        .recalculateCostPerCallQuery(true)
        .xSalesmanUserHeader(SALESMAN_USER)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(
      req.apply(api),
      jsonEquals[JsonObject](req.apply(prodApi))
    )
  }
}
