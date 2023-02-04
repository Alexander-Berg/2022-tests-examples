package ru.auto.tests.schedules

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
import ru.auto.tests.model.AutoApiBillingSchedulesScheduleRequest
import ru.auto.tests.model.AutoApiBillingSchedulesScheduleRequest.ScheduleTypeEnum.ONCE_AT_TIME
import ru.auto.tests.module.SalesmanApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName(
  "PUT /service/{service}/schedules/client/{clientId}/product/{product}"
)
@GuiceModules(Array(classOf[SalesmanApiModule]))
@RunWith(classOf[GuiceTestRunner])
class PutSchedulesNegativeTest {

  private val CLIENT_ID = 20101
  private val TIME = "00:00"
  private val PRODUCT = "boost"
  private val TIMEZONE = "+03:00"
  private val OFFER_ID = "1096532688-862ef801"

  private val SCHEDULE_REQUEST = new AutoApiBillingSchedulesScheduleRequest()
    .scheduleType(ONCE_AT_TIME)
    .time(TIME)
    .timezone(TIMEZONE)

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithoutBody(): Unit = {
    val response = api.schedules.putSchedules
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(CLIENT_ID)
      .productPath(PRODUCT)
      .offerIdQuery(OFFER_ID)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .asString

    assertThat(response).isEqualTo(
      "The request content was malformed:\nExpect message object but got: null"
    )
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidService(): Unit =
    api.schedules.putSchedules
      .reqSpec(defaultSpec)
      .servicePath(getRandomString)
      .clientIdPath(CLIENT_ID)
      .productPath(PRODUCT)
      .offerIdQuery(OFFER_ID)
      .body(SCHEDULE_REQUEST)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidProduct(): Unit = {
    val invalidProduct = getRandomString
    val response = api.schedules.putSchedules
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(CLIENT_ID)
      .productPath(invalidProduct)
      .offerIdQuery(OFFER_ID)
      .body(SCHEDULE_REQUEST)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
      .asString

    assertThat(response).isEqualTo(
      s""""Unknown product name [$invalidProduct] for domain [autoru]""""
    )
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WithInvalidOfferId(): Unit = {
    val response = api.schedules.putSchedules
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(CLIENT_ID)
      .productPath(PRODUCT)
      .offerIdQuery(getRandomString)
      .body(SCHEDULE_REQUEST)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .asString

    assertThat(response).isEqualTo(
      "The query parameter 'offerId' was malformed:\nCannot parse offerId param"
    )
  }
}
