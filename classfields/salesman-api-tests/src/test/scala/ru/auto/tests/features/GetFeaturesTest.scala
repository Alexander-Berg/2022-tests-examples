package ru.auto.tests.features

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonArray
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_NOT_FOUND
import org.hamcrest.MatcherAssert
import org.junit.{Rule, Test}
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.tests.ApiClient
import ru.auto.tests.ResponseSpecBuilders.validatedWith
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{
  shouldBe200OkJSON,
  shouldBeCode
}
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.constants.Constants.SERVICE
import ru.auto.tests.constants.Owners.TIMONDL
import ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals
import ru.auto.tests.module.SalesmanApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec

import scala.annotation.meta.getter

@DisplayName("GET /service/{service}/features")
@GuiceModules(Array(classOf[SalesmanApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetFeaturesTest {

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
  def shouldSee404WithInvalidServicePath(): Unit =
    api.features.getFeatures
      .reqSpec(defaultSpec)
      .servicePath(getRandomString)
      .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))

  @Test
  @Owner(TIMONDL)
  def shouldGetFeaturesHasNoDiffWithProduction(): Unit = {
    val req = (apiClient: ApiClient) =>
      apiClient.features.getFeatures
        .reqSpec(defaultSpec)
        .servicePath(SERVICE)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonArray])

    MatcherAssert.assertThat(
      req.apply(api),
      jsonEquals[JsonArray](req.apply(prodApi))
    )
  }
}
