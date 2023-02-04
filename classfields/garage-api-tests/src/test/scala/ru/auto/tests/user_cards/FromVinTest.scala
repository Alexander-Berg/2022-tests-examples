package ru.auto.tests.user_cards

import com.carlosbecker.guice.{GuiceModules, GuiceTestRunner}
import com.google.gson.JsonObject
import com.google.inject.Inject
import io.qameta.allure.Owner
import io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals
import io.qameta.allure.junit4.DisplayName
import org.apache.http.HttpStatus.SC_BAD_REQUEST
import org.hamcrest.MatcherAssert
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.restassured.ResponseSpecBuilders.{shouldBe200OkJSON, shouldBeCode, validatedWith}
import ru.auto.tests.commons.util.Utils.{getRandomShortLong, getRandomString}
import ru.auto.tests.constants.Owners.{CARFAX, TIMONDL}
import ru.auto.tests.garage.ApiClient
import ru.auto.tests.module.GarageApiModule
import ru.auto.tests.ra.RequestSpecBuilders.defaultSpec
import ru.auto.tests.user_cards.GarageTestUtils.IgnoreFields

import scala.annotation.meta.getter

@DisplayName("GET /user/card/from_vin")
@GuiceModules(Array(classOf[GarageApiModule]))
@RunWith(classOf[GuiceTestRunner])
class FromVinTest {

  private val vin = "KMHDN41BP6U366989"

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
  def shouldSee400ForInvalidVin(): Unit = {
    val request = (apiClient: ApiClient, randomId: String) =>
      apiClient.userCard
        .buildCardFromVin()
        .reqSpec(defaultSpec)
        .vinQuery(randomId)
        .cardTypeQuery("EX_CAR")
        .xUserIdHeader(s"qa_user:$getRandomShortLong")
        .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
        .as(classOf[JsonObject])

    val randomId = getRandomString
    MatcherAssert.assertThat(request(api, randomId), jsonEquals[JsonObject](request(prodApi, randomId)))
  }

  @Test
  @Owner(CARFAX)
  def shouldNotSeeDiffWithProduction(): Unit = {
    val request = (apiClient: ApiClient, user: String) =>
      apiClient.userCard
        .buildCardFromVin()
        .reqSpec(defaultSpec)
        .vinQuery(vin)
        .cardTypeQuery("EX_CAR")
        .xUserIdHeader(user)
        .execute(validatedWith(shouldBe200OkJSON))
        .as(classOf[JsonObject])

    val user = s"qa_user:$getRandomShortLong"
    MatcherAssert.assertThat(
      request(api, user),
      jsonEquals[JsonObject](request(prodApi, user)).whenIgnoringPaths(IgnoreFields: _*)
    )
  }

  @Test
  @Owner(CARFAX)
  def shouldSee400ForInvalidUserId(): Unit = {
    val request = (apiClient: ApiClient, randomId: String) =>
      apiClient.userCard
        .buildCardFromVin()
        .reqSpec(defaultSpec)
        .xUserIdHeader(randomId)
        .vinQuery(vin)
        .cardTypeQuery("EX_CAR")
        .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
        .as(classOf[JsonObject])

    val randomId = getRandomString
    MatcherAssert.assertThat(request(api, randomId), jsonEquals[JsonObject](request(prodApi, randomId)))
  }
}
