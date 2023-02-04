package ru.auto.tests.product

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
import ru.auto.tests.ResponseSpecBuilders.validatedWith
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{
  shouldBe200OkJSON,
  shouldBeCode
}
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.constants.Constants.{SALESMAN_USER, SERVICE}
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.module.SalesmanApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET /service/{service}/product/vin-history/client/{clientId}")
@GuiceModules(Array(classOf[SalesmanApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetVinHistoryPaymentStatusTest {

  private val CLIENT_ID = 20101
  private val PAID_VIN = "BATM0BILEXXX00777"
  private val NOT_PAID_VIN = "SALWA2FK7HA135034"

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
  def shouldSeePaidStatus(): Unit = {
    val response = api.product.getVinHistoryPaymentStatus
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(CLIENT_ID)
      .vinQuery(PAID_VIN)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    val paymentStatus = response.get("paymentStatus").getAsString
    assertThat(paymentStatus).isEqualTo("PAID")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSeeNotPaidStatus(): Unit = {
    val response = api.product.getVinHistoryPaymentStatus
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(CLIENT_ID)
      .vinQuery(NOT_PAID_VIN)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    val paymentStatus = response.get("paymentStatus").getAsString
    assertThat(paymentStatus).isEqualTo("NOT_PAID")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidService(): Unit =
    api.product.getVinHistoryPaymentStatus
      .reqSpec(defaultSpec)
      .servicePath(getRandomString)
      .clientIdPath(CLIENT_ID)
      .vinQuery(NOT_PAID_VIN)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))

  @Test
  @Owner(TIMONDL)
  def shouldGetVinHistoryPaymentStatusHasNoDiffWithProduction(): Unit = {
    val req = (apiClient: ApiClient) =>
      apiClient.product.getVinHistoryPaymentStatus
        .reqSpec(defaultSpec)
        .servicePath(SERVICE)
        .clientIdPath(CLIENT_ID)
        .vinQuery(NOT_PAID_VIN)
        .xSalesmanUserHeader(SALESMAN_USER)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(
      req.apply(api),
      jsonEquals[JsonObject](req.apply(prodApi))
    )
  }
}
