package ru.auto.tests.offers

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
import ru.auto.tests.constants.Constants.SERVICE
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.module.SalesmanApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET /service/{service}/offers/client/{clientId}")
@GuiceModules(Array(classOf[SalesmanApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetOffersWithBilledServicesTest {

  private val CLIENT_ID = 20101
  private val DATE_FROM = "2019-01-01"
  private val DATE_TO = "2019-02-20"

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
  def shouldSee404WhenInvalidService(): Unit =
    api.offers.getOffersWithBilledServices
      .reqSpec(defaultSpec)
      .clientIdPath(CLIENT_ID)
      .servicePath(getRandomString)
      .fromQuery(DATE_FROM)
      .toQuery(DATE_TO)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))

  @Test
  @Owner(TIMONDL)
  def shouldSee400WhenInvalidDateFrom(): Unit = {
    val response = api.offers.getOffersWithBilledServices
      .reqSpec(defaultSpec)
      .clientIdPath(CLIENT_ID)
      .servicePath(SERVICE)
      .fromQuery(getRandomString)
      .toQuery(DATE_TO)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .asString

    assertThat(response).isEqualTo(
      "The query parameter 'from' was malformed:\nInvalid 'from' date specification"
    )
  }

  @Test
  @Owner(TIMONDL)
  def shouldSee400WhenInvalidDateTo(): Unit = {
    val response = api.offers.getOffersWithBilledServices
      .reqSpec(defaultSpec)
      .clientIdPath(CLIENT_ID)
      .servicePath(SERVICE)
      .fromQuery(DATE_FROM)
      .toQuery(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
      .asString

    assertThat(response).isEqualTo(
      "The query parameter 'to' was malformed:\nInvalid 'to' date specification"
    )
  }

  @Test
  @Owner(TIMONDL)
  def shouldGetOffersWithBilledServicesHasNoDiffWithProduction(): Unit = {
    val req = (apiClient: ApiClient) =>
      apiClient.offers.getOffersWithBilledServices
        .reqSpec(defaultSpec)
        .clientIdPath(CLIENT_ID)
        .servicePath(SERVICE)
        .fromQuery(DATE_FROM)
        .toQuery(DATE_TO)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(
      req.apply(api),
      jsonEquals[JsonObject](req.apply(prodApi))
    )
  }
}
