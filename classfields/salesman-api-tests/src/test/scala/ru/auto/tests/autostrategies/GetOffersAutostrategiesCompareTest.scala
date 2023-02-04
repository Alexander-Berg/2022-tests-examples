package ru.auto.tests.autostrategies

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonArray
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.hamcrest.MatcherAssert
import org.junit.{After, Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.validatedWith
import ru.auto.tests.adaptor.SalesmanApiAdaptor
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.constants.Constants.{SALESMAN_USER, SERVICE}
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.module.SalesmanApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET /service/{service}/autostrategies")
@GuiceModules(Array(classOf[SalesmanApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetOffersAutostrategiesCompareTest {

  private val OFFER_ID = "1095858148-3a897e9c"

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Inject
  private val salesmanAdaptor: SalesmanApiAdaptor = null

  @Test
  @Owner(TIMONDL)
  def shouldGetOffersAutostrategiesRouteHasNoDiffWithProduction(): Unit = {
    salesmanAdaptor.createAutostrategies(OFFER_ID)

    val req = (apiClient: ApiClient) =>
      apiClient.autostrategies.getOffersAutostrategiesRoute
        .reqSpec(defaultSpec)
        .servicePath(SERVICE)
        .offerIdQuery(OFFER_ID)
        .xSalesmanUserHeader(SALESMAN_USER)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonArray])

    MatcherAssert.assertThat(
      req.apply(api),
      jsonEquals[JsonArray](req.apply(prodApi))
    )
  }

  @After def after(): Unit = salesmanAdaptor.deleteAutostrategies(OFFER_ID)
}
