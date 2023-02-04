package ru.auto.tests.campaign

import java.lang.String.format

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonArray
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_NOT_FOUND
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.MatcherAssert
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.constants.Constants.{SALESMAN_USER, SERVICE}
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.module.SalesmanApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.ra.ResponseSpecBuilders.shouldBe400WithMissedSalesmanUser

import scala.annotation.meta.getter

@DisplayName("GET /service/{service}/campaign/client/{clientId}")
@GuiceModules(Array(classOf[SalesmanApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetCampaignTest {

  private val CLIENT_ID = 20101
  private val INVALID_CLIENT_ID = 0

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
    api.campaign.getCampaignRoute
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(CLIENT_ID)
      .execute(validatedWith(shouldBe400WithMissedSalesmanUser))

  @Test
  @Owner(TIMONDL)
  def shouldSee404WhenInvalidClientId(): Unit = {
    val response = api.campaign.getCampaignRoute
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(INVALID_CLIENT_ID)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
      .asString

    assertThat(response).isEqualTo(
      format("\"Client %s not found\"", INVALID_CLIENT_ID)
    )
  }

  @Test
  @Owner(TIMONDL)
  def shouldGetCampaignHasNoDiffWithProduction(): Unit = {
    val req = (apiClient: ApiClient) =>
      apiClient.campaign.getCampaignRoute
        .reqSpec(defaultSpec)
        .clientIdPath(CLIENT_ID)
        .servicePath(SERVICE)
        .xSalesmanUserHeader(SALESMAN_USER)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonArray])

    MatcherAssert.assertThat(
      req.apply(api),
      jsonEquals[JsonArray](req.apply(prodApi))
    )
  }
}
