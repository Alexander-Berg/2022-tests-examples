package ru.auto.tests.offers

import java.util.function.Consumer

import com.carlosbecker.guice.GuiceModules
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import io.restassured.builder.RequestSpecBuilder
import org.hamcrest.MatcherAssert
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.validatedWith
import ru.auto.tests.anno.Prod
import ru.auto.tests.api.OffersApi.GetPlacementDayOper.{
  OFFER_CATEGORY_PATH,
  OFFER_ID_PATH,
  PRODUCT_ID_PATH,
  SERVICE_PATH
}
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory
import ru.auto.tests.constants.Constants.SERVICE
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.module.SalesmanApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

object GetPlacementDayCompareTest {

  @Parameterized.Parameters
  def getParameters: Array[Consumer[RequestSpecBuilder]] =
    Array[Consumer[RequestSpecBuilder]](
      req =>
        req.and
          .addPathParam(OFFER_CATEGORY_PATH, "cars")
          .addPathParam(OFFER_ID_PATH, "1088989228-50a62064")
          .addPathParam(PRODUCT_ID_PATH, "premium")
          .addPathParam(SERVICE_PATH, SERVICE),
      req =>
        req.and
          .addPathParam(OFFER_CATEGORY_PATH, "lcv")
          .addPathParam(OFFER_ID_PATH, "16037618-cf432741")
          .addPathParam(PRODUCT_ID_PATH, "premium")
          .addPathParam(SERVICE_PATH, SERVICE)
    )
}

@DisplayName(
  "GET /service/{service}/offers/category/{offerCategory}/{offerId}/{productId}"
)
@GuiceModules(Array(classOf[SalesmanApiModule]))
@RunWith(classOf[Parameterized])
@Parameterized.UseParametersRunnerFactory(classOf[GuiceParametersRunnerFactory])
class GetPlacementDayCompareTest(reqSpec: Consumer[RequestSpecBuilder]) {

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
  def shouldGetPlacementDayHasNoDiffWithProduction(): Unit = {
    val req = (apiClient: ApiClient) =>
      apiClient.offers.getPlacementDay
        .reqSpec(defaultSpec)
        .reqSpec(reqSpec)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(
      req.apply(api),
      jsonEquals[JsonObject](req.apply(prodApi))
    )
  }
}
