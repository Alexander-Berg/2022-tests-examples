package ru.auto.tests.ads

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_NOT_FOUND
import org.assertj.core.api.Assertions.assertThat
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.validatedWith
import ru.auto.tests.adaptor.SalesmanApiAdaptor
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.constants.Constants.{SALESMAN_USER, SERVICE}
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.module.SalesmanApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.ra.ResponseSpecBuilders.shouldBe200WithMessageOK

import scala.annotation.meta.getter

@DisplayName(
  "DELETE /service/{service}/ads/request/{request}/client/{clientId}"
)
@GuiceModules(Array(classOf[SalesmanApiModule]))
@RunWith(classOf[GuiceTestRunner])
class DeleteAdsRequestTest {

  private val CLIENT_ID = 20101
  private val REQUEST_TYPE = "cars:used"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  private val adaptor: SalesmanApiAdaptor = null

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidService(): Unit =
    api.ads
      .deleteAdsRequestRoute()
      .reqSpec(defaultSpec)
      .servicePath(getRandomString)
      .requestPath(REQUEST_TYPE)
      .clientIdPath(CLIENT_ID)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))

  @Test
  @Owner(TIMONDL)
  def shouldSee404WithInvalidRequestType(): Unit =
    api.ads
      .deleteAdsRequestRoute()
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .requestPath(getRandomString)
      .clientIdPath(CLIENT_ID)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))

  @Test
  @Owner(TIMONDL)
  def shouldSee200DeleteAdsRequest(): Unit = {
    adaptor.createAdsRequest(CLIENT_ID, REQUEST_TYPE)

    api.ads
      .deleteAdsRequestRoute()
      .reqSpec(defaultSpec)
      .servicePath(SERVICE)
      .clientIdPath(CLIENT_ID)
      .requestPath(REQUEST_TYPE)
      .xSalesmanUserHeader(SALESMAN_USER)
      .execute(validatedWith(shouldBe200WithMessageOK))

    val response = adaptor.getAdsRequest(CLIENT_ID, REQUEST_TYPE).asString

    assertThat(response).isEmpty()
  }
}
