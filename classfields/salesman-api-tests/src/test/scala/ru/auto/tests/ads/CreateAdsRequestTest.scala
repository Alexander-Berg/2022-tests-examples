package ru.auto.tests.ads

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
import ru.auto.tests.module.SalesmanApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.ra.ResponseSpecBuilders.shouldBe200WithMessageOK

import scala.annotation.meta.getter

@DisplayName("POST /service/{service}/ads/request/{request}/client/{clientId}")
@RunWith(classOf[GuiceTestRunner])
@GuiceModules(Array(classOf[SalesmanApiModule]))
class CreateAdsRequestTest {

  private val CLIENT_ID = 20101
  private val REQUEST_TYPE = "cars:used"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Test
  @Owner(TIMONDL)
  def shouldSee200InsertAdsRequestRoute(): Unit = {
    api.ads
      .insertAdsRequestRoute()
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .requestPath(REQUEST_TYPE)
      .clientIdPath(CLIENT_ID)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBe200WithMessageOK))

    val response = api.ads.getAdsRequestRoute
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .requestPath(REQUEST_TYPE)
      .clientIdPath(CLIENT_ID)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBe200OkJSON))
      .as(classOf[JsonObject])

    val clientId = response.get("clientId").getAsInt
    val requestType = response.get("requestType").getAsString

    assertThat(clientId).isEqualTo(CLIENT_ID)
    assertThat(requestType).isEqualTo(REQUEST_TYPE)
  }

  @After
  def after(): Unit =
    api.ads
      .deleteAdsRequestRoute()
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .requestPath(REQUEST_TYPE)
      .clientIdPath(CLIENT_ID)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBe200WithMessageOK))
}
