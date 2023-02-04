package ru.auto.tests.cashback

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

@DisplayName("GET /service/{service}/cashback/amount")
@GuiceModules(Array(classOf[SalesmanApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetCashbackTest {

  private val CLIENT_ID = 20101
  private val PERIOD_ID = 22
  private val INVALID_PERIOD_ID = 1
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
    api.cashback.getCashback
      .reqSpec(defaultSpec)
      .servicePath(getRandomString)
      .clientIdQuery(CLIENT_ID)
      .periodIdQuery(PERIOD_ID)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))

  @Test
  @Owner(TIMONDL)
  def shouldSeeZerosWithNotRecordedPeriodId(): Unit = {
    val response = api.cashback.getCashback
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdQuery(CLIENT_ID)
      .periodIdQuery(INVALID_PERIOD_ID)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBe200OkJSON()))
      .as(classOf[JsonObject])

    val amount = response.get("amount").getAsInt
    val percent = response.get("percent").getAsInt
    assertThat(amount).isEqualTo(0)
    assertThat(percent).isEqualTo(0)
  }

  @Test
  @Owner(TIMONDL)
  def shouldSeeZerosWithNotRecordedClientId(): Unit = {
    val response = api.cashback.getCashback
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdQuery(INVALID_CLIENT_ID)
      .periodIdQuery(PERIOD_ID)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBe200OkJSON()))
      .as(classOf[JsonObject])

    val amount = response.get("amount").getAsInt
    val percent = response.get("percent").getAsInt
    assertThat(amount).isEqualTo(0)
    assertThat(percent).isEqualTo(0)
  }

  @Test
  @Owner(TIMONDL)
  def shouldGetCashbackHasNoDiffWithProduction(): Unit = {
    val req = (apiClient: ApiClient) =>
      apiClient.cashback.getCashback
        .reqSpec(defaultSpec)
        .servicePath(SERVICE)
        .clientIdQuery(CLIENT_ID)
        .periodIdQuery(PERIOD_ID)
        .xSalesmanUserHeader(SALESMAN_USER)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(
      req.apply(api),
      jsonEquals[JsonObject](req.apply(prodApi))
    )
  }
}
