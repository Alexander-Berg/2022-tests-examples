package ru.auto.tests.user_cards

import com.carlosbecker.guice.GuiceModules
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_BAD_REQUEST
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.{Rule, Test}
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200OkJSON, shouldBeCode, validatedWith}
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory
import ru.auto.tests.constants.Owners.TIMONDL
import io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals
import ru.auto.tests.module.RecallApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.recall.ApiClient

import scala.annotation.meta.getter

object GetUserCardsTest {

  @Parameterized.Parameters(name = "{index}: {0}")
  def getParameters: Array[String] = {
    Map(
      "default" -> "swagger",
      "activeAndNonactiveCards" -> "user:30137274",
      "activeCardsOnly" -> "user:33077404",
      "nonActiveCardsOnly" -> "user:33077404"
    ).values.toArray
  }

}

@DisplayName("GET /user-cards")
@GuiceModules(Array(classOf[RecallApiModule]))
@RunWith(classOf[Parameterized])
@Parameterized.UseParametersRunnerFactory(classOf[GuiceParametersRunnerFactory])
class GetUserCardsTest(userId: String) {

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
  def shouldSee400WithoutUserId(): Unit = {
    api.userCards.userCards
      .reqSpec(defaultSpec)
      .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldNotSeeDiffWithProduction(): Unit = {
    val request = (apiClient: ApiClient) =>
      apiClient.userCards.userCards
        .reqSpec(defaultSpec)
        .xUserIdHeader(userId)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(request(api), jsonEquals[JsonObject](request(prodApi)))
  }
}
