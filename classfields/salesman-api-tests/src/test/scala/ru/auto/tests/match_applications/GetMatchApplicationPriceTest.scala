package ru.auto.tests.match_applications

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_NOT_FOUND}
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.MatcherAssert
import org.junit.{Rule, Test}
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

import scala.annotation.meta.getter

@DisplayName(
  "GET /service/{service}/match-applications/client/{clientId}/price"
)
@GuiceModules(Array(classOf[SalesmanApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetMatchApplicationPriceTest {

  private val CLIENT_ID = 20101
  private val PRODUCT = "match-application:cars:new"
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
  def shouldSee404WithInvalidService(): Unit =
    api.matchApplications.getMatchApplicationPrice
      .reqSpec(defaultSpec)
      .servicePath(getRandomString)
      .clientIdPath(CLIENT_ID)
      .productQuery(PRODUCT)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithInvalidProduct(): Unit = {
    val invalidProduct = getRandomString

    val response = api.matchApplications.getMatchApplicationPrice
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(CLIENT_ID)
      .productQuery(invalidProduct)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .asString

    assertThat(response).isEqualTo(
      s"The query parameter 'product' was malformed:\nInvalid product type $invalidProduct"
    )
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidClientId(): Unit = {
    val response = api.matchApplications.getMatchApplicationPrice
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(INVALID_CLIENT_ID)
      .productQuery(PRODUCT)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
      .as(classOf[JsonObject])

    val error = response.get("error").getAsString
    assertThat(error).isEqualTo(s"Unable to resolve client $INVALID_CLIENT_ID")
  }

  @Test
  @Owner(TIMONDL)
  def shouldGetMatchApplicationPriceHasNoDiffWithProduction(): Unit = {
    val req = (apiClient: ApiClient) =>
      apiClient.matchApplications.getMatchApplicationPrice
        .reqSpec(defaultSpec)
        .servicePath(SERVICE)
        .clientIdPath(CLIENT_ID)
        .productQuery(PRODUCT)
        .xSalesmanUserHeader(SALESMAN_USER)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(
      req.apply(api),
      jsonEquals[JsonObject](req.apply(prodApi))
    )
  }
}
