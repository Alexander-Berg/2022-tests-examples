package ru.auto.tests.cashback

import java.lang.String.format

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
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
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.constants.Constants.{SALESMAN_USER, SERVICE}
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.module.SalesmanApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET /service/{service}/cashback/period/{periodId}")
@GuiceModules(Array(classOf[SalesmanApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetCashbackPeriodTest {

  private val PERIOD_ID = 22
  private val INVALID_PERIOD_ID = 1

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
  def shouldSee404WithInvalidPeriodId(): Unit = {
    val response = api.cashback.getCashbackPeriod
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .periodIdPath(INVALID_PERIOD_ID)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
      .as(classOf[JsonObject])

    val error = response.get("error").getAsString
    assertThat(error).isEqualTo(
      format("no period with id %s", INVALID_PERIOD_ID)
    )
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidService(): Unit =
    api.cashback.getCashbackPeriod
      .reqSpec(defaultSpec)
      .servicePath(getRandomString)
      .periodIdPath(PERIOD_ID)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))

  @Test
  @Owner(TIMONDL)
  def shouldGetCashbackPeriodHasNoDiffWithProduction(): Unit = {
    val req = (apiClient: ApiClient) =>
      apiClient.cashback.getCashbackPeriod
        .reqSpec(defaultSpec)
        .servicePath(SERVICE)
        .periodIdPath(PERIOD_ID)
        .xSalesmanUserHeader(SALESMAN_USER)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(
      req.apply(api),
      jsonEquals[JsonObject](req.apply(prodApi))
    )
  }
}
