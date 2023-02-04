package ru.auto.tests.product

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_NOT_FOUND, SC_PAYMENT_REQUIRED}
import org.assertj.core.api.Assertions.assertThat
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.constants.Constants.{SALESMAN_USER, SERVICE}
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.module.SalesmanApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.ra.ResponseSpecBuilders.shouldBe400WithMissedSalesmanUser

import scala.annotation.meta.getter

@DisplayName("POST /service/{service}/product/vin-history/client/{clientId}")
@RunWith(classOf[GuiceTestRunner])
@GuiceModules(Array(classOf[SalesmanApiModule]))
class ActivateVinHistoryTest {

  private val CLIENT_ID = 20101
  private val INVALID_CLIENT_ID = 0
  private val CLIENT_ID_WITHOUT_MONEY = 98
  private val VIN = "SALWA2FK7HA135034"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSeePaymentStatusAlreadyPaid(): Unit = {
    val response = api.product.activateVinHistoryRequest
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(CLIENT_ID)
      .vinQuery(VIN)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    val paymentStatus = response.get("paymentStatus").getAsString
    assertThat(paymentStatus).isEqualTo("ALREADY_PAID")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSeePaymentStatusOk(): Unit = {
    val response = api.product.activateVinHistoryRequest
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(CLIENT_ID)
      .vinQuery(getRandomString)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    val paymentStatus = response.get("paymentStatus").getAsString
    assertThat(paymentStatus).isEqualTo("OK")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidService(): Unit =
    api.product.activateVinHistoryRequest
      .reqSpec(defaultSpec)
      .servicePath(getRandomString)
      .clientIdPath(CLIENT_ID)
      .vinQuery(VIN)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))

  @Test
  @Owner(TIMONDL)
  def shouldBe400WithoutSalesmanUserHeader(): Unit =
    api.product.activateVinHistoryRequest
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(CLIENT_ID)
      .vinQuery(VIN)
      .execute(validatedWith(shouldBe400WithMissedSalesmanUser))

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidClientId(): Unit = {
    val response = api.product.activateVinHistoryRequest
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(INVALID_CLIENT_ID)
      .vinQuery(VIN)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
      .asString

    assertThat(response).isEqualTo(s""""Client $INVALID_CLIENT_ID not found"""")
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee402ActivateVinHistoryRequest(): Unit =
    api.product.activateVinHistoryRequest
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(CLIENT_ID_WITHOUT_MONEY)
      .vinQuery(VIN)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_PAYMENT_REQUIRED)))
}
