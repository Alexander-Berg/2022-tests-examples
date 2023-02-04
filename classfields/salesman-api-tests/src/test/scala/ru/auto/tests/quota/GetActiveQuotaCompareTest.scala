package ru.auto.tests.quota

import com.carlosbecker.guice.GuiceModules
import com.carlosbecker.guice.GuiceTestRunner
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.hamcrest.MatcherAssert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.ApiClient
import ru.auto.tests.anno.Prod
import ru.auto.tests.module.SalesmanApiModule
import java.util.function.Function

import ru.auto.tests.ResponseSpecBuilders.validatedWith
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.constants.Constants.SALESMAN_USER
import ru.auto.tests.constants.Constants.SERVICE
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET /service/{service}/quota/active")
@GuiceModules(Array(classOf[SalesmanApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetActiveQuotaCompareTest {

  private val CLIENT_ID = 16453

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
  def shouldGetActiveQuotaRouteHasNoDiffWithProduction(): Unit = {
    val req = (apiClient: ApiClient) =>
      apiClient.quota.getActiveQuotaRoute
        .reqSpec(defaultSpec)
        .servicePath(SERVICE)
        .clientIdQuery(CLIENT_ID)
        .xSalesmanUserHeader(SALESMAN_USER)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonArray])

    MatcherAssert.assertThat(
      req.apply(api),
      jsonEquals[JsonArray](req.apply(prodApi))
    )
  }
}
