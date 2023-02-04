package ru.auto.tests.tradein

import java.time.OffsetDateTime

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_NOT_FOUND}
import org.assertj.core.api.Assertions.assertThat
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.{shouldBeCode, validatedWith}
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.constants.Constants.{SALESMAN_USER, SERVICE}
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.module.SalesmanApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.ra.ResponseSpecBuilders.shouldBe400WithMissedSalesmanUser

import scala.annotation.meta.getter

@DisplayName("GET /service/{service}/trade-in/client/{clientId}")
@GuiceModules(Array(classOf[SalesmanApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetTradeInRequestsNegativeTest {

  private val CLIENT_ID = 20101
  private val DATE = OffsetDateTime.now.toLocalDate.toString

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidService(): Unit =
    api.tradeIn.getTradeInRequestsRoute
      .reqSpec(defaultSpec)
      .servicePath(getRandomString)
      .clientIdPath(CLIENT_ID)
      .fromQuery(DATE)
      .pageNumQuery(1)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithMissedFromDate(): Unit = {
    val response = api.tradeIn.getTradeInRequestsRoute
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(CLIENT_ID)
      .pageNumQuery(1)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
      .asString

    assertThat(response).isEqualTo(
      "Request is missing required query parameter 'from'"
    )
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithMissedPageNumber(): Unit = {
    val response = api.tradeIn.getTradeInRequestsRoute
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(CLIENT_ID)
      .fromQuery(DATE)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
      .asString

    assertThat(response).isEqualTo(
      "Request is missing required query parameter 'pageNum'"
    )
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithInvalidFromDate(): Unit = {
    val response = api.tradeIn.getTradeInRequestsRoute
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(CLIENT_ID)
      .fromQuery(getRandomString)
      .pageNumQuery(1)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .asString

    assertThat(response).isEqualTo(
      "The query parameter 'from' was malformed:\nInvalid 'from' date specification"
    )
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WhenMissedSalesmanUserHeader(): Unit =
    api.tradeIn.getTradeInRequestsRoute
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(CLIENT_ID)
      .fromQuery(DATE)
      .pageNumQuery(1)
      .execute(validatedWith(shouldBe400WithMissedSalesmanUser))
}
