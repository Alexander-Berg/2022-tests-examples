package ru.auto.tests.publicapi.billing

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_FORBIDDEN}
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat
import org.assertj.core.api.Assertions
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.publicapi.ApiClient
import ru.auto.tests.publicapi.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.publicapi.anno.Prod
import ru.auto.tests.publicapi.consts.Owners.TIMONDL
import ru.auto.tests.publicapi.model.AutoApiErrorResponse
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.BAD_REQUEST
import ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR
import ru.auto.tests.publicapi.module.PublicApiModule
import ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET /billing/subscriptions/{product}/prices")
@GuiceModules(Array(classOf[PublicApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetProductSubscriptionPricesListTest {

  private val DOMAIN = "autoru"
  private val PRODUCT_ALIAS = "offers-history-reports"

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
  def shouldSee403WhenNoAuth(): Unit = {
    api.billingSubscriptions.getPrices
      .domainQuery(getRandomString)
      .productPath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithInvalidProductAlias(): Unit = {
    val invalidProductAlias = getRandomString

    val response = api.billingSubscriptions.getPrices.reqSpec(defaultSpec)
      .domainQuery(DOMAIN)
      .productPath(invalidProductAlias)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasError(BAD_REQUEST).hasStatus(ERROR)
      .hasDetailedError(s"Unknown product ${invalidProductAlias}")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithInvalidDomain(): Unit = {
    val invalidDomain = getRandomString

    val response = api.billingSubscriptions.getPrices.reqSpec(defaultSpec)
      .domainQuery(invalidDomain)
      .productPath(PRODUCT_ALIAS)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .as(classOf[AutoApiErrorResponse])

    assertThat(response).hasError(BAD_REQUEST).hasStatus(ERROR)
    Assertions.assertThat(response.getDetailedError)
      .contains(s"The query parameter 'domain' was malformed:\nWrong name of the enum: ${invalidDomain}")
  }

  @Test
  @Owner(TIMONDL)
  def shouldHasNoDiffWithProduction(): Unit = {
    val req = (apiClient: ApiClient) => apiClient.billingSubscriptions.getPrices
      .reqSpec(defaultSpec)
      .domainQuery(DOMAIN)
      .productPath(PRODUCT_ALIAS)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    MatcherAssert.assertThat(req.apply(api), jsonEquals[JsonObject](req.apply(prodApi)))
  }
}
