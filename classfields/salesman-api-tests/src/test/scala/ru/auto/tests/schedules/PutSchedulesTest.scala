package ru.auto.tests.schedules

import java.lang.String.format

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.assertj.core.api.Assertions.assertThat
import org.junit.{After, Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.validatedWith
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
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
class PutSchedulesTest {

  private val CLIENT_ID = 20101
  private val TIME = "00:00"
  private val PRODUCT = "boost"
  private val TIMEZONE = "+03:00"
  private val OFFER_ID = "1096532688-862ef801"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSeeScheduleInOfferAfterCreate(): Unit = {
    api.schedules.putSchedules
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(CLIENT_ID)
      .productPath(PRODUCT)
      .body(
        new AutoApiBillingSchedulesScheduleRequest()
          .scheduleType(ONCE_AT_TIME)
          .time(TIME)
          .timezone(TIMEZONE)
      )
      .offerIdQuery(OFFER_ID)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBe200OkJSON))

    val response = api.schedules.getSchedules
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(CLIENT_ID)
      .offerIdQuery(OFFER_ID)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    val product = response
      .getAsJsonObject("offers")
      .getAsJsonObject(OFFER_ID)
      .getAsJsonObject("products")
      .getAsJsonObject("all_sale_fresh")
    assertThat(product.get("scheduleType").getAsString)
      .isEqualTo(format("%s", ONCE_AT_TIME))

    val weekdays =
      product.getAsJsonObject("onceAtTime").getAsJsonArray("weekdays")

    // Тут из-за кривой генерации моделек невозможно указать конкретный массив с днями неделями - он всегда пустой
    // и в таком случае заполняется по дефолту полностью всеми днями недели
    assertThat(weekdays.size).isEqualTo(7)
    assertThat(product.getAsJsonObject("onceAtTime").get("time").getAsString)
      .isEqualTo(TIME)
    assertThat(product.get("timezone").getAsString).isEqualTo(TIMEZONE)
  }

  @After
  def deleteSchedule(): Unit =
    api.schedules
      .deleteSchedules()
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(CLIENT_ID)
      .offerIdQuery(OFFER_ID)
      .productQuery(PRODUCT)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBe200OkJSON))
}
