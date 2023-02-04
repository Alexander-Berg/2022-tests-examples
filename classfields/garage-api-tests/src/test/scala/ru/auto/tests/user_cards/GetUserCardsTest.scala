package ru.auto.tests.user_cards

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_NOT_FOUND}
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200OkJSON, shouldBeCode, validatedWith}
import ru.auto.tests.commons.util.Utils.getRandomString
import ru.auto.tests.constants.Owners.{CARFAX, TIMONDL}
import ru.auto.tests.module.GarageApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.garage.ApiClient

import scala.annotation.meta.getter

@DisplayName("GET /user/card/{card-id}")
@GuiceModules(Array(classOf[GarageApiModule]))
@RunWith(classOf[GuiceTestRunner])
class GetUserCardsTest {

  private val cardId = 123456789

  @(Rule @getter)
  @Inject
  val defaultRules: RuleChain = null

  @Inject
  private val api: ApiClient = null

  @Inject
  @Prod
  private val prodApi: ApiClient = null

  @Test
  @Owner(CARFAX)
  def shouldSee404ForUnknownCard(): Unit = {
    val request = (apiClient: ApiClient) =>
      apiClient.userCard.getCard
        .reqSpec(defaultSpec)
        .cardIdPath(-1)
        .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(request(api), jsonEquals[JsonObject](request(prodApi)))
  }

  @Test
  @Owner(TIMONDL)
  def shouldNotSeeDiffWithProduction(): Unit = {
    val request = (apiClient: ApiClient) =>
      apiClient.userCard.getCard
        .reqSpec(defaultSpec)
        .cardIdPath(cardId)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    MatcherAssert.assertThat(request(api), jsonEquals[JsonObject](request(prodApi)))
  }

  @Test
  @Owner(CARFAX)
  def shouldSee400ForInvalidCardId(): Unit = {
    val request = (apiClient: ApiClient, randomId: String) =>
      apiClient.userCard.getCard
        .reqSpec(defaultSpec)
        .cardIdPath(randomId)
        .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
        .as(classOf[JsonObject])

    val randomId = getRandomString
    MatcherAssert.assertThat(request(api, randomId), jsonEquals[JsonObject](request(prodApi, randomId)))
  }
}
