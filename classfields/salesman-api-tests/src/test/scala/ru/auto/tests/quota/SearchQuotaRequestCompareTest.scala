package ru.auto.tests.quota

import java.time.OffsetDateTime

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonArray
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.hamcrest.MatcherAssert
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.validatedWith
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.constants.Constants.{SALESMAN_USER, SERVICE}
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.module.SalesmanApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET /service/{service}/quota/requests")
@GuiceModules(Array(classOf[SalesmanApiModule]))
@RunWith(classOf[GuiceTestRunner])
class SearchQuotaRequestCompareTest {

  private val ENTITY = "dealer"

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
  def shouldSearchQuotaRequestRouteHasNoDiffWithProduction(): Unit = {
    val req = (apiClient: ApiClient) =>
      apiClient.quota.searchQuotaRequestRoute
        .reqSpec(defaultSpec)
        .servicePath(SERVICE)
        .fromQuery(OffsetDateTime.now.minusMonths(3).toString)
        .entityQuery(ENTITY)
        .xSalesmanUserHeader(SALESMAN_USER)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonArray])

    MatcherAssert.assertThat(
      req.apply(api),
      jsonEquals[JsonArray](req.apply(prodApi))
    )
  }
}
